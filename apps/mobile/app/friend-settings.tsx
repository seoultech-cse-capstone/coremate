import { ActivityIndicator, Alert, Pressable, StyleSheet, Switch, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { AvatarFaceThumbnail, toEquippedMap } from "../src/features/avatar";
import {
  useBlockedUsersQuery,
  useUnblockUserMutation,
  type BlockedUser,
} from "../src/features/friend";
import { getAvatarColor } from "../src/features/friend/avatarColor";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { useMyProfile, useUpdateLearningPublicMutation } from "../src/features/user";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

/** 친구 설정. 학습 기록 공개 여부를 바꾸고, 차단한 사용자를 보고 차단을 푼다. */
export default function FriendSettingsScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const { data: profile } = useMyProfile(accessToken);
  const updateLearningPublic = useUpdateLearningPublicMutation(accessToken);
  const { data: blockedUsers, isPending, isError } = useBlockedUsersQuery();
  const unblock = useUnblockUserMutation();

  const confirmUnblock = (user: BlockedUser) => {
    Alert.alert(
      "차단 해제",
      `${user.nickname} 님의 차단을 풀까요?\n다시 친구가 되려면 친구 요청부터 해야 해요.`,
      [
        { text: "취소", style: "cancel" },
        {
          text: "해제",
          onPress: () =>
            unblock.mutate(user.userId, {
              onError: (error) => Alert.alert("차단 해제", getApiErrorMessage(error)),
            }),
        },
      ],
    );
  };

  return (
    <SettingsDetailLayout title="친구 설정">
      <Text style={detailStyles.sectionTitle}>공개 범위</Text>
      <View style={detailStyles.card}>
        {profile ? (
          <>
            <View style={styles.switchRow}>
              <Text style={styles.switchText}>친구에게 학습 기록 공개</Text>
              <Switch
                onValueChange={(learningPublic) =>
                  updateLearningPublic.mutate(learningPublic, {
                    onError: (error) => Alert.alert("공개 범위", getApiErrorMessage(error)),
                  })
                }
                thumbColor="#ffffff"
                trackColor={{ false: "#CBD5E1", true: "#6D5DFB" }}
                value={profile.learningPublic !== false}
              />
            </View>
            <Text style={styles.switchHelp}>
              끄면 친구가 내 프로필에서 과목별 통계와 학습 기록(풀이 시간·맞힌 문제·완료한 개념)을
              볼 수 없어요. 레벨·출석·아바타·방은 계속 보여요.
            </Text>
          </>
        ) : (
          <ActivityIndicator color="#6D5DFB" style={styles.loading} />
        )}
      </View>

      <Text style={detailStyles.sectionTitle}>차단한 사용자</Text>

      <View style={detailStyles.card}>
        {isPending ? (
          <ActivityIndicator color="#6D5DFB" style={styles.loading} />
        ) : isError ? (
          <Text style={detailStyles.bodyText}>
            차단 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
          </Text>
        ) : blockedUsers.length === 0 ? (
          <Text style={detailStyles.bodyText}>차단한 사용자가 없어요.</Text>
        ) : (
          blockedUsers.map((user, index) => (
            <View key={user.userId} style={[styles.row, index > 0 && styles.rowDivider]}>
              <AvatarFaceThumbnail
                backgroundColor={getAvatarColor(user.nickname).background}
                equipped={toEquippedMap(user.equippedItems)}
                size={40}
              />
              <View style={styles.rowInfo}>
                <Text numberOfLines={1} style={styles.nickname}>
                  {user.nickname}
                </Text>
                <Text style={styles.meta}>{user.blockedAt.slice(0, 10).replace(/-/g, ".")} 차단</Text>
              </View>
              <Pressable
                disabled={unblock.isPending}
                onPress={() => confirmUnblock(user)}
                style={styles.unblockButton}
              >
                <Text style={styles.unblockButtonText}>차단 해제</Text>
              </Pressable>
            </View>
          ))
        )}
      </View>

      <Text style={detailStyles.helperText}>
        차단한 사용자는 나를 검색하거나 친구 요청을 보낼 수 없어요. 차단한 사실은 상대에게 알리지
        않아요. 친구를 차단하려면 친구 프로필에서 차단하기를 누르세요.
      </Text>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  loading: {
    paddingVertical: 24,
  },
  switchRow: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  switchText: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 14,
  },
  switchHelp: {
    marginTop: 4,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingVertical: 12,
  },
  rowDivider: {
    borderTopWidth: 1,
    borderTopColor: "#EEF2F7",
  },
  rowInfo: {
    flex: 1,
  },
  nickname: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  meta: {
    marginTop: 3,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  unblockButton: {
    borderRadius: 12,
    backgroundColor: "#F1F5F9",
    paddingHorizontal: 12,
    paddingVertical: 9,
  },
  unblockButtonText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
});
