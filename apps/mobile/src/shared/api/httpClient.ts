type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type RequestOptions = {
  headers?: Record<string, string>;
  accessToken?: string;
  /**
   * 응답을 기다릴 최대 시간(ms). 주지 않으면 플랫폼 기본값을 그대로 쓴다(iOS는 60초).
   * 서버가 60초보다 오래 걸릴 수 있는 요청에만 붙인다 — 앱이 먼저 끊으면 서버는 계속
   * 처리하는데(코인 차감 같은 부수효과까지 끝난다) 화면에는 실패로만 보인다.
   */
  timeoutMs?: number;
};

type BodyRequestOptions = RequestOptions & {
  body?: unknown;
};

export class HttpError extends Error {
  status: number;
  data: unknown;

  constructor(status: number, data: unknown) {
    super(`HTTP request failed with status ${status}`);
    this.name = 'HttpError';
    this.status = status;
    this.data = data;
  }
}

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL;

/**
 * 액세스 토큰 만료(401) 시 자동 재발급을 위해 auth 기능이 등록하는 훅.
 * httpClient는 features/auth를 직접 import하지 않고(순환 참조 방지),
 * AuthProvider가 앱 시작 시 setAuthHandlers로 실제 구현을 주입한다.
 */
type AuthHandlers = {
  getRefreshToken: () => Promise<string | null>;
  onTokensRefreshed: (accessToken: string, refreshToken: string) => Promise<void> | void;
  onRefreshFailed: () => Promise<void> | void;
  /** 기기 세션이 해지되어 끊긴 경우. 사유를 받아 안내 모달을 띄운다. */
  onSessionRevoked: (code: SessionRevokedCode) => Promise<void> | void;
};

/** 사용자에게 이유를 알려야 하는 해지 사유. 서버 ErrorCode와 문자열이 일치한다. */
export type SessionRevokedCode =
  | "SESSION_REVOKED_BY_OTHER_DEVICE"
  | "SESSION_REVOKED_BY_USER";

const SESSION_REVOKED_CODES: SessionRevokedCode[] = [
  "SESSION_REVOKED_BY_OTHER_DEVICE",
  "SESSION_REVOKED_BY_USER",
];

/** 세션은 끝났지만 사용자가 이미 이유를 아는 경우(본인 로그아웃·비밀번호 재설정·탈퇴). */
const SESSION_EXPIRED_CODE = "SESSION_EXPIRED";

function readErrorCode(data: unknown): string | null {
  if (data && typeof data === "object" && "code" in data) {
    const code = (data as { code?: unknown }).code;
    return typeof code === "string" ? code : null;
  }
  return null;
}

function toRevokedCode(data: unknown): SessionRevokedCode | null {
  const code = readErrorCode(data);
  return SESSION_REVOKED_CODES.find((candidate) => candidate === code) ?? null;
}

let authHandlers: AuthHandlers | null = null;

export function setAuthHandlers(handlers: AuthHandlers | null): void {
  authHandlers = handlers;
}

// 겹치는 401 사이에는 재발급 요청을 이 Promise로 공유한다. 단, 이 Promise가
// 끝나 null로 초기화된 뒤에 뒤늦게 도착한 401은 재발급을 다시 호출한다.
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!authHandlers) {
    return null;
  }
  // authHandlers는 진행 중에 null로 바뀔 수 있으므로(AuthProvider 언마운트 등),
  // 이번 재발급에 쓸 참조를 로컬로 고정해 이후 await 도중 사라지지 않게 한다.
  const handlers = authHandlers;

  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const refreshToken = await handlers.getRefreshToken();
        if (!refreshToken) {
          return null;
        }

        const tokens = await request<{ accessToken: string; refreshToken: string }>(
          'POST',
          '/api/auth/reissue',
          { body: { refreshToken } },
        );
        await handlers.onTokensRefreshed(tokens.accessToken, tokens.refreshToken);
        return tokens.accessToken;
      } catch (error) {
        // 재발급 요청 자체가 해지 사유를 들고 실패할 수도 있다(만료된 access로 먼저
        // 재발급을 시도한 경우). 위 분기와 이 경로 둘 다 막아야 사유가 새지 않는다.
        const revokedCode = error instanceof HttpError ? toRevokedCode(error.data) : null;
        if (revokedCode) {
          await handlers.onSessionRevoked(revokedCode);
        } else {
          await handlers.onRefreshFailed();
        }
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }

  return refreshPromise;
}

async function request<TResponse>(
  method: HttpMethod,
  path: string,
  options: BodyRequestOptions = {},
  isRetry = false,
): Promise<TResponse> {
  const url = `${API_BASE_URL ?? ''}${path}`;
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...options.headers,
  };

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }

  // 타임아웃을 지정한 요청에만 중단 신호를 단다. 타이머는 응답 본문까지 다 읽은 뒤에
  // 푼다 — 그 전에 풀면 본문을 읽는 동안은 무제한으로 기다리게 된다.
  const controller = options.timeoutMs === undefined ? null : new AbortController();
  const timeoutId = controller
    ? setTimeout(() => controller.abort(), options.timeoutMs)
    : null;

  try {
    const response = await fetch(url, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller?.signal,
    });

    const data = await parseJson(response);

    if (response.status === 401 && options.accessToken && !isRetry) {
      // 재발급을 시도하기 "전에" 사유를 본다. 해지된 세션으로 재발급을 부르면 어차피
      // 같은 이유로 거절당하는 요청을 한 번 더 보내는 셈이고, 사용자에게 띄울 사유도
      // 잃어버린다.
      const revokedCode = toRevokedCode(data);
      if (revokedCode) {
        await authHandlers?.onSessionRevoked(revokedCode);
        throw new HttpError(response.status, data);
      }

      // 세션이 끝난 건 맞지만 사용자가 이유를 아는 경우. 조용히 로그아웃만 한다.
      if (readErrorCode(data) === SESSION_EXPIRED_CODE) {
        await authHandlers?.onRefreshFailed();
        throw new HttpError(response.status, data);
      }

      // code가 없는 401 = 단순 만료. 지금까지와 동일하게 조용히 재발급한다.
      // 재시도는 자기 타이머를 새로 단다. 여기서 반환하면 아래 finally가 바로 돌아
      // 이번 요청의 타이머는 풀린다.
      const newAccessToken = await refreshAccessToken();
      if (newAccessToken) {
        return request<TResponse>(method, path, { ...options, accessToken: newAccessToken }, true);
      }
    }

    if (!response.ok) {
      throw new HttpError(response.status, data);
    }

    return data as TResponse;
  } finally {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }
  }
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export const httpClient = {
  get<TResponse>(path: string, options?: RequestOptions) {
    return request<TResponse>('GET', path, options);
  },
  post<TResponse>(path: string, body?: unknown, options?: RequestOptions) {
    return request<TResponse>('POST', path, { ...options, body });
  },
  put<TResponse>(path: string, body?: unknown, options?: RequestOptions) {
    return request<TResponse>('PUT', path, { ...options, body });
  },
  patch<TResponse>(path: string, body?: unknown, options?: RequestOptions) {
    return request<TResponse>('PATCH', path, { ...options, body });
  },
  delete<TResponse>(path: string, options?: RequestOptions) {
    return request<TResponse>('DELETE', path, options);
  },
};
