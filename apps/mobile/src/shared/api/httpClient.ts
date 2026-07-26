type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

type RequestOptions = {
  headers?: Record<string, string>;
  accessToken?: string;
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
};

let authHandlers: AuthHandlers | null = null;

export function setAuthHandlers(handlers: AuthHandlers | null): void {
  authHandlers = handlers;
}

// 401이 동시에 여러 건 발생해도 재발급 요청은 한 번만 나가도록 공유한다.
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

        const response = await fetch(`${API_BASE_URL ?? ''}/api/auth/reissue`, {
          method: 'POST',
          headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        const data = await parseJson(response);

        if (!response.ok) {
          await handlers.onRefreshFailed();
          return null;
        }

        const tokens = data as { accessToken: string; refreshToken: string };
        await handlers.onTokensRefreshed(tokens.accessToken, tokens.refreshToken);
        return tokens.accessToken;
      } catch {
        await handlers.onRefreshFailed();
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

  const response = await fetch(url, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  const data = await parseJson(response);

  if (response.status === 401 && options.accessToken && !isRetry) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      return request<TResponse>(method, path, { ...options, accessToken: newAccessToken }, true);
    }
  }

  if (!response.ok) {
    throw new HttpError(response.status, data);
  }

  return data as TResponse;
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
  delete<TResponse>(path: string, options?: RequestOptions) {
    return request<TResponse>('DELETE', path, options);
  },
};
