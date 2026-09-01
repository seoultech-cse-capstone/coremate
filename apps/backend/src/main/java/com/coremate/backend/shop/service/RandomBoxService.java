package com.coremate.backend.shop.service;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.catalog.type.AcquiredSource;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.avatar.inventory.repository.UserItemRepository;
import com.coremate.backend.avatar.inventory.service.ItemGrantResult;
import com.coremate.backend.avatar.inventory.service.ItemGrantService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.shop.dto.request.OpenRandomBoxRequest;
import com.coremate.backend.shop.dto.response.RandomBoxItemResponse;
import com.coremate.backend.shop.dto.response.RandomBoxOddsResponse;
import com.coremate.backend.shop.dto.response.RandomBoxOpenResponse;
import com.coremate.backend.shop.dto.response.RandomBoxResponse;
import com.coremate.backend.shop.dto.type.RandomBoxGrade;
import com.coremate.backend.shop.entity.RandomBox;
import com.coremate.backend.shop.entity.RandomBoxItem;
import com.coremate.backend.shop.entity.RandomBoxOpenLog;
import com.coremate.backend.shop.entity.ShopProduct;
import com.coremate.backend.shop.repository.RandomBoxItemRepository;
import com.coremate.backend.shop.repository.RandomBoxOpenLogRepository;
import com.coremate.backend.shop.repository.RandomBoxRepository;
import com.coremate.backend.shop.repository.ShopProductRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RandomBoxService {

    private static final String REFERENCE_TYPE_RANDOM_BOX = "RANDOM_BOX";
    private static final BigDecimal MIN_PROBABILITY_SUM = BigDecimal.ONE;

    // 등급별 확률 공개용 버킷. 아이템 보상은 등급(COMMON/RARE/EPIC), 코인 보상은 COIN 으로 묶는다.
    private static final String ODDS_GRADE_COIN = "COIN";
    private static final List<String> ODDS_GRADE_ORDER = List.of("COMMON", "RARE", "EPIC", "LEGENDARY", ODDS_GRADE_COIN);

    private final RandomBoxRepository randomBoxRepository;
    private final RandomBoxItemRepository randomBoxItemRepository;
    private final RandomBoxOpenLogRepository randomBoxOpenLogRepository;
    private final ShopProductRepository shopProductRepository;
    private final RewardService rewardService;
    private final ItemGrantService itemGrantService;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<RandomBoxResponse> getRandomBoxes() {
        return randomBoxRepository.findByActiveTrueOrderByRequiredLevelAscRandomBoxIdAsc()
                .stream()
                .map(this::toRandomBoxResponse)
                .toList();
    }

    public List<RandomBoxItemResponse> getRandomBoxItems(Long randomBoxId) {
        List<RandomBoxItem> items = randomBoxId == null
                ? getAllActiveRandomBoxItems()
                : getActiveRandomBoxItems(randomBoxId);
        return items.stream()
                .map(this::toRandomBoxItemResponse)
                .toList();
    }

    // 등급별 확률 공개. 아이템 1행씩인 random_box_items 를 등급 버킷으로 합산해
    // 하 0.45 / 중 0.20 / 코인 0.20 / 상 0.15 처럼 내려준다.
    // 저장된 행을 그대로 합치는 것이라 표시 확률의 합이 DB 저장 확률의 합과 항상 일치한다.
    public List<RandomBoxOddsResponse> getRandomBoxOdds(Long randomBoxId) {
        List<RandomBoxItem> items = randomBoxId == null
                ? getAllActiveRandomBoxItems()
                : getActiveRandomBoxItems(randomBoxId);

        Map<String, BigDecimal> byGrade = new LinkedHashMap<>();
        for (RandomBoxItem item : items) {
            byGrade.merge(gradeOf(item), item.getProbability(), BigDecimal::add);
        }
        return byGrade.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> gradeOrder(entry.getKey())))
                .map(entry -> new RandomBoxOddsResponse(entry.getKey(), entry.getValue().toPlainString()))
                .toList();
    }

    // 보상의 등급 버킷 이름. 코인은 COIN, 아이템은 그 아이템의 rarity 를 쓴다.
    private String gradeOf(RandomBoxItem item) {
        if (item.isCoinReward()) {
            return ODDS_GRADE_COIN;
        }
        AvatarItem avatarItem = item.getItem();
        String rarity = avatarItem == null ? null : avatarItem.getRarity();
        return rarity == null ? "UNKNOWN" : rarity.trim().toUpperCase();
    }

    private int gradeOrder(String grade) {
        int index = ODDS_GRADE_ORDER.indexOf(grade);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    // 랜덤박스 오픈은 코인 차감, 확률 보상 결정, 지급, 로그 저장을 하나로 묶는다.
    @Transactional
    public RandomBoxOpenResponse open(Long userId, OpenRandomBoxRequest request) {
        User user = findUser(userId);
        RandomBox randomBox = randomBoxRepository.findByRandomBoxIdAndActiveTrue(request.getRandomBoxId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.RANDOM_BOX_NOT_FOUND));
        validateLevel(user, randomBox);

        ShopProduct randomBoxProduct = shopProductRepository
                .findFirstByRandomBox_RandomBoxIdAndActiveTrueOrderByPriceCoinAsc(randomBox.getRandomBoxId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.SHOP_PRODUCT_NOT_FOUND));
        validateProductForRandomBox(user, randomBoxProduct);
        CurrencyLog spendLog = rewardService.spendCoin(
                userId,
                randomBoxProduct.getPriceCoin(),
                CurrencyReason.RANDOM_BOX_PURCHASE,
                REFERENCE_TYPE_RANDOM_BOX,
                randomBox.getRandomBoxId()
        );

        RandomBoxItem selectedReward = pickReward(randomBox.getRandomBoxId());
        RandomBoxOpenLog openLog = grantRewardAndSaveLog(user, randomBox, spendLog, selectedReward);
        return toOpenResponse(openLog);
    }

    private RandomBoxOpenLog grantRewardAndSaveLog(
            User user,
            RandomBox randomBox,
            CurrencyLog spendLog,
            RandomBoxItem selectedReward
    ) {
        if (selectedReward.isItemReward()) {
            AvatarItem item = selectedReward.getItem();
            if (item == null || !item.isActive()) {
                throw new CoreMateException(ErrorCode.INVALID_RANDOM_BOX_POLICY);
            }
            // 지급은 avatar.inventory 의 단일 진입점에 맡긴다. 중복 분기(가구 수량 증가/그 외 환급)는
            // grant() 안에 있어 여기서는 item_type / rarity 를 알 필요가 없다.
            ItemGrantResult grantResult = itemGrantService.grant(
                    user.getUserId(),
                    item.getItemId(),
                    AcquiredSource.RANDOM_BOX,
                    randomBox.getRandomBoxId()
            );
            if (grantResult.isRefundable()) {
                // 이미 가진 아이템이 또 나왔다. user_items 는 그대로 두고 등급별 코인만 환급한다.
                // 환급액은 grant() 가 등급에서 계산해 내려준 값을 그대로 쓴다 — 여기서 다시 계산하지 않는다.
                rewardService.earnCoin(
                        user.getUserId(),
                        grantResult.refundCoin(),
                        CurrencyReason.DUPLICATE_ITEM_REFUND,
                        REFERENCE_TYPE_RANDOM_BOX,
                        randomBox.getRandomBoxId()
                );
                // 뽑힌 보상은 아이템이지만 새 user_items 행은 없다(환급으로 대체). rewardCoinAmount 로 환급액을 남긴다.
                return randomBoxOpenLogRepository.save(new RandomBoxOpenLog(
                        user,
                        randomBox,
                        spendLog.getCurrencyLogId(),
                        RandomBoxItem.REWARD_TYPE_ITEM,
                        item,
                        null,
                        grantResult.refundCoin()
                ));
            }
            // 신규 지급 또는 가구 수량 증가. 생긴/늘어난 user_items 행을 로그에 연결한다.
            UserItem userItem = userItemRepository.getReferenceById(grantResult.userItemId());
            return randomBoxOpenLogRepository.save(new RandomBoxOpenLog(
                    user,
                    randomBox,
                    spendLog.getCurrencyLogId(),
                    RandomBoxItem.REWARD_TYPE_ITEM,
                    item,
                    userItem,
                    null
            ));
        }

        if (selectedReward.isCoinReward()) {
            Integer coinAmount = selectedReward.getCoinAmount();
            if (coinAmount == null || coinAmount <= 0) {
                throw new CoreMateException(ErrorCode.INVALID_RANDOM_BOX_POLICY);
            }
            rewardService.earnCoin(
                    user.getUserId(),
                    coinAmount,
                    CurrencyReason.RANDOM_BOX_REWARD,
                    REFERENCE_TYPE_RANDOM_BOX,
                    randomBox.getRandomBoxId()
            );
            return randomBoxOpenLogRepository.save(new RandomBoxOpenLog(
                    user,
                    randomBox,
                    spendLog.getCurrencyLogId(),
                    RandomBoxItem.REWARD_TYPE_COIN,
                    null,
                    null,
                    coinAmount
            ));
        }

        throw new CoreMateException(ErrorCode.INVALID_RANDOM_BOX_POLICY);
    }

    // random_box_items 는 이미 아이템 1개당 1행이라, 등급 버킷을 고르고 그 안에서 다시 뽑는
    // 2단 추첨이 필요 없다. 전체 행을 확률로 누적해 단 한 번만 가중 추첨한다. (FR-SHOP-04)
    private RandomBoxItem pickReward(Long randomBoxId) {
        List<RandomBoxItem> rewards = randomBoxItemRepository
                .findByRandomBox_RandomBoxIdOrderByRandomBoxItemIdAsc(randomBoxId)
                .stream()
                .filter(this::hasValidRewardShape)
                .toList();
        if (rewards.isEmpty()) {
            throw new CoreMateException(ErrorCode.RANDOM_BOX_REWARD_NOT_FOUND);
        }

        BigDecimal totalProbability = rewards.stream()
                .map(RandomBoxItem::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalProbability.compareTo(MIN_PROBABILITY_SUM) < 0) {
            throw new CoreMateException(ErrorCode.INVALID_RANDOM_BOX_POLICY);
        }

        BigDecimal ticket = BigDecimal.valueOf(secureRandom.nextDouble()).multiply(totalProbability);
        BigDecimal cumulative = BigDecimal.ZERO;
        for (RandomBoxItem reward : rewards) {
            cumulative = cumulative.add(reward.getProbability());
            if (ticket.compareTo(cumulative) < 0) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private List<RandomBoxItem> getAllActiveRandomBoxItems() {
        List<RandomBoxItem> items = new ArrayList<>();
        for (RandomBox randomBox : randomBoxRepository.findByActiveTrueOrderByRequiredLevelAscRandomBoxIdAsc()) {
            items.addAll(randomBoxItemRepository.findByRandomBox_RandomBoxIdOrderByRandomBoxItemIdAsc(
                    randomBox.getRandomBoxId()));
        }
        return items;
    }

    private List<RandomBoxItem> getActiveRandomBoxItems(Long randomBoxId) {
        // 비활성 랜덤박스의 보상 구성은 일반 사용자 화면에 노출하지 않는다.
        RandomBox randomBox = randomBoxRepository.findByRandomBoxIdAndActiveTrue(randomBoxId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.RANDOM_BOX_NOT_FOUND));
        return randomBoxItemRepository.findByRandomBox_RandomBoxIdOrderByRandomBoxItemIdAsc(
                randomBox.getRandomBoxId());
    }

    private boolean hasValidRewardShape(RandomBoxItem item) {
        if (item.getProbability() == null || item.getProbability().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (item.isItemReward()) {
            return item.getItem() != null;
        }
        return item.isCoinReward() && item.getCoinAmount() != null && item.getCoinAmount() > 0;
    }

    private RandomBoxResponse toRandomBoxResponse(RandomBox randomBox) {
        return new RandomBoxResponse(
                randomBox.getRandomBoxId(),
                randomBox.getBoxName(),
                RandomBoxGrade.from(randomBox.getBoxGrade()),
                randomBox.getRequiredLevel(),
                resolveRandomBoxPrice(randomBox.getRandomBoxId()),
                randomBox.isActive()
        );
    }

    private RandomBoxItemResponse toRandomBoxItemResponse(RandomBoxItem item) {
        AvatarItem avatarItem = item.getItem();
        return new RandomBoxItemResponse(
                avatarItem == null ? null : avatarItem.getItemId(),
                avatarItem == null ? null : avatarItem.getItemName(),
                item.getRewardType(),
                item.getCoinAmount(),
                item.getProbability().toPlainString()
        );
    }

    private RandomBoxOpenResponse toOpenResponse(RandomBoxOpenLog openLog) {
        // 아이템 보상이면 그 아이템의 rarity 를, 코인 보상이면 "COIN" 을 등급으로 내려준다.
        // (프론트가 상자 들썩임 연출에 등급 색을 입히는 데 쓴다)
        String rewardRarity = openLog.getRewardItem() == null
                ? RandomBoxItem.REWARD_TYPE_COIN
                : openLog.getRewardItem().getRarity();
        return new RandomBoxOpenResponse(
                openLog.getRandomBoxOpenLogId(),
                openLog.getUser().getUserId(),
                openLog.getRandomBox().getRandomBoxId(),
                openLog.getRewardType(),
                openLog.getRewardItem() == null ? null : openLog.getRewardItem().getItemId(),
                openLog.getRewardCoinAmount(),
                rewardRarity,
                openLog.getOpenedAt()
        );
    }

    private void validateLevel(User user, RandomBox randomBox) {
        if (user.getLevel() < randomBox.getRequiredLevel()) {
            throw new CoreMateException(ErrorCode.SHOP_LEVEL_REQUIRED);
        }
    }

    private void validateProductForRandomBox(User user, ShopProduct product) {
        if (!product.isRandomBoxProduct() || product.getRandomBox() == null) {
            throw new CoreMateException(ErrorCode.SHOP_PRODUCT_NOT_FOUND);
        }
        if (user.getLevel() < product.getRequiredLevel()) {
            throw new CoreMateException(ErrorCode.SHOP_LEVEL_REQUIRED);
        }
    }

    private Integer resolveRandomBoxPrice(Long randomBoxId) {
        return shopProductRepository.findFirstByRandomBox_RandomBoxIdAndActiveTrueOrderByPriceCoinAsc(randomBoxId)
                .map(ShopProduct::getPriceCoin)
                .orElse(null);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }
}
