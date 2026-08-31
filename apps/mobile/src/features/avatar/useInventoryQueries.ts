import { useQuery } from "@tanstack/react-query";

import { inventoryApi } from "./inventoryApi";

export function useInventory(accessToken: string | null) {
  return useQuery({
    queryKey: ["avatar", "inventory"],
    queryFn: () => inventoryApi.getInventory(accessToken as string),
    enabled: accessToken !== null,
  });
}
