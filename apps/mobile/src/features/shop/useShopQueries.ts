import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

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

  return useMutation({
    mutationFn: (body: PurchaseShopProductRequest) =>
      shopApi.purchase(body, accessToken as string),
    onSuccess: () => {
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

  return useMutation({
    mutationFn: (body: OpenRandomBoxRequest) =>
      randomBoxApi.open(body, accessToken as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["currency", "me"] });
      queryClient.invalidateQueries({ queryKey: ["avatar", "inventory"] });
    },
  });
}
