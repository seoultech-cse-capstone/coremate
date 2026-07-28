import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

// 법무 검토 전 임시 초안. 실제 서비스 오픈 전 법률 검토 및 확정 필요.
export default function PrivacyPolicyScreen() {
  return (
    <SettingsDetailLayout title="개인정보 처리방침">
      <Text style={detailStyles.sectionTitle}>제1조 (수집하는 개인정보 항목)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          CoreMate는 회원가입 및 서비스 제공을 위해 다음과 같은 정보를 수집합니다.{"\n\n"}
          - 필수: 이메일, 비밀번호, 닉네임{"\n"}
          - 선택: 학교명
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제2조 (개인정보의 수집 및 이용 목적)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원 식별 및 로그인{"\n"}
          2. 학습 기록, 약점 분석, 출석·보상 지급 등 개인화 서비스 제공{"\n"}
          3. 친구, 아바타 등 부가 기능 제공
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제3조 (개인정보의 보유 및 이용 기간)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회원 탈퇴 시 관련 법령에 따라 일정 기간 보관해야 하는 정보를 제외하고 지체 없이
          파기합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제4조 (개인정보의 제3자 제공)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회사는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않으며, 법령에 근거가 있거나
          이용자가 사전에 동의한 경우에만 제공합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제5조 (이용자의 권리)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회원은 언제든지 본인의 개인정보를 조회, 수정할 수 있으며, 회원 탈퇴를 통해 개인정보
          삭제를 요청할 수 있습니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
