package com.coremate.backend.solve.service;

import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.solve.entity.SolveSession;
import com.coremate.backend.solve.dto.type.SolveSessionType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 완료된 풀이 회차에 보상을 지급한다.
 *
 * <p>확정된 보상 구조(2026-07-30)를 그대로 따른다.
 * <ul>
 *   <li>XP는 정답률과 무관하게 "완료 + 일일 한도"로만 판정한다.</li>
 *   <li>코인은 챕터/과목 모의고사에서만 나오고, 정답률 기준과 범위별 최초 1회를 함께 본다.</li>
 *   <li>최초 1회를 이미 받은 범위는 정답률만 다시 보고 소액의 재응시 코인을 준다(2026-08-07).</li>
 *   <li>모의고사 XP와 재응시 코인은 같은 범위(대분류/중분류/과목)로는 하루 1회만 준다(2026-08-11).
 *       같은 챕터를 하루에 반복해 푸는 것으로 보상을 캐지 못하게 하기 위해서다.
 *       개념 문제집은 적용 대상이 아니다.</li>
 *   <li>일일 한도를 넘겨도 풀이와 기록은 이미 정상 저장돼 있고 보상만 지급되지 않는다.</li>
 * </ul>
 *
 * <p>지급량과 한도는 코드에 두지 않고 {@code reward_policies} 행에서 읽는다.
 */
@Service
@RequiredArgsConstructor
public class SolveRewardService {

    private final RewardService rewardService;

