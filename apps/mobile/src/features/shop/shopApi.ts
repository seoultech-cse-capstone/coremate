import { httpClient } from "../../shared/api/httpClient";
import type { PurchaseShopProductRequest, ShopProduct, ShopPurchaseResult } from "./types";

/**
 * 상점 도메인 REST 호출. ShopController는 ApiResponse 래핑 없이 DTO를 그대로 응답한다.
 */
export const shopApi = {
  getProducts(accessToken: string): Promise<ShopProduct[]> {
    return httpClient.get<ShopProduct[]>("/api/shop/products", { accessToken });
  },

  // 코인이 차감되는 요청이라 서버가 Idempotency-Key 를 필수로 요구한다. 키 수명은
  // 호출부(useIdempotencyKey)가 정한다 — 같은 상품을 다시 사는 건 막으면 안 되고,
  // 연타·재시도는 막아야 하기 때문이다.
  purchase(
    body: PurchaseShopProductRequest,
    accessToken: string,
    idempotencyKey: string,
  ): Promise<ShopPurchaseResult> {
    return httpClient.post<ShopPurchaseResult>("/api/shop/purchase", body, {
      accessToken,
      headers: { "Idempotency-Key": idempotencyKey },
    });
  },
};
