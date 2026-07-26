import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

// 법무 검토 전 임시 초안. 실제 서비스 오픈 전 법률 검토 및 확정 필요.
export default function MarketingConsentScreen() {
  return (
    <SettingsDetailLayout title="마케팅 정보 수신 동의">
      <Text style={detailStyles.sectionTitle}>제1조 (수신 정보)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          동의 시 신규 콘텐츠, 이벤트, 혜택 등 마케팅 정보를 앱 내 알림 및 이메일로 받아보실
          수 있습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제2조 (동의 철회)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          본 동의는 선택 사항이며, 동의하지 않아도 서비스 이용에 제한이 없습니다. 동의 이후에도
          설정 화면의 알림 설정에서 언제든지 수신을 거부할 수 있습니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
