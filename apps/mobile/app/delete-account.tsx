import { Pressable, Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

export default function DeleteAccountScreen() {
  return (
    <SettingsDetailLayout title="회원탈퇴">
      <Text style={detailStyles.sectionTitle}>탈퇴 전 확인</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회원탈퇴 시 계정 정보와 서비스 이용 권한이 비활성화됩니다. 학습 기록, 즐겨찾기, 보상 정보 처리 방식은 운영 정책에 따라 진행됩니다.
        </Text>

        <Pressable style={detailStyles.dangerButton}>
          <Text style={detailStyles.dangerButtonText}>회원탈퇴 진행</Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}
