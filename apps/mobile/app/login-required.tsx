import { LoginRequiredGuide } from "../src/features/auth";

// SCR-COMMON-02 로그인 필요 안내 화면. 회원 전용 기능 진입 시 이 라우트로 push한다.
export default function LoginRequiredScreen() {
  return <LoginRequiredGuide />;
}
