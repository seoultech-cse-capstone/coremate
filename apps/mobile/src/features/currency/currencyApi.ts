import { httpClient } from "../../shared/api/httpClient";
import type { UserCurrency } from "./types";

/**
 * 재화(코인) REST 호출. 다른 도메인과 달리 이 컨트롤러는 ApiResponse로 감싸지
 * 않고 DTO를 그대로 반환한다(백엔드 CurrencyController 확인).
 */
export const currencyApi = {
  getMyCurrency(accessToken: string): Promise<UserCurrency> {
    return httpClient.get<UserCurrency>("/api/currency/me", { accessToken });
  },
};