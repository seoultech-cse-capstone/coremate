import { Text, View } from "react-native";

import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

// 노션 공개 페이지 「개인정보 처리방침」과 같은 내용이다. 한쪽을 고치면 다른 쪽도 같이 고친다.
export default function PrivacyPolicyScreen() {
  return (
    <SettingsDetailLayout title="개인정보 처리방침">
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          CoreMate(이하 “회사”)는 「개인정보 보호법」에 따라 이용자의 개인정보를 보호하고, 관련
          고충을 신속하게 처리하기 위해 다음과 같이 개인정보 처리방침을 둡니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제1조 (수집하는 개인정보 항목과 수집 방법)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원가입 시 (이메일 가입){"\n"}
          - 필수: 이름, 닉네임, 휴대폰 번호, 이메일 주소, 비밀번호(암호화하여 저장){"\n\n"}
          2. 소셜 로그인 시 (카카오·네이버·구글·Apple){"\n"}
          - 필수: 각 서비스가 제공하는 회원 식별값{"\n"}
          - 제공에 동의한 경우: 이메일 주소, 이름 또는 닉네임{"\n"}
          - Apple 로그인에서 이메일 가리기를 선택하면 Apple이 만든 대리 이메일 주소를 받습니다.
          {"\n\n"}
          3. 회원정보 수정 시 (선택){"\n"}
          - 학교명, 학교 이메일 주소{"\n\n"}
          4. 서비스 이용 중 자동으로 생성·수집되는 정보{"\n"}
          - 기기 정보: 기기 모델명, 운영체제와 버전, 앱이 만든 기기 식별값, 알림 수신용 토큰{"\n"}
          - 이용 기록: 로그인 시각, 문제 풀이 기록(정답 여부·소요 시간·힌트 사용), 출석·경험치·코인·아이템
          기록, 친구 관계{"\n"}
          - 문의하기·버그 신고에 이용자가 직접 적은 내용{"\n"}
          - 광고 관련: Android 광고 ID, 광고 프로그램(Google AdMob)이 쓰는 기기 식별값(iPhone), 광고 시청
          기록
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제2조 (개인정보의 이용 목적)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원 식별, 로그인, 기기 관리, 아이디(이메일) 찾기와 비밀번호 재설정{"\n"}
          2. 문제 풀이 기록 저장, 복습, AI 약점 분석표 생성 등 학습 서비스 제공{"\n"}
          3. 경험치·코인·출석 보상 지급과 아바타·친구 기능 제공{"\n"}
          4. 알림 발송, 문의·버그 신고 응대{"\n"}
          5. 광고 제공과 광고 시청 보상(코인) 지급 확인{"\n"}
          6. 부정 이용 방지와 서비스 안정성 확보
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제3조 (보유 및 이용 기간)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 회원 탈퇴 시 이름·휴대폰 번호·이메일·학교 정보·소셜 로그인 연결 정보를 지체 없이 파기합니다.
          이메일과 닉네임은 다른 값으로 바꿔 더 이상 개인을 알아볼 수 없게 합니다.{"\n"}
          2. 탈퇴 후 남는 풀이·보상 기록은 개인을 알아볼 수 없는 상태로 통계에만 사용합니다.{"\n"}
          3. 관련 법령에 따라 보존해야 하는 정보는 그 법령에서 정한 기간 동안 보관합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제4조 (개인정보의 제3자 제공)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회사는 이용자의 개인정보를 제3자에게 제공하지 않습니다. 다만 이용자가 미리 동의했거나 법령에
          근거가 있는 경우에는 예외로 합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제5조 (개인정보 처리의 위탁과 국외 이전)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회사는 서비스 제공을 위해 다음과 같이 개인정보 처리 업무를 맡기고 있습니다.{"\n\n"}
          1. Amazon Web Services, Inc. (서버 위치: 대한민국 서울){"\n"}
          - 업무: 서버 운영, 데이터 보관 / 항목: 제1조의 전체 항목{"\n"}
          - 서비스 이용 시 네트워크로 전송, 회원 탈퇴 또는 위탁 계약 종료 시까지 보유{"\n\n"}
          2. Google LLC (미국) — 앱 알림 발송(Firebase Cloud Messaging){"\n"}
          - 항목: 알림 수신용 토큰 / 알림 발송 시 네트워크로 전송, 위탁 계약 종료 시까지 보유{"\n\n"}
          3. Google LLC (미국) — 인증 메일 발송(Gmail){"\n"}
          - 항목: 이메일 주소 / 인증 메일 발송 시 네트워크로 전송, 위탁 계약 종료 시까지 보유{"\n\n"}
          4. Google LLC (미국) — 광고 제공과 광고 시청 보상 확인(AdMob){"\n"}
          - 항목: 광고 ID·기기 식별값, 광고 시청 기록, 회원 번호 / 광고를 불러오거나 볼 때 네트워크로 전송,
          위탁 계약 종료 시까지 보유{"\n\n"}
          국외 이전을 원하지 않으면 회원 탈퇴로 거부할 수 있습니다. 거부하면 서비스를 이용할 수 없습니다.
          광고 식별자는 제9조의 방법으로 따로 막을 수 있습니다.{"\n\n"}
          AI 약점 분석표를 만들 때 OpenAI(미국)의 AI를 사용합니다. 이때는 개념 이름과 오답률 같은 학습
          통계만 보내고, 이름·이메일·회원 번호처럼 개인을 알아볼 수 있는 정보는 보내지 않습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제6조 (만 14세 미만 아동의 개인정보)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회사는 만 14세 미만 아동의 회원가입을 받지 않습니다. 가입할 때 만 14세 이상인지 확인하며, 만 14세
          미만이 가입한 사실을 알게 되면 해당 계정의 개인정보를 지체 없이 파기합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제7조 (개인정보의 파기 절차와 방법)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          보유 기간이 끝나거나 처리 목적을 달성한 개인정보는 지체 없이 파기합니다. 전자 파일은 되살릴 수
          없는 방법으로 지웁니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제8조 (이용자의 권리와 행사 방법)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 이용자는 언제든지 자신의 개인정보를 열람·정정·삭제하거나 처리 정지를 요구할 수 있습니다.{"\n"}
          2. 앱의 「마이페이지 → 설정」에서 회원정보를 고치거나 회원탈퇴를 할 수 있습니다. 그 밖의 요청은
          「설정 → 문의하기」나 아래 보호책임자 이메일로 하면 지체 없이 처리합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제9조 (광고 식별자 사용과 거부 방법)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          앱은 광고를 보여 주고 광고 시청 보상을 확인하려고 광고 식별자를 사용합니다.{"\n"}
          - iPhone: 앱이 추적 허용을 요청하지 않으므로 광고 식별자(IDFA)는 받지 않습니다. 광고 프로그램이
          쓰는 기기 식별값은 앱을 지우면 초기화됩니다.{"\n"}
          - Android: 설정 → Google → 광고에서 광고 ID를 삭제하거나 재설정할 수 있습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제10조 (개인정보의 안전성 확보 조치)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          1. 비밀번호는 복원할 수 없는 방식으로 암호화해 저장합니다.{"\n"}
          2. 앱과 서버 사이의 통신은 암호화(HTTPS)합니다.{"\n"}
          3. 개인정보를 처리하는 시스템에는 접근 권한을 가진 사람만 들어갈 수 있게 관리합니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제11조 (개인정보 보호책임자)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          - 담당 부서: CoreMate 운영팀{"\n"}
          - 이메일: coremate.team@gmail.com{"\n\n"}
          이용자는 개인정보 보호와 관련한 문의·불만·피해 구제를 보호책임자에게 요청할 수 있습니다. 그 밖에
          개인정보 침해 신고는 개인정보침해신고센터(국번 없이 118, privacy.kisa.or.kr)에 할 수 있습니다.
        </Text>
      </View>

      <Text style={detailStyles.sectionTitle}>제12조 (처리방침의 변경)</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          이 처리방침은 2026년 10월 30일부터 적용합니다. 내용이 바뀌면 앱 공지와 이 화면으로 알립니다.
        </Text>
      </View>
    </SettingsDetailLayout>
  );
}
