import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

// 법무 검토 전 임시 초안. 실제 서비스 오픈 전 법률 검토 및 확정 필요.
export default function TermsPoliciesScreen() {
  return (
    <SettingsDetailLayout title="서비스 이용약관">
      <Text style={detailStyles.sectionTitle}>제1조 (목적)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          본 약관은 CoreMate(이하 "회사")가 제공하는 CS 학습 서비스(이하 "서비스")의 이용과
          관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제2조 (정의)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. "서비스"란 회사가 제공하는 문제 풀이, 학습 기록, 약점 분석, 출석·보상, 아바타,
          친구 등 CoreMate 앱 내 모든 기능을 의미합니다.{"\n\n"}
          2. "회원"이란 본 약관에 동의하고 회사와 서비스 이용계약을 체결한 자를 의미합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제3조 (약관의 효력 및 변경)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 본 약관은 서비스 화면에 게시하거나 기타의 방법으로 회원에게 공지함으로써 효력이
          발생합니다.{"\n\n"}
          2. 회사는 관련 법령을 위배하지 않는 범위에서 본 약관을 변경할 수 있으며, 변경된
          약관은 공지 후 효력이 발생합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제4조 (서비스의 제공 및 변경)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회사는 문제 풀이, 학습 기록 저장, 약점 분석, 출석 및 보상, 친구, 아바타 등의
          서비스를 제공합니다.{"\n\n"}
          2. 회사는 서비스의 내용, 운영상 또는 기술상 필요에 따라 제공하는 서비스의 전부 또는
          일부를 변경할 수 있습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제5조 (회원의 의무)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원은 가입 시 정확한 정보를 제공해야 하며, 정보가 변경된 경우 이를 지체 없이
          갱신해야 합니다.{"\n\n"}
          2. 회원은 타인의 계정을 도용하거나 서비스를 부정한 목적으로 이용해서는 안 됩니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제6조 (계약 해지 및 이용 제한)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원은 언제든지 설정 화면을 통해 회원 탈퇴를 요청할 수 있습니다.{"\n\n"}
          2. 회사는 회원이 본 약관을 위반한 경우 사전 통지 후 서비스 이용을 제한하거나 이용계약을
          해지할 수 있습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제7조 (면책조항)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회사는 천재지변, 불가항력적 사유 또는 회원의 귀책사유로 인해 서비스를 제공할 수 없는
          경우 이에 대한 책임을 지지 않습니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
