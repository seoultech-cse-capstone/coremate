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

async function request<TResponse>(
  method: HttpMethod,
  path: string,
  options: BodyRequestOptions = {},
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
