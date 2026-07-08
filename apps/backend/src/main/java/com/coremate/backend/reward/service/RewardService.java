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
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    // 조회 API에서 한 번에 내려줄 최근 로그 개수.
    private static final int DEFAULT_LOG_LIMIT = 50;

    // 임시 MVP 레벨 공식. 추후 레벨 정책이 확정되면 이 값/계산식을 교체한다.
    private static final int XP_PER_LEVEL = 100;

    private final UserRepository userRepository;
    private final RewardUserProgressRepository rewardUserProgressRepository;
    private final UserCurrencyRepository userCurrencyRepository;
    private final CurrencyLogRepository currencyLogRepository;
    private final XpLogRepository xpLogRepository;
    private final RewardLogRepository rewardLogRepository;
    private final DailyRewardLimitRepository dailyRewardLimitRepository;
    private final RewardPolicyRepository rewardPolicyRepository;

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

        return xpLogRepository.save(new XpLog(userId, rewardPolicyId, amount, totalXpAfter, reason,
                referenceType, referenceId));
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

    // reward_policies 기준으로 일일 제한, 최초 보상 여부, XP/코인 지급을 한 번에 처리한다.
    @Transactional
    public RewardLog grantPolicyReward(Long userId, RewardType rewardType, RewardSource rewardSource, Long sourceId,
                                       RewardTargetType targetType, Long targetId, Long solveSessionId) {
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

    // 챕터/과목 최초 코인 보상처럼 target 단위 중복 지급을 막는다.
    private boolean isAlreadyRewarded(Long userId, RewardPolicy policy, RewardTargetType targetType, Long targetId) {
        return policy.isFirstRewardOnly()
                && targetType != null
                && targetId != null
                && rewardLogRepository.existsByUserIdAndRewardTypeAndTargetTypeAndTargetIdAndRewardStatus(
                userId,
                policy.getRewardType(),
                targetType,
                targetId,
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
        return Math.max(1, (totalXp / XP_PER_LEVEL) + 1);
    }

    // reward_type을 XP 로그의 reason 값으로 매핑한다.
    private XpReason resolveXpReason(RewardType rewardType) {
        return switch (rewardType) {
            case CONCEPT_PRACTICE_XP -> XpReason.CONCEPT_PRACTICE_XP;
            case CATEGORY_MOCK_XP -> XpReason.CATEGORY_MOCK_XP;
            case SUBJECT_MOCK_XP -> XpReason.SUBJECT_MOCK_XP;
            case DAILY_QUIZ_COMPLETE, DAILY_QUIZ_REWARD -> XpReason.DAILY_QUIZ_XP;
            case ATTENDANCE_REWARD, DAILY_QUIZ_ATTENDANCE -> XpReason.ATTENDANCE_XP;
            case LEVEL_UP_REWARD -> XpReason.LEVEL_UP_BONUS_XP;
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
            case LEVEL_UP_REWARD -> CurrencyReason.LEVEL_UP_REWARD;
            case STARTER_PACK -> CurrencyReason.STARTER_PACK;
            case AD_REWARD, AD_REWARD_COIN -> CurrencyReason.AD_REWARD;
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
