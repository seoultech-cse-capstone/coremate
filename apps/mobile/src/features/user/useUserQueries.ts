import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { MyProfile } from "./types";
import { userApi } from "./userApi";

const MY_PROFILE_QUERY_KEY = ["user", "me"] as const;

export function useMyProfile(accessToken: string | null) {
  return useQuery({
    queryKey: MY_PROFILE_QUERY_KEY,
    queryFn: () => userApi.getMyProfile(accessToken as string),
    enabled: accessToken !== null,
  });
}

/** 친구에게 학습 기록 공개 여부를 바꾼다. 스위치가 바로 움직이도록 내 프로필 캐시를 먼저 고친다. */
export function useUpdateLearningPublicMutation(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (learningPublic: boolean) =>
      userApi.updatePrivacy({ learningPublic }, accessToken as string),
    onMutate: async (learningPublic) => {
      await queryClient.cancelQueries({ queryKey: MY_PROFILE_QUERY_KEY });
      const previous = queryClient.getQueryData<MyProfile>(MY_PROFILE_QUERY_KEY);
      if (previous) {
        queryClient.setQueryData<MyProfile>(MY_PROFILE_QUERY_KEY, { ...previous, learningPublic });
      }
      return { previous };
    },
    onError: (_error, _learningPublic, context) => {
      if (context?.previous) {
        queryClient.setQueryData(MY_PROFILE_QUERY_KEY, context.previous);
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: MY_PROFILE_QUERY_KEY }),
  });
}

export function useMySubjectStats(accessToken: string | null) {
  return useQuery({
    queryKey: ["user", "me", "subject-stats"],
    queryFn: () => userApi.getMySubjectStats(accessToken as string),
    enabled: accessToken !== null,
  });
}
