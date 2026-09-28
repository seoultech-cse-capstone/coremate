import { router } from "expo-router";
import { ActivityIndicator, Pressable, Text, View } from "react-native";

import { useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { useMyProfile } from "../src/features/user";

// 무료 닉네임 변경은 정책상 막혀 있다(#202). 백엔드 PATCH /api/users/me/nickname 도 폐쇄돼,
// 이 화면에서 직접 바꾸려 하면 서버 오류만 난다. 상점의 닉네임 변경권으로 안내한다(#246).
export default function EditNicknameScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const profileQuery = useMyProfile(accessToken);

  return (
    <SettingsDetailLayout title="닉네임 수정">
      <Text style={detailStyles.sectionTitle}>닉네임</Text>
      <View style={detailStyles.card}>
        {profileQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : (
          <>
            <Text style={detailStyles.label}>현재 닉네임</Text>
            <View style={detailStyles.field}>
              <Text style={detailStyles.fieldText}>{profileQuery.data?.nickname}</Text>
            </View>

            <Text style={detailStyles.helperText}>
              닉네임은 상점의 닉네임 변경권으로 바꿀 수 있어요.
            </Text>

            <Pressable style={detailStyles.primaryButton} onPress={() => router.push("/avatar-shop")}>
              <Text style={detailStyles.primaryButtonText}>상점에서 닉네임 바꾸기</Text>
            </Pressable>
          </>
        )}
      </View>
    </SettingsDetailLayout>
  );
}
