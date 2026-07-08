import { Pressable, Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

export default function EditProfileScreen() {
  return (
    <SettingsDetailLayout title="회원정보 수정">
      <Text style={detailStyles.sectionTitle}>프로필 정보</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.label}>닉네임</Text>
        <View style={detailStyles.field}>
          <Text style={detailStyles.fieldText}>코어메이트</Text>
        </View>

        <Text style={[detailStyles.label, { marginTop: 16 }]}>학교</Text>
        <View style={detailStyles.field}>
          <Text style={detailStyles.fieldText}>서울과학기술대학교</Text>
        </View>

        <Text style={[detailStyles.label, { marginTop: 16 }]}>학과</Text>
        <View style={detailStyles.field}>
          <Text style={detailStyles.fieldText}>컴퓨터공학과</Text>
        </View>

        <Pressable style={detailStyles.primaryButton}>
          <Text style={detailStyles.primaryButtonText}>저장</Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}
