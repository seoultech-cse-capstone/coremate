import { useQuery } from "@tanstack/react-query";

import { supportApi } from "./supportApi";

export function useMyInquiries(accessToken: string | null) {
  return useQuery({
    queryKey: ["support", "inquiries"],
    queryFn: () => supportApi.getMyInquiries(accessToken as string),
    enabled: accessToken !== null,
  });
}
