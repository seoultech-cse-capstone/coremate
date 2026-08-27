package com.coremate.backend.shop.service;

import com.coremate.backend.avatar.inventory.service.InventoryService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.entity.CurrencyLog;
import com.coremate.backend.reward.entity.type.CurrencyReason;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.shop.dto.request.PurchaseShopProductRequest;
import com.coremate.backend.shop.dto.response.ShopProductResponse;
import com.coremate.backend.shop.dto.response.ShopPurchaseResponse;
import com.coremate.backend.shop.dto.type.ShopProductType;
import com.coremate.backend.shop.entity.ShopProduct;
import com.coremate.backend.shop.entity.ShopPurchaseLog;
import com.coremate.backend.shop.repository.ShopProductRepository;
import com.coremate.backend.shop.repository.ShopPurchaseLogRepository;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private static final String REFERENCE_TYPE_SHOP_PRODUCT = "SHOP_PRODUCT";
    private static final String ACQUIRED_SOURCE_SHOP_PURCHASE = "SHOP_PURCHASE";

    // 상점에 노출하는 상품은 3종뿐이다. 꾸미기 직판(ITEM/ROOM_THEME/ACTION_CARD)이 폐기돼
    // 폐기된 product_type 행이 DB 에 남아 있어도 조회에서 제외한다. (FR-SHOP-01)
    private static final Set<String> VISIBLE_PRODUCT_TYPES = Set.of(
            ShopProduct.PRODUCT_TYPE_RANDOM_BOX,
            ShopProduct.PRODUCT_TYPE_NICKNAME_CHANGE,
            ShopProduct.PRODUCT_TYPE_GENDER_CHANGE
    );

    private final ShopProductRepository shopProductRepository;
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;
    private final RewardService rewardService;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    public List<ShopProductResponse> getProducts() {
        return shopProductRepository.findByActiveTrueOrderByRequiredLevelAscShopProductIdAsc()
                .stream()
                .filter(this::isVisibleProduct)
                .map(this::toShopProductResponse)
                .toList();
    }

    private boolean isVisibleProduct(ShopProduct product) {
        return product.getProductType() != null
                && VISIBLE_PRODUCT_TYPES.contains(product.getProductType().toUpperCase());
    }

    // 코인 차감, 상품 지급, 구매 로그 저장을 같은 트랜잭션으로 처리한다.
    @Transactional
    public ShopPurchaseResponse purchase(Long userId, PurchaseShopProductRequest request) {
        User user = findUser(userId);
        ShopProduct product = shopProductRepository.findByShopProductIdAndActiveTrue(request.getShopProductId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.SHOP_PRODUCT_NOT_FOUND));
        validatePurchasable(user, product);
        if (!product.grantsItem()) {
            // 랜덤박스와 기능권은 별도 전용 흐름이 붙기 전까지 여기서 구매 처리하지 않는다.
            throw new CoreMateException(ErrorCode.UNSUPPORTED_SHOP_PRODUCT);
        }

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        int totalPrice = product.getPriceCoin() * quantity;
        CurrencyLog currencyLog = rewardService.spendCoin(
                userId,
                totalPrice,
                resolvePurchaseReason(product),
                REFERENCE_TYPE_SHOP_PRODUCT,
                product.getShopProductId()
        );

        inventoryService.grantItem(
                userId,
                product.getItem().getItemId(),
                quantity,
                ACQUIRED_SOURCE_SHOP_PURCHASE,
                product.getShopProductId()
        );

        ShopPurchaseLog purchaseLog = shopPurchaseLogRepository.save(
                new ShopPurchaseLog(user, product, currencyLog.getCurrencyLogId()));
        return new ShopPurchaseResponse(
                purchaseLog.getShopPurchaseId(),
                userId,
                product.getShopProductId(),
                currencyLog.getBalanceAfter(),
                purchaseLog.getPurchasedAt()
        );
    }

    private ShopProductResponse toShopProductResponse(ShopProduct product) {
        return new ShopProductResponse(
                product.getShopProductId(),
                ShopProductType.from(product.getProductType()),
                product.getItem() == null ? null : product.getItem().getItemId(),
                product.getRandomBox() == null ? null : product.getRandomBox().getRandomBoxId(),
                product.getProductName(),
                product.getPriceCoin(),
                product.getRequiredLevel(),
                product.isActive(),
                product.getCreatedAt()
        );
    }

    private void validatePurchasable(User user, ShopProduct product) {
        if (!product.isActive()) {
            throw new CoreMateException(ErrorCode.SHOP_PRODUCT_NOT_FOUND);
        }
        if (user.getLevel() < product.getRequiredLevel()) {
            throw new CoreMateException(ErrorCode.SHOP_LEVEL_REQUIRED);
        }
    }

    private CurrencyReason resolvePurchaseReason(ShopProduct product) {
        String productType = product.getProductType();
        if (ShopProduct.PRODUCT_TYPE_ROOM_THEME.equalsIgnoreCase(productType)) {
            return CurrencyReason.ROOM_THEME_PURCHASE;
        }
        if (ShopProduct.PRODUCT_TYPE_GENDER_CHANGE.equalsIgnoreCase(productType)) {
            return CurrencyReason.GENDER_CHANGE;
        }
        if (ShopProduct.PRODUCT_TYPE_NICKNAME_CHANGE.equalsIgnoreCase(productType)) {
            return CurrencyReason.NICKNAME_CHANGE;
        }
        return CurrencyReason.SHOP_ITEM_PURCHASE;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }
}
