import { API_BASE } from '@/lib/constants';
import type { Problem } from './types';

export class ApiError extends Error {
  problem: Problem;
  status: number;

  constructor(problem: Problem, status: number) {
    super(problem.detail ?? `API Error ${status}`);
    this.name = 'ApiError';
    this.problem = problem;
    this.status = status;
  }
}

export const PROBLEM_TYPES = {
  PASSWORD_MUST_BE_CHANGED: '/bouncr/problem/PASSWORD_MUST_BE_CHANGED',
  ONE_TIME_PASSWORD_IS_NEEDED: '/bouncr/problem/ONE_TIME_PASSWORD_IS_NEEDED',
  ACCOUNT_IS_LOCKED: '/bouncr/problem/ACCOUNT_IS_LOCKED',
  WEBAUTHN_CHALLENGE_EXPIRED: '/bouncr/problem/WEBAUTHN_CHALLENGE_EXPIRED',
  WEBAUTHN_VERIFICATION_FAILED: '/bouncr/problem/WEBAUTHN_VERIFICATION_FAILED',
  WEBAUTHN_CREDENTIAL_NOT_FOUND: '/bouncr/problem/WEBAUTHN_CREDENTIAL_NOT_FOUND',
} as const;

type QueryParams = Record<string, string | number | boolean | undefined | null>;

function toQueryString(params: QueryParams): string {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value));
    }
  }
  const qs = searchParams.toString();
  return qs ? `?${qs}` : '';
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit & { params?: QueryParams } = {},
): Promise<T> {
  const { params, ...fetchOptions } = options;
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(fetchOptions.headers as Record<string, string> | undefined),
  };

  const url = `${API_BASE}${path}${params ? toQueryString(params) : ''}`;

  const response = await fetch(url, {
    ...fetchOptions,
    credentials: 'same-origin',
    headers,
  });

  if (!response.ok) {
    let problem: Problem;
    try {
      problem = (await response.json()) as Problem;
    } catch {
      problem = { status: response.status, detail: response.statusText };
    }
    throw new ApiError(problem, response.status);
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
