package com.coremate.backend.shop.service;

import com.coremate.backend.avatar.entity.AvatarItem;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.inventory.entity.UserItem;
import com.coremate.backend.inventory.service.InventoryService;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.shop.dto.request.OpenRandomBoxRequest;
import com.coremate.backend.shop.dto.response.RandomBoxItemResponse;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RandomBoxService {

    private static final String REFERENCE_TYPE_RANDOM_BOX = "RANDOM_BOX";
    private static final String ACQUIRED_SOURCE_RANDOM_BOX = "RANDOM_BOX";
    private static final BigDecimal MIN_PROBABILITY_SUM = BigDecimal.ONE;

    private final RandomBoxRepository randomBoxRepository;
    private final RandomBoxItemRepository randomBoxItemRepository;
    private final RandomBoxOpenLogRepository randomBoxOpenLogRepository;
    private final ShopProductRepository shopProductRepository;
    private final RewardService rewardService;
    private final InventoryService inventoryService;
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
            UserItem userItem = inventoryService.grantItemEntity(
                    user.getUserId(),
                    item.getItemId(),
                    1,
                    ACQUIRED_SOURCE_RANDOM_BOX,
                    randomBox.getRandomBoxId()
            );
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
        return new RandomBoxOpenResponse(
                openLog.getRandomBoxOpenLogId(),
                openLog.getUser().getUserId(),
                openLog.getRandomBox().getRandomBoxId(),
                openLog.getRewardType(),
                openLog.getRewardItem() == null ? null : openLog.getRewardItem().getItemId(),
                openLog.getRewardCoinAmount(),
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
