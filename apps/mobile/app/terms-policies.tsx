import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

export default function TermsPoliciesScreen() {
  return (
    <SettingsDetailLayout title="약관 및 정책">
      <Text style={detailStyles.sectionTitle}>서비스 약관</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          CoreMate 서비스 이용약관, 보상 정책, 상점 이용 정책을 이 화면에서 확인할 수 있도록 구성할 예정입니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>운영 정책</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          문제 콘텐츠 이용, 즐겨찾기, 학습 기록, 코인 사용과 관련된 기본 정책을 표시합니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
