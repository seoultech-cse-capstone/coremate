import { useQuery } from "@tanstack/react-query";

import { currencyApi } from "./currencyApi";

export function useMyCurrency(accessToken: string | null) {
  return useQuery({
    queryKey: ["currency", "me"],
    queryFn: () => currencyApi.getMyCurrency(accessToken as string),
    enabled: accessToken !== null,
  });
}