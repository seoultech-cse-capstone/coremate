import { httpClient } from "../../shared/api/httpClient";
import type { PurchaseShopProductRequest, ShopProduct, ShopPurchaseResult } from "./types";

/**
 * 상점 도메인 REST 호출. ShopController는 ApiResponse 래핑 없이 DTO를 그대로 응답한다.
 */
export const shopApi = {
  getProducts(accessToken: string): Promise<ShopProduct[]> {
    return httpClient.get<ShopProduct[]>("/api/shop/products", { accessToken });
  },

  purchase(
    body: PurchaseShopProductRequest,
    accessToken: string,
  ): Promise<ShopPurchaseResult> {
    return httpClient.post<ShopPurchaseResult>("/api/shop/purchase", body, {
      accessToken,
    });
  },
};
