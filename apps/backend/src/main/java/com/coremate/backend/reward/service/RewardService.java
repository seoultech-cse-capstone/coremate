package com.coremate.backend.reward.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.dto.response.CurrencyLogResponse;
import com.coremate.backend.reward.dto.response.DailyRewardLimitResponse;
import com.coremate.backend.reward.dto.response.RewardLogResponse;
import com.coremate.backend.reward.dto.response.RewardPolicyResponse;
import com.coremate.backend.reward.dto.response.UserCurrencyResponse;
import com.coremate.backend.reward.dto.response.UserRewardSummaryResponse;
import com.coremate.backend.reward.dto.response.XpLogResponse;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.DailyRewardLimit;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.RewardPolicy;
import com.coremate.backend.reward.entity.UserCurrency;
import com.coremate.backend.reward.entity.XpLog;
import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.entity.type.CurrencyTransactionType;
import com.coremate.backend.reward.entity.type.RewardScope;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.entity.type.XpReason;
import com.coremate.backend.reward.repository.CurrencyLogRepository;
import com.coremate.backend.reward.repository.DailyRewardLimitRepository;
import com.coremate.backend.reward.repository.RewardLogRepository;
import com.coremate.backend.reward.repository.RewardPolicyRepository;
import com.coremate.backend.reward.repository.RewardUserProgressRepository;
import com.coremate.backend.reward.repository.UserCurrencyRepository;
import com.coremate.backend.reward.repository.XpLogRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.coremate.backend.notification.service.NotificationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    // 조회 API에서 한 번에 내려줄 최근 로그 개수.
    private static final int DEFAULT_LOG_LIMIT = 50;

    // 레벨 곡선과 보상 구간은 LevelPolicy 에 모아 두었다.

    private final UserRepository userRepository;
    private final RewardUserProgressRepository rewardUserProgressRepository;
    private final UserCurrencyRepository userCurrencyRepository;
    private final CurrencyLogRepository currencyLogRepository;
    private final XpLogRepository xpLogRepository;
    private final RewardLogRepository rewardLogRepository;
    private final DailyRewardLimitRepository dailyRewardLimitRepository;
    private final RewardPolicyRepository rewardPolicyRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 마이페이지/홈에서 쓰는 XP, 레벨, 코인 요약.
    @Transactional
    public UserRewardSummaryResponse getSummary(Long userId) {
        User user = findUser(userId);
        UserCurrency currency = getOrCreateCurrency(userId);
        return new UserRewardSummaryResponse(userId, user.getTotalXp(), user.getLevel(), currency.getCoin());
    }

    // 사용자 재화 행이 없으면 0코인으로 자동 생성한다.
    @Transactional
    public UserCurrencyResponse getMyCurrency(Long userId) {
        UserCurrency currency = getOrCreateCurrency(userId);
        return toUserCurrencyResponse(currency);
    }

    public List<CurrencyLogResponse> getCurrencyLogs(Long userId) {
        findUser(userId);
        return currencyLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, DEFAULT_LOG_LIMIT))
                .stream()
                .map(this::toCurrencyLogResponse)
                .toList();
    }

    public List<XpLogResponse> getXpLogs(Long userId) {
        findUser(userId);
        return xpLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, DEFAULT_LOG_LIMIT))
                .stream()
                .map(this::toXpLogResponse)
                .toList();
    }

    public List<RewardLogResponse> getRewardLogs(Long userId) {
        findUser(userId);
        return rewardLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, DEFAULT_LOG_LIMIT))
                .stream()
                .map(this::toRewardLogResponse)
                .toList();
    }

    public List<DailyRewardLimitResponse> getDailyLimits(Long userId, LocalDate rewardDate) {
        findUser(userId);
        List<DailyRewardLimit> limits = rewardDate == null
                ? dailyRewardLimitRepository.findByUserIdOrderByRewardDateDescRewardTypeAsc(userId)
                : dailyRewardLimitRepository.findByUserIdAndRewardDateOrderByRewardTypeAsc(userId, rewardDate);

        return limits.stream()
                .map(this::toDailyRewardLimitResponse)
                .toList();
    }

    public List<RewardPolicyResponse> getActivePolicies() {
        return rewardPolicyRepository.findByActiveTrueOrderByRewardTypeAsc()
                .stream()
                .map(this::toRewardPolicyResponse)
                .toList();
    }

    // 코인 지급은 잔액 변경과 원장 로그 저장을 한 트랜잭션으로 묶는다.
    @Transactional
    public CurrencyLog earnCoin(Long userId, int amount, CurrencyReason reason, String referenceType, Long referenceId) {
        UserCurrency currency = getOrCreateCurrencyForUpdate(userId);
        currency.earn(amount);
        return currencyLogRepository.save(new CurrencyLog(
                userId,
                amount,
                currency.getCoin(),
                CurrencyTransactionType.EARN,
                reason,
                referenceType,
                referenceId
        ));
    }

    // 코인 차감은 잔액 잠금 후 처리해서 동시 구매/차감 경쟁을 줄인다.
    @Transactional
    public CurrencyLog spendCoin(Long userId, int amount, CurrencyReason reason, String referenceType, Long referenceId) {
        UserCurrency currency = getOrCreateCurrencyForUpdate(userId);
        currency.spend(amount);
        return currencyLogRepository.save(new CurrencyLog(
                userId,
                -amount,
                currency.getCoin(),
                CurrencyTransactionType.SPEND,
                reason,
                referenceType,
                referenceId
        ));
    }

    // XP 지급 후 users.total_xp/level과 xp_logs를 함께 갱신한다.
    @Transactional
    public XpLog grantXp(Long userId, Long rewardPolicyId, int amount, XpReason reason,
                         String referenceType, Long referenceId) {
        validatePositiveAmount(amount);
        User user = findUser(userId);
        int totalXpAfter = user.getTotalXp() + amount;
        int levelAfter = calculateLevel(totalXpAfter);

        int updatedCount = rewardUserProgressRepository.updateLearningProgress(userId, totalXpAfter, levelAfter);
        if (updatedCount == 0) {
            throw new CoreMateException(ErrorCode.USER_NOT_FOUND);
        }

        XpLog xpLog = xpLogRepository.save(new XpLog(userId, rewardPolicyId, amount, totalXpAfter, reason,
                referenceType, referenceId));

        grantLevelUpRewards(userId, calculateLevel(user.getTotalXp()), levelAfter);

        return xpLog;
    }

    // 레벨이 올랐을 때, 도달한 보상 구간 중 아직 못 받은 것을 지급한다.
    //
    // "이번에 넘어선 구간"이 아니라 "도달한 구간 전체 중 미지급분"을 보는 이유는
    // 한 번 놓친 보상이 영영 못 받는 상태로 남지 않게 하기 위해서다. 예를 들어
    // 정책 시드가 없던 시점에 5레벨에 도달하면 POLICY_NOT_FOUND 로 스킵되는데,
    // 넘어선 구간만 보면 이미 5레벨이라 다시는 지급 기회가 오지 않는다.
    // 이렇게 두면 다음 레벨업 때 밀린 보상까지 함께 정산된다.
    //
    // 이미 지급된 구간은 조회로 걸러 SKIPPED 로그가 쌓이지 않게 한다.
    // 레벨업 정책의 xp_amount 는 0 이라 여기서 grantXp 가 다시 불리지 않는다.
    // 누군가 레벨업 정책에 XP 를 넣으면 재귀가 생기므로 시드에서 0 을 유지해야 한다.
    private void grantLevelUpRewards(Long userId, int levelBefore, int levelAfter) {
        if (levelAfter <= levelBefore) {
            return;
        }

        // 보상 구간이 아닌 레벨도 상승 자체는 알린다. 성장이 눈에 보이는 게 재방문 이유다.
        eventPublisher.publishEvent(NotificationMessages.levelUp(userId, levelAfter));

        for (Integer level : LevelPolicy.rewardLevelsUpTo(levelAfter)) {
            RewardType rewardType = resolveLevelUpRewardType(level);
            if (rewardType == null || isLevelRewardGranted(userId, rewardType, level)) {
                continue;
            }
            grantPolicyReward(
                    userId,
                    rewardType,
                    RewardSource.LEVEL_UP,
                    null,
                    RewardTargetType.LEVEL,
                    level.longValue(),
                    null
            );
        }
    }

    private boolean isLevelRewardGranted(Long userId, RewardType rewardType, Integer level) {
        return rewardLogRepository.existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
                userId, rewardType, RewardTargetType.LEVEL, level.longValue(), RewardStatus.GRANTED);
    }

    private RewardType resolveLevelUpRewardType(int level) {
        return switch (level) {
            case 5 -> RewardType.LEVEL_UP_5;
            case 10 -> RewardType.LEVEL_UP_10;
            case 20 -> RewardType.LEVEL_UP_20;
            case 30 -> RewardType.LEVEL_UP_30;
            case 40 -> RewardType.LEVEL_UP_40;
            case 50 -> RewardType.LEVEL_UP_50;
            default -> null;
        };
    }

    // 출석/퀴즈/모의고사처럼 일일 제한이 있는 행동의 횟수를 소비한다.
    @Transactional
    public DailyRewardLimit consumeDailyLimit(Long userId, RewardType rewardType, LocalDate rewardDate,
                                              Integer dailyLimit) {
        findUser(userId);
        DailyRewardLimit limit = getOrCreateDailyRewardLimit(userId, rewardDate, rewardType, dailyLimit);
        if (!limit.canConsume()) {
            throw new CoreMateException(ErrorCode.DAILY_REWARD_LIMIT_EXCEEDED);
        }
        limit.consume();
        return limit;
    }

    // 정답률 조건이 없는 보상(XP, 출석, 퀴즈 등)의 진입점.
    @Transactional
    public RewardLog grantPolicyReward(Long userId, RewardType rewardType, RewardSource rewardSource, Long sourceId,
                                       RewardTargetType targetType, Long targetId, Long solveSessionId) {
        return grantPolicyReward(userId, rewardType, rewardSource, sourceId, targetType, targetId,
                solveSessionId, null);
    }

    // reward_policies 기준으로 보상 지급 가능 여부를 판정한다.
    //
    // 판정 순서(문서 확정): 활성 정책 -> 범위별 최초 보상 여부 -> 정답률 -> 일일 한도 소모.
    // 최초 보상 여부와 정답률에서 걸러진 요청은 일일 한도를 소모하지 않는다.
    // 이미 보상받은 범위를 다시 풀거나 정답률이 미달했을 때 하루 한도가
    // 깎이면 사용자가 손해를 보기 때문이다.
    //
    // correctRate 는 코인 보상 판정에만 쓰이며 null 이면 정답률 검사를 건너뛴다.
    @Transactional
    public RewardLog grantPolicyReward(Long userId, RewardType rewardType, RewardSource rewardSource, Long sourceId,
                                       RewardTargetType targetType, Long targetId, Long solveSessionId,
                                       BigDecimal correctRate) {
        findUser(userId);

        RewardPolicy policy = rewardPolicyRepository.findByRewardTypeAndActiveTrue(rewardType)
                .orElse(null);
        if (policy == null) {
            return saveSkippedReward(userId, rewardType, rewardSource, sourceId, targetType, targetId,
                    solveSessionId, RewardSkipReason.POLICY_NOT_FOUND);
        }

        if (isAlreadyRewarded(userId, policy, targetType, targetId)) {
            return saveSkippedReward(userId, policy.getRewardPolicyId(), rewardType, rewardSource, sourceId, targetType, targetId,
                    solveSessionId, RewardSkipReason.ALREADY_REWARDED);
        }

        if (isCorrectRateBelowPolicy(policy, correctRate)) {
            return saveSkippedReward(userId, policy.getRewardPolicyId(), rewardType, rewardSource, sourceId, targetType, targetId,
                    solveSessionId, RewardSkipReason.LOW_CORRECT_RATE);
        }

        if (policy.hasDailyLimit()
                && !tryConsumeDailyLimit(userId, rewardType, LocalDate.now(), policy.getDailyLimit())) {
            return saveSkippedReward(userId, policy.getRewardPolicyId(), rewardType, rewardSource, sourceId, targetType, targetId,
                    solveSessionId, RewardSkipReason.DAILY_LIMIT_EXCEEDED);
        }

        if (policy.getXpAmount() > 0) {
            grantXp(userId, policy.getRewardPolicyId(), policy.getXpAmount(), resolveXpReason(rewardType),
                    rewardSource.name(), sourceId);
        }
        if (policy.getCoinAmount() > 0) {
            earnCoin(userId, policy.getCoinAmount(), resolveCurrencyReason(rewardType), rewardSource.name(), sourceId);
        }

        return rewardLogRepository.save(RewardLog.granted(
                userId,
                policy.getRewardPolicyId(),
                rewardType,
                rewardSource,
                sourceId,
                targetType,
                targetId,
                solveSessionId,
                policy.getXpAmount(),
                policy.getCoinAmount(),
                null,
                null
        ));
    }

    // 조회 시에도 사용자 재화가 없으면 기본 행을 만들어 이후 로직을 단순화한다.
    private UserCurrency getOrCreateCurrency(Long userId) {
        findUser(userId);
        return userCurrencyRepository.findById(userId)
                .orElseGet(() -> userCurrencyRepository.save(new UserCurrency(userId)));
    }

    // 잔액을 바꾸는 흐름에서는 비관적 잠금으로 같은 사용자 재화를 보호한다.
    private UserCurrency getOrCreateCurrencyForUpdate(Long userId) {
        findUser(userId);
        return userCurrencyRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> userCurrencyRepository.saveAndFlush(new UserCurrency(userId)));
    }

    // 사용자+날짜+보상유형 조합의 일일 제한 행을 가져오거나 새로 만든다.
    private DailyRewardLimit getOrCreateDailyRewardLimit(Long userId, LocalDate rewardDate, RewardType rewardType,
                                                         Integer dailyLimit) {
        return dailyRewardLimitRepository.findByUserIdAndRewardDateAndRewardTypeForUpdate(
                        userId, rewardDate, rewardType)
                .orElseGet(() -> dailyRewardLimitRepository.save(
                        new DailyRewardLimit(userId, rewardDate, rewardType, dailyLimit)));
    }

    // 정책 기반 보상에서 한도 초과는 예외 대신 SKIPPED 로그로 남기기 위해 boolean으로 반환한다.
    private boolean tryConsumeDailyLimit(Long userId, RewardType rewardType, LocalDate rewardDate, Integer dailyLimit) {
        DailyRewardLimit limit = getOrCreateDailyRewardLimit(userId, rewardDate, rewardType, dailyLimit);
        if (!limit.canConsume()) {
            return false;
        }
        limit.consume();
        return true;
    }

    // 코인 보상의 최소 정답률 조건. 정책에 기준이 없으면 검사하지 않는다.
    // 기준이 있는데 정답률이 전달되지 않았다면 지급하지 않는 쪽(미달)으로 판정한다.
    private boolean isCorrectRateBelowPolicy(RewardPolicy policy, BigDecimal correctRate) {
        if (policy.getMinCorrectRate() == null) {
            return false;
        }
        if (correctRate == null) {
            return true;
        }
        return correctRate.compareTo(policy.getMinCorrectRate()) < 0;
    }

    // 챕터/과목 최초 코인 보상처럼 target 단위 중복 지급을 막는다.
    //
    // 스타터팩·프리미엄 가입 보너스처럼 reward_scope가 PER_USER인 보상은 범위 타겟이
    // 따로 없다. 호출부가 타겟을 넘기지 않으면 여기서 사용자 자신을 타겟으로 삼아
    // 계정당 1회 판정이 빠지지 않게 한다.
    private boolean isAlreadyRewarded(Long userId, RewardPolicy policy, RewardTargetType targetType, Long targetId) {
        if (!policy.isFirstRewardOnly()) {
            return false;
        }

        RewardTargetType effectiveTargetType = targetType;
        Long effectiveTargetId = targetId;
        if (policy.getRewardScope() == RewardScope.PER_USER && (targetType == null || targetId == null)) {
            effectiveTargetType = RewardTargetType.USER;
            effectiveTargetId = userId;
        }

        return effectiveTargetType != null
                && effectiveTargetId != null
                && rewardLogRepository.existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
                userId,
                policy.getRewardType(),
                effectiveTargetType,
                effectiveTargetId,
                RewardStatus.GRANTED
        );
    }

    // 정책이 없거나 조건을 못 맞춘 경우에도 reward_logs에 미지급 사유를 남긴다.
    private RewardLog saveSkippedReward(Long userId, RewardType rewardType, RewardSource rewardSource, Long sourceId,
                                        RewardTargetType targetType, Long targetId, Long solveSessionId,
                                        RewardSkipReason skipReason) {
        return saveSkippedReward(userId, null, rewardType, rewardSource, sourceId, targetType, targetId,
                solveSessionId, skipReason);
    }

    private RewardLog saveSkippedReward(Long userId, Long rewardPolicyId, RewardType rewardType,
                                        RewardSource rewardSource, Long sourceId, RewardTargetType targetType,
                                        Long targetId, Long solveSessionId, RewardSkipReason skipReason) {
        return rewardLogRepository.save(RewardLog.skipped(
                userId,
                rewardPolicyId,
                rewardType,
                rewardSource,
                sourceId,
                targetType,
                targetId,
                solveSessionId,
                skipReason
        ));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new CoreMateException(ErrorCode.INVALID_REWARD_AMOUNT);
        }
    }

    private int calculateLevel(int totalXp) {
        return LevelPolicy.levelOf(totalXp);
    }

    // reward_type을 XP 로그의 reason 값으로 매핑한다.
    private XpReason resolveXpReason(RewardType rewardType) {
        return switch (rewardType) {
            case CONCEPT_PRACTICE_XP -> XpReason.CONCEPT_PRACTICE_XP;
            case CATEGORY_MOCK_XP -> XpReason.CATEGORY_MOCK_XP;
            case SUBJECT_MOCK_XP -> XpReason.SUBJECT_MOCK_XP;
            case DAILY_QUIZ_COMPLETE, DAILY_QUIZ_REWARD -> XpReason.DAILY_QUIZ_XP;
            case ATTENDANCE_REWARD, DAILY_QUIZ_ATTENDANCE -> XpReason.ATTENDANCE_XP;
            case LEVEL_UP_5, LEVEL_UP_10, LEVEL_UP_20, LEVEL_UP_30, LEVEL_UP_40, LEVEL_UP_50
                    -> XpReason.LEVEL_UP_BONUS_XP;
            default -> throw new CoreMateException(ErrorCode.INVALID_REWARD_AMOUNT);
        };
    }

    // reward_type을 코인 로그의 reason 값으로 매핑한다.
    private CurrencyReason resolveCurrencyReason(RewardType rewardType) {
        return switch (rewardType) {
            case DAILY_QUIZ_COMPLETE, DAILY_QUIZ_REWARD -> CurrencyReason.DAILY_QUIZ_REWARD;
            case ATTENDANCE_REWARD, DAILY_QUIZ_ATTENDANCE -> CurrencyReason.ATTENDANCE_REWARD;
            case CATEGORY_MOCK_FIRST_PASS_COIN -> CurrencyReason.CATEGORY_MOCK_FIRST_PASS_REWARD;
            case SUBJECT_MOCK_FIRST_PASS_COIN -> CurrencyReason.SUBJECT_MOCK_FIRST_PASS_REWARD;
            case CATEGORY_MOCK_RETRY_COIN -> CurrencyReason.CATEGORY_MOCK_RETRY_REWARD;
            case SUBJECT_MOCK_RETRY_COIN -> CurrencyReason.SUBJECT_MOCK_RETRY_REWARD;
            case LEVEL_UP_5, LEVEL_UP_10, LEVEL_UP_20, LEVEL_UP_30, LEVEL_UP_40, LEVEL_UP_50
                    -> CurrencyReason.LEVEL_UP_REWARD;
            case STARTER_PACK -> CurrencyReason.STARTER_PACK;
            case AD_REWARD -> CurrencyReason.AD_REWARD;
            case PREMIUM_DAILY_COIN -> CurrencyReason.PREMIUM_DAILY_COIN;
            case PREMIUM_SIGNUP_BONUS -> CurrencyReason.PREMIUM_SIGNUP_BONUS;
            case PAYMENT_COIN_PACKAGE -> CurrencyReason.COIN_PACKAGE_PURCHASE;
            default -> throw new CoreMateException(ErrorCode.INVALID_REWARD_AMOUNT);
        };
    }

    private UserCurrencyResponse toUserCurrencyResponse(UserCurrency currency) {
        return new UserCurrencyResponse(currency.getUserId(), currency.getCoin(), currency.getUpdatedAt());
    }

    private CurrencyLogResponse toCurrencyLogResponse(CurrencyLog log) {
        return new CurrencyLogResponse(
                log.getCurrencyLogId(),
                log.getUserId(),
                log.getAmount(),
                log.getBalanceAfter(),
                log.getTransactionType(),
                log.getReason(),
                log.getReferenceType(),
                log.getReferenceId(),
                log.getCreatedAt()
        );
    }

    private XpLogResponse toXpLogResponse(XpLog log) {
        return new XpLogResponse(
                log.getXpLogId(),
                log.getUserId(),
                log.getAmount(),
                log.getTotalXpAfter(),
                log.getReason(),
                log.getReferenceType(),
                log.getReferenceId(),
                log.getCreatedAt()
        );
    }

    private RewardLogResponse toRewardLogResponse(RewardLog log) {
        return new RewardLogResponse(
                log.getRewardLogId(),
                log.getUserId(),
                log.getRewardType(),
                log.getRewardSource(),
                log.getSourceId(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSolveSessionId(),
                log.getXpAmount(),
                log.getCoinAmount(),
                log.getItemId(),
                log.getRandomBoxId(),
                log.getRewardStatus(),
                log.getSkipReason(),
                log.getCreatedAt()
        );
    }

    private DailyRewardLimitResponse toDailyRewardLimitResponse(DailyRewardLimit limit) {
        return new DailyRewardLimitResponse(
                limit.getDailyRewardLimitId(),
                limit.getUserId(),
                limit.getRewardDate(),
                limit.getRewardType(),
                limit.getRewardCount(),
                limit.getDailyLimit(),
                limit.getUpdatedAt()
        );
    }

    private RewardPolicyResponse toRewardPolicyResponse(RewardPolicy policy) {
        return new RewardPolicyResponse(
                policy.getRewardPolicyId(),
                policy.getRewardType(),
                policy.getRewardKind(),
                policy.getCoinAmount(),
                policy.getXpAmount(),
                policy.getDailyLimit(),
                policy.getMinCorrectRate(),
                policy.isFirstRewardOnly(),
                policy.getRewardScope(),
                policy.getDescription()
        );
    }
}