    /**
     * 완료된 회차에 XP와(대상이면) 코인을 지급한다.
     *
     * <p>풀이 완료 트랜잭션과 분리한다(REQUIRES_NEW). 보상은 학습 기록보다 부수적이라
     * 보상 지급이 실패해도 사용자가 푼 기록까지 사라져서는 안 된다. 같은 트랜잭션에서
     * 돌면 여기서 난 예외가 완료 트랜잭션까지 롤백시킨다.
     *
     * @param correctRate 회차 정답률(%). 코인 지급 기준과 비교된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SolveRewardResult grantForCompletedSession(SolveSession session, BigDecimal correctRate) {
        RewardType xpType = resolveXpRewardType(session.getSessionType());
        if (xpType == null) {
            // 오늘의 CS 퀴즈는 출석 흐름(AttendanceService)에서 따로 보상을 처리한다.
            return SolveRewardResult.none();
        }

        // 모의고사 XP 는 "같은 범위 하루 1회"(scope_daily_once) 판정 대상이라 target 을 넘긴다.
        // 개념 문제집은 적용 대상이 아니라 target 이 null 로 나가고, 정책도 플래그가 꺼져 있다.
        //
        // ⚠️ XP 정책의 first_reward_only 는 반드시 FALSE 로 유지해야 한다. target 이 붙은 지금
        // 그 값을 켜면 "범위당 평생 XP 1회" 로 동작이 조용히 바뀐다. 하루 1회 제한은
        // first_reward_only 가 아니라 scope_daily_once 로만 표현한다.
        RewardLog xpLog = rewardService.grantPolicyReward(
                session.getUserId(),
                xpType,
                RewardSource.SOLVE_SESSION,
                session.getSolveSessionId(),
                resolveScopeTargetType(session.getSessionType()),
                resolveScopeTargetId(session, session.getSessionType()),
                session.getSolveSessionId()
        );

        RewardLog coinLog = grantCoinIfEligible(session, correctRate);

        return new SolveRewardResult(
                grantedXp(xpLog),
                grantedCoin(coinLog),
                skipReasonOf(xpLog),
                skipReasonOf(coinLog)
        );
    }

    // 코인은 챕터/과목 모의고사만 대상이다. 개념 문제집과 오늘의 퀴즈는 여기서 제외된다.
    //
    // 최초 통과 정책을 먼저 태우고, 그게 "이미 이 범위에서 받았음"으로 걸린 회차만
    // 재응시 정책으로 다시 판정한다. 한 회차가 두 보상을 모두 받는 일은 없다.
    private RewardLog grantCoinIfEligible(SolveSession session, BigDecimal correctRate) {
        SolveSessionType type = session.getSessionType();
        if (!type.isMock()) {
            return null;
        }

        RewardLog firstPassLog = rewardService.grantPolicyReward(
                session.getUserId(),
                firstPassCoinType(type),
                RewardSource.SOLVE_SESSION,
                session.getSolveSessionId(),
                resolveScopeTargetType(type),
                resolveScopeTargetId(session, type),
                session.getSolveSessionId(),
                correctRate
        );

        // ALREADY_REWARDED 일 때만 넘어간다. 정답률 미달이나 일일 한도 초과로 걸린
        // 회차까지 재응시로 넘기면, 최초 통과 조건을 한 번도 못 넘긴 풀이가
        // 재응시 보상으로 우회 지급된다.
        if (firstPassLog.getSkipReason() != RewardSkipReason.ALREADY_REWARDED) {
            return firstPassLog;
        }

        // 재응시는 평생 중복 판정은 하지 않지만(first_reward_only = FALSE)
        // "같은 범위 하루 1회"(scope_daily_once = TRUE) 판정 대상이라 target 을 넘긴다.
        return rewardService.grantPolicyReward(
                session.getUserId(),
                retryCoinType(type),
                RewardSource.SOLVE_SESSION,
                session.getSolveSessionId(),
                resolveScopeTargetType(type),
                resolveScopeTargetId(session, type),
                session.getSolveSessionId(),
                correctRate
        );
    }

    private RewardType firstPassCoinType(SolveSessionType type) {
        return type == SolveSessionType.SUBJECT_MOCK
                ? RewardType.SUBJECT_MOCK_FIRST_PASS_COIN
                : RewardType.CATEGORY_MOCK_FIRST_PASS_COIN;
    }

    private RewardType retryCoinType(SolveSessionType type) {
        return type == SolveSessionType.SUBJECT_MOCK
                ? RewardType.SUBJECT_MOCK_RETRY_COIN
                : RewardType.CATEGORY_MOCK_RETRY_COIN;
    }

    private RewardType resolveXpRewardType(SolveSessionType type) {
        return switch (type) {
            case CONCEPT_PRACTICE -> RewardType.CONCEPT_PRACTICE_XP;
            case CATEGORY_LV1_MOCK, CATEGORY_LV2_MOCK -> RewardType.CATEGORY_MOCK_XP;
            case SUBJECT_MOCK -> RewardType.SUBJECT_MOCK_XP;
            case DAILY_QUIZ -> null;
        };
    }


    // 범위 중복을 판정하는 단위. 챕터는 대분류/중분류, 과목은 과목이다.
    // 코인의 "범위별 평생 1회"와 XP·재응시 코인의 "같은 범위 하루 1회"가 같은 단위를 쓴다.
    // 대분류와 중분류를 따로 세므로 같은 대분류에서 대분류 회차 1회 + 중분류 회차 1회를 받을 수 있다.
    private RewardTargetType resolveScopeTargetType(SolveSessionType type) {
        return switch (type) {
            case CATEGORY_LV1_MOCK -> RewardTargetType.CATEGORY_LV1;
            case CATEGORY_LV2_MOCK -> RewardTargetType.CATEGORY_LV2;
            case SUBJECT_MOCK -> RewardTargetType.SUBJECT;
            default -> null;
        };
    }

    private Long resolveScopeTargetId(SolveSession session, SolveSessionType type) {
        return switch (type) {
            case CATEGORY_LV1_MOCK -> session.getCategoryLv1() == null ? null : session.getCategoryLv1().getCategoryLv1Id();
            case CATEGORY_LV2_MOCK -> session.getCategoryLv2() == null ? null : session.getCategoryLv2().getCategoryLv2Id();
            case SUBJECT_MOCK -> session.getSubject() == null ? null : session.getSubject().getSubjectId();
            default -> null;
        };
    }

    private int grantedXp(RewardLog log) {
        return log != null && log.getRewardStatus() == RewardStatus.GRANTED ? log.getXpAmount() : 0;
    }

    private int grantedCoin(RewardLog log) {
        return log != null && log.getRewardStatus() == RewardStatus.GRANTED ? log.getCoinAmount() : 0;
    }

    private RewardSkipReason skipReasonOf(RewardLog log) {
        return log == null ? null : log.getSkipReason();
    }
}
