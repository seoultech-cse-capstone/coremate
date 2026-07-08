import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

export default function PrivacyPolicyScreen() {
  return (
    <SettingsDetailLayout title="개인정보 처리방침">
      <Text style={detailStyles.sectionTitle}>수집 정보</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          이메일, 닉네임, 학교 정보, 학습 기록, 즐겨찾기, 아바타 및 아이템 정보를 서비스 제공 범위 안에서 사용합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>데이터 관리</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          사용자는 본인 계정 정보와 서비스 이용 기록에 대한 조회, 수정, 탈퇴를 요청할 수 있습니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
