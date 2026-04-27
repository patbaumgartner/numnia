/**
 * Unit tests for the typed API client (UC-001, FR-001…FR-004).
 *
 * Mocks the global `fetch` to verify correct HTTP method, path, headers, and
 * body are constructed for each endpoint.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  registerParent,
  verifyEmail,
  createChildProfile,
  confirmChildProfile,
  ApiError,
} from './client';

const mockFetch = vi.fn();

beforeEach(() => {
  vi.stubGlobal('fetch', mockFetch);
});

afterEach(() => {
  vi.unstubAllGlobals();
  mockFetch.mockReset();
});

function okResponse(body: unknown, status = 200) {
  return Promise.resolve({
    ok: true,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

function errorResponse(status: number, body: unknown) {
  return Promise.resolve({
    ok: false,
    status,
    statusText: 'Error',
    json: () => Promise.resolve(body),
  } as Response);
}

describe('API client', () => {
  describe('registerParent', () => {
    it('POSTs to /api/parents and returns parentId', async () => {
      mockFetch.mockReturnValueOnce(okResponse({ parentId: 'uuid-1' }, 201));

      const result = await registerParent({
        firstName: 'Anna',
        salutation: 'Frau',
        email: 'anna@example.com',
        password: 'pass1234',
        privacyConsented: true,
        termsAccepted: true,
      });

      expect(mockFetch).toHaveBeenCalledWith('/api/parents', expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }));
      expect(result).toEqual({ parentId: 'uuid-1' });
    });

    it('throws ApiError with status 409 on duplicate email', async () => {
      mockFetch.mockReturnValueOnce(
        errorResponse(409, { error: 'DUPLICATE_EMAIL', message: 'Duplicate' }),
      );

      await expect(
        registerParent({
          firstName: 'Anna',
          salutation: 'Frau',
          email: 'anna@example.com',
          password: 'pass1234',
          privacyConsented: true,
          termsAccepted: true,
        }),
      ).rejects.toMatchObject({ status: 409, code: 'DUPLICATE_EMAIL' });
    });
  });

  describe('verifyEmail', () => {
    it('POSTs to /api/parents/verify and returns parentId + status', async () => {
      mockFetch.mockReturnValueOnce(
        okResponse({ parentId: 'uuid-2', status: 'ACTIVE' }),
      );

      const result = await verifyEmail({ token: 'tok-abc' });

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/parents/verify',
        expect.objectContaining({ method: 'POST' }),
      );
      expect(result.parentId).toBe('uuid-2');
      expect(result.status).toBe('ACTIVE');
    });

    it('throws ApiError with status 410 on expired token', async () => {
      mockFetch.mockReturnValueOnce(
        errorResponse(410, { error: 'TOKEN_EXPIRED', message: 'Expired' }),
      );

      await expect(verifyEmail({ token: 'expired' })).rejects.toMatchObject({
        status: 410,
        code: 'TOKEN_EXPIRED',
      });
    });
  });

  describe('createChildProfile', () => {
    it('POSTs to /api/parents/{id}/child-profiles', async () => {
      mockFetch.mockReturnValueOnce(
        okResponse({ childProfileId: 'cid-1', pseudonym: 'Luna' }, 201),
      );

      const result = await createChildProfile('parent-1', {
        pseudonym: 'Luna',
        yearOfBirth: 2015,
        avatarBaseModel: 'star',
      });

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/parents/parent-1/child-profiles',
        expect.objectContaining({ method: 'POST' }),
      );
      expect(result.pseudonym).toBe('Luna');
    });

    it('throws ApiError with status 422 on invalid profile', async () => {
      mockFetch.mockReturnValueOnce(
        errorResponse(422, { error: 'INVALID_CHILD_PROFILE', message: 'Invalid age' }),
      );

      await expect(
        createChildProfile('parent-1', {
          pseudonym: 'Luna',
          yearOfBirth: 2000,
          avatarBaseModel: 'star',
        }),
      ).rejects.toMatchObject({ status: 422 });
    });
  });

  describe('confirmChildProfile', () => {
    it('POSTs to /api/parents/{pid}/child-profiles/{cid}/confirm', async () => {
      mockFetch.mockReturnValueOnce(
        okResponse(null, 204),
      );

      await confirmChildProfile('parent-1', 'child-1', { token: 'tok-xyz' });

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/parents/parent-1/child-profiles/child-1/confirm',
        expect.objectContaining({ method: 'POST' }),
      );
    });

    it('throws ApiError on expired token', async () => {
      mockFetch.mockReturnValueOnce(
        errorResponse(410, { error: 'TOKEN_EXPIRED', message: 'Gone' }),
      );

      await expect(
        confirmChildProfile('parent-1', 'child-1', { token: 'bad' }),
      ).rejects.toMatchObject({ status: 410 });
    });
  });

  describe('ApiError', () => {
    it('stores status, code and message', () => {
      const err = new ApiError(404, 'NOT_FOUND', 'Resource not found');
      expect(err.status).toBe(404);
      expect(err.code).toBe('NOT_FOUND');
      expect(err.message).toBe('Resource not found');
      expect(err.name).toBe('ApiError');
    });
  });

  describe('fetch error fallback', () => {
    it('uses unknown error code when JSON parse fails', async () => {
      mockFetch.mockReturnValueOnce(
        Promise.resolve({
          ok: false,
          status: 500,
          statusText: 'Internal Server Error',
          json: () => Promise.reject(new Error('parse error')),
        } as Response),
      );

      await expect(registerParent({
        firstName: 'A',
        salutation: 'Herr',
        email: 'a@b.com',
        password: 'password1',
        privacyConsented: true,
        termsAccepted: true,
      })).rejects.toMatchObject({ code: 'UNKNOWN' });
    });
  });
});
