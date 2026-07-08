import { Pressable, Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

export default function EditNicknameScreen() {
  return (
    <SettingsDetailLayout title="닉네임 수정">
      <Text style={detailStyles.sectionTitle}>닉네임</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.label}>현재 닉네임</Text>
        <View style={detailStyles.field}>
          <Text style={detailStyles.fieldText}>코어메이트</Text>
        </View>

        <Text style={[detailStyles.label, { marginTop: 16 }]}>새 닉네임</Text>
        <View style={detailStyles.field}>
          <Text style={detailStyles.fieldText}>새 닉네임 입력 영역</Text>
        </View>

        <Text style={detailStyles.helperText}>
          닉네임은 다른 사용자와 중복될 수 없으며, 변경 정책에 따라 제한될 수 있습니다.
        </Text>

        <Pressable style={detailStyles.primaryButton}>
          <Text style={detailStyles.primaryButtonText}>중복 확인 및 저장</Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}
