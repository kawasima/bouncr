import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiRequest, ApiError } from './client';

const mockFetch = vi.fn();
globalThis.fetch = mockFetch;

beforeEach(() => {
  mockFetch.mockReset();
});

describe('apiRequest', () => {
  it('constructs correct URL without params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-length': '2' }),
      json: () => Promise.resolve({}),
    });

    await apiRequest('/users');

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url] = mockFetch.mock.calls[0];
    expect(url).toBe('/bouncr/api/users');
  });

  it('constructs correct URL with params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-length': '2' }),
      json: () => Promise.resolve({}),
    });

    await apiRequest('/users', { params: { page: 1, q: 'test' } });

    const [url] = mockFetch.mock.calls[0];
    expect(url).toBe('/bouncr/api/users?page=1&q=test');
  });

  it('omits undefined and null params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-length': '2' }),
      json: () => Promise.resolve({}),
    });

    await apiRequest('/users', { params: { page: 1, q: undefined, sort: null } });

    const [url] = mockFetch.mock.calls[0];
    expect(url).toBe('/bouncr/api/users?page=1');
  });

  it('throws ApiError on non-ok response', async () => {
    const problem = { status: 404, detail: 'Not found' };
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve(problem),
    });

    await expect(apiRequest('/users/unknown')).rejects.toThrow(ApiError);

    try {
      await apiRequest('/users/unknown');
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.status).toBe(404);
      expect(apiErr.problem).toEqual(problem);
    }
  });

  it('returns undefined for 204 responses', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 204,
      headers: new Headers(),
    });

    const result = await apiRequest('/users/1');
    expect(result).toBeUndefined();
  });

  it('returns undefined when content-length is 0', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-length': '0' }),
    });

    const result = await apiRequest('/users/1');
    expect(result).toBeUndefined();
  });

  it('sets correct default headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-length': '2' }),
      json: () => Promise.resolve({}),
    });

    await apiRequest('/users');

    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers).toMatchObject({
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    });
    expect(options.credentials).toBe('same-origin');
  });
});

describe('ApiError', () => {
  it('contains the problem and status', () => {
    const problem = { status: 400, detail: 'Bad request', type: '/bouncr/problem/test' };
    const err = new ApiError(problem, 400);

    expect(err).toBeInstanceOf(Error);
    expect(err.name).toBe('ApiError');
    expect(err.status).toBe(400);
    expect(err.problem).toBe(problem);
    expect(err.message).toBe('Bad request');
  });

  it('uses fallback message when detail is absent', () => {
    const problem = { status: 500 };
    const err = new ApiError(problem, 500);

    expect(err.message).toBe('API Error 500');
  });
});
