package com.coremate.backend.solve.service;

import com.coremate.backend.reward.entity.RewardLog;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 완료된 풀이 회차에 보상을 지급한다.
 *
 * <p>확정된 보상 구조(2026-07-30)를 그대로 따른다.
 * <ul>
 *   <li>XP는 정답률과 무관하게 "완료 + 일일 한도"로만 판정한다.</li>
 *   <li>코인은 챕터/과목 모의고사에서만 나오고, 정답률 기준과 범위별 최초 1회를 함께 본다.</li>
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
     * @param correctRate 회차 정답률(%). 코인 지급 기준과 비교된다.
     */
    @Transactional
    public SolveRewardResult grantForCompletedSession(SolveSession session, BigDecimal correctRate) {
        RewardType xpType = resolveXpRewardType(session.getSessionType());
        if (xpType == null) {
            // 오늘의 CS 퀴즈는 출석 흐름(AttendanceService)에서 따로 보상을 처리한다.
            return SolveRewardResult.none();
        }

        RewardLog xpLog = rewardService.grantPolicyReward(
                session.getUserId(),
                xpType,
                RewardSource.SOLVE_SESSION,
                session.getSolveSessionId(),
                resolveXpTargetType(session),
                resolveXpTargetId(session),
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
    private RewardLog grantCoinIfEligible(SolveSession session, BigDecimal correctRate) {
        SolveSessionType type = session.getSessionType();
        if (!type.isMock()) {
            return null;
        }

        RewardType coinType = type == SolveSessionType.SUBJECT_MOCK
                ? RewardType.SUBJECT_MOCK_FIRST_PASS_COIN
                : RewardType.CATEGORY_MOCK_FIRST_PASS_COIN;

        return rewardService.grantPolicyReward(
                session.getUserId(),
                coinType,
                RewardSource.SOLVE_SESSION,
                session.getSolveSessionId(),
                resolveCoinTargetType(type),
                resolveCoinTargetId(session, type),
                session.getSolveSessionId(),
                correctRate
        );
    }

    private RewardType resolveXpRewardType(SolveSessionType type) {
        return switch (type) {
            case CONCEPT_PRACTICE -> RewardType.CONCEPT_PRACTICE_XP;
            case CATEGORY_LV1_MOCK, CATEGORY_LV2_MOCK -> RewardType.CATEGORY_MOCK_XP;
            case SUBJECT_MOCK -> RewardType.SUBJECT_MOCK_XP;
            case DAILY_QUIZ -> null;
        };
    }

    // XP는 일일 한도만 보므로 범위별 중복 판정 타겟이 필요 없다.
    // 다만 어떤 대상에서 발생한 보상인지 추적할 수 있도록 타겟을 남긴다.
    private RewardTargetType resolveXpTargetType(SolveSession session) {
        return switch (session.getSessionType()) {
            case CONCEPT_PRACTICE -> RewardTargetType.CONCEPT;
            case CATEGORY_LV1_MOCK -> RewardTargetType.CATEGORY_LV1;
            case CATEGORY_LV2_MOCK -> RewardTargetType.CATEGORY_LV2;
            case SUBJECT_MOCK -> RewardTargetType.SUBJECT;
            case DAILY_QUIZ -> RewardTargetType.DAILY;
        };
    }

    private Long resolveXpTargetId(SolveSession session) {
        return switch (session.getSessionType()) {
            case CONCEPT_PRACTICE -> session.getConcept() == null ? null : session.getConcept().getConceptId();
            case CATEGORY_LV1_MOCK -> session.getCategoryLv1() == null ? null : session.getCategoryLv1().getCategoryLv1Id();
            case CATEGORY_LV2_MOCK -> session.getCategoryLv2() == null ? null : session.getCategoryLv2().getCategoryLv2Id();
            case SUBJECT_MOCK -> session.getSubject() == null ? null : session.getSubject().getSubjectId();
            case DAILY_QUIZ -> null;
        };
    }

    // 코인의 "범위별 평생 1회"를 판정하는 단위. 챕터는 대분류/중분류, 과목은 과목이다.
    private RewardTargetType resolveCoinTargetType(SolveSessionType type) {
        return switch (type) {
            case CATEGORY_LV1_MOCK -> RewardTargetType.CATEGORY_LV1;
            case CATEGORY_LV2_MOCK -> RewardTargetType.CATEGORY_LV2;
            case SUBJECT_MOCK -> RewardTargetType.SUBJECT;
            default -> null;
        };
    }

    private Long resolveCoinTargetId(SolveSession session, SolveSessionType type) {
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

    private com.coremate.backend.reward.entity.type.RewardSkipReason skipReasonOf(RewardLog log) {
        return log == null ? null : log.getSkipReason();
    }
}
