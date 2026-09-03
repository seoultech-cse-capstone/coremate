package com.coremate.backend.shop.service;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.idempotency.IdempotencyGuard;
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
    private static final int MAX_NICKNAME_LENGTH = 6;

    // 상점에 노출하는 상품은 2종뿐이다. 꾸미기 직판(ITEM/ROOM_THEME/ACTION_CARD)과 성별 변경권(08-28 폐기)이
    // 빠져, 폐기된 product_type 행이 DB 에 남아 있어도 조회에서 제외한다. (FR-SHOP-01)
    private static final Set<String> VISIBLE_PRODUCT_TYPES = Set.of(
            ShopProduct.PRODUCT_TYPE_RANDOM_BOX,
            ShopProduct.PRODUCT_TYPE_NICKNAME_CHANGE
    );

    private final ShopProductRepository shopProductRepository;
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;
    private final RewardService rewardService;
    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;

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

    // 변경권 구매는 코인 차감 + 효과 즉시 적용 + 구매 로그를 같은 트랜잭션으로 처리한다.
    // 하나라도 실패하면 전부 롤백된다. (EX-ITEM-001)
    // 꾸미기 아이템 직판은 폐기됐고 랜덤박스는 /random-box/open 전용 흐름이라, 여기서 즉시 처리하는 상품은
    // 닉네임 변경권뿐이다. (성별 변경권은 성별 구분 폐기(08-28)로 없어졌다)
    @Transactional
    public ShopPurchaseResponse purchase(Long userId, PurchaseShopProductRequest request, String idempotencyKey) {
        // 더블클릭·재시도로 코인이 이중 차감되지 않도록 트랜잭션 맨 앞에서 멱등키를 등록한다. (#168)
        idempotencyGuard.register(userId, idempotencyKey, "SHOP_PURCHASE");
        User user = findUser(userId);
        ShopProduct product = shopProductRepository.findByShopProductIdAndActiveTrue(request.getShopProductId())
                .orElseThrow(() -> new CoreMateException(ErrorCode.SHOP_PRODUCT_NOT_FOUND));
        validatePurchasable(user, product);

        if (!ShopProduct.PRODUCT_TYPE_NICKNAME_CHANGE.equalsIgnoreCase(product.getProductType())) {
            throw new CoreMateException(ErrorCode.UNSUPPORTED_SHOP_PRODUCT);
        }
        // 코인을 빼기 전에 입력부터 검증해 잘못된 요청은 차감 없이 막는다.
        validateNewNickname(user, request.getNewNickname());

        CurrencyLog currencyLog = rewardService.spendCoin(
                userId,
                product.getPriceCoin(),
                CurrencyReason.NICKNAME_CHANGE,
                REFERENCE_TYPE_SHOP_PRODUCT,
                product.getShopProductId()
        );

        // 변경권은 1회용이라 구매 즉시 효과를 적용한다.
        user.updateNickname(request.getNewNickname().trim());

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

    // 닉네임 변경권: 가입과 같은 규칙(최대 6글자) + 중복 금지. 같은 닉네임으로의 변경은 막는다.
    private void validateNewNickname(User user, String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        String trimmed = newNickname.trim();
        if (trimmed.length() > MAX_NICKNAME_LENGTH || trimmed.equals(user.getNickname())) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        if (userRepository.existsByNickname(trimmed)) {
            throw new CoreMateException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }
}
