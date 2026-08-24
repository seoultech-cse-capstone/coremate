/**
 * 비밀번호 형식 규칙(FR-AUTH-01). 회원가입·비밀번호 변경·비밀번호 재설정 화면이 같은 값을 쓴다.
 *
 * 백엔드 `global/validation/PasswordPolicy` 와 같은 규칙이어야 한다.
 * 한쪽만 바뀌면 앱은 통과시키는데 서버가 거부하는(또는 그 반대) 상태가 된다.
 */
export const PASSWORD_RULE =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

export const PASSWORD_RULE_MESSAGE =
  "비밀번호는 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 포함해 8자 이상이어야 합니다.";
