import { HttpError } from "../api/httpClient";

type BackendError = {
  code?: string;
  message?: string;
};

const DEFAULT_MESSAGE = "문제가 발생했습니다. 잠시 후 다시 시도해주세요.";

/**
 * 백엔드 에러 응답({ code, message })에서 사용자에게 보여줄 메시지를 뽑아낸다.
 * HttpError가 아니면(네트워크 오류 등) 기본 메시지를 반환한다.
 */
export function getApiErrorMessage(error: unknown, fallback = DEFAULT_MESSAGE): string {
  if (error instanceof HttpError) {
    const data = error.data as BackendError | null;
    if (data && typeof data.message === "string" && data.message.length > 0) {
      return data.message;
    }
  }
  return fallback;
}

/**
 * 백엔드 에러 응답의 코드(예: "EMAIL_ALREADY_EXISTS")를 반환한다. 없으면 null.
 */
export function getApiErrorCode(error: unknown): string | null {
  if (error instanceof HttpError) {
    const data = error.data as BackendError | null;
    if (data && typeof data.code === "string") {
      return data.code;
    }
  }
  return null;
}
