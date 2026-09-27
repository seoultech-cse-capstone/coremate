import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { useIdempotencyKey } from "../../shared/hooks/useIdempotencyKey";
import { randomBoxApi } from "./randomBoxApi";
import { shopApi } from "./shopApi";
import type { OpenRandomBoxRequest, PurchaseShopProductRequest } from "./types";

export function useShopProducts(accessToken: string | null) {
  return useQuery({
    queryKey: ["shop", "products"],
    queryFn: () => shopApi.getProducts(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function usePurchaseMutation(accessToken: string | null) {
  const queryClient = useQueryClient();
  const idempotency = useIdempotencyKey();

  return useMutation({
    // 같은 상품을 연타하거나 실패 후 다시 누르면 같은 키가 나가 이중 차감이 막힌다.
    // 성공하면 키를 비워 같은 상품을 한 번 더 사는 것은 막지 않는다.
    mutationFn: (body: PurchaseShopProductRequest) =>
      shopApi.purchase(
        body,
        accessToken as string,
        idempotency.keyFor(`shop-${body.shopProductId}`),
      ),
    onSuccess: () => {
      idempotency.clear();
      queryClient.invalidateQueries({ queryKey: ["currency", "me"] });
    },
  });
}

export function useRandomBoxes(accessToken: string | null) {
  return useQuery({
    queryKey: ["random-box", "list"],
    queryFn: () => randomBoxApi.getRandomBoxes(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useRandomBoxItems(randomBoxId: number | null, accessToken: string | null) {
  return useQuery({
    queryKey: ["random-box", "items", randomBoxId],
    queryFn: () => randomBoxApi.getRandomBoxItems(randomBoxId as number, accessToken as string),
    enabled: randomBoxId !== null && accessToken !== null,
  });
}

export function useRandomBoxOdds(randomBoxId: number | null, accessToken: string | null) {
  return useQuery({
    queryKey: ["random-box", "odds", randomBoxId],
    queryFn: () => randomBoxApi.getRandomBoxOdds(randomBoxId as number, accessToken as string),
    enabled: randomBoxId !== null && accessToken !== null,
  });
}

export function useOpenRandomBoxMutation(accessToken: string | null) {
  const queryClient = useQueryClient();
  const idempotency = useIdempotencyKey();

  return useMutation({
    mutationFn: (body: OpenRandomBoxRequest) =>
      randomBoxApi.open(
        body,
        accessToken as string,
        idempotency.keyFor(`box-${body.randomBoxId}`),
      ),
    onSuccess: () => {
      idempotency.clear();
      queryClient.invalidateQueries({ queryKey: ["currency", "me"] });
      queryClient.invalidateQueries({ queryKey: ["avatar", "inventory"] });
    },
  });
}
