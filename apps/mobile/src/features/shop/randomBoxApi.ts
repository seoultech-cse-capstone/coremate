import { httpClient } from "../../shared/api/httpClient";
import type {
  OpenRandomBoxRequest,
  RandomBox,
  RandomBoxItem,
  RandomBoxOdds,
  RandomBoxOpenResult,
} from "./types";

/**
 * 랜덤박스 도메인 REST 호출. RandomBoxController는 ApiResponse 래핑 없이 DTO를 그대로 응답한다.
 */
export const randomBoxApi = {
  getRandomBoxes(accessToken: string): Promise<RandomBox[]> {
    return httpClient.get<RandomBox[]>("/api/random-box", { accessToken });
  },

  getRandomBoxItems(randomBoxId: number, accessToken: string): Promise<RandomBoxItem[]> {
    return httpClient.get<RandomBoxItem[]>(
      `/api/random-box/items?randomBoxId=${randomBoxId}`,
      { accessToken },
    );
  },

  getRandomBoxOdds(randomBoxId: number, accessToken: string): Promise<RandomBoxOdds[]> {
    return httpClient.get<RandomBoxOdds[]>(
      `/api/random-box/odds?randomBoxId=${randomBoxId}`,
      { accessToken },
    );
  },

  // 코인 차감 + 보상 지급이라 서버가 Idempotency-Key 를 필수로 요구한다.
  // 키 수명은 호출부(useIdempotencyKey)가 정한다.
  open(
    body: OpenRandomBoxRequest,
    accessToken: string,
    idempotencyKey: string,
  ): Promise<RandomBoxOpenResult> {
    return httpClient.post<RandomBoxOpenResult>("/api/random-box/open", body, {
      accessToken,
      headers: { "Idempotency-Key": idempotencyKey },
    });
  },
};
