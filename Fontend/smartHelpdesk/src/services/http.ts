import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  getStoredUser,
  updateTokens,
} from './tokenStorage'
import { viMessage } from './messages'
const BASE_URL =
  (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '') || '/api'
interface Envelope<T> {
  status: number
  message: string
  errorCode?: string
  data?: T
}
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string | null = null,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }
  get isNetworkError() {
    return this.status === 0
  }
  get isUnauthorized() {
    return this.status === 401
  }
  get isAuthFailure() {
    return this.status === 401
  }
}
interface RequestOptions {
  auth?: boolean
  signal?: AbortSignal
}
function isEnvelope(value: unknown): value is Envelope<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    'message' in value
  )
}
function apiError(body: unknown, status: number): ApiError {
  const fields: Record<string, string> = {}
  if (
    isEnvelope(body) &&
    body.data &&
    typeof body.data === 'object' &&
    !Array.isArray(body.data)
  ) {
    for (const [key, value] of Object.entries(body.data))
      if (typeof value === 'string') fields[key] = viMessage(value)
  }
  const fallback =
    status === 401
      ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
      : status === 403
        ? 'Bạn không có quyền thực hiện thao tác này.'
        : 'Máy chủ trả về lỗi ' + status + '. Vui lòng thử lại.'
  return new ApiError(
    isEnvelope(body) && body.message ? viMessage(body.message) : fallback,
    status,
    isEnvelope(body) ? (body.errorCode ?? null) : null,
    fields,
  )
}
function expireSession(): void {
  clearSession()
  window.dispatchEvent(new Event('auth:expired'))
}
// A rotating refresh token can only be used once: share its pending refresh.
let refreshing: { token: string; promise: Promise<boolean> } | null = null
function refreshSession(token: string): Promise<boolean> {
  if (refreshing?.token === token) return refreshing.promise
  const promise = (async () => {
    try {
      const tokens = await request<{
        accessToken: string
        refreshToken: string
      }>('POST', '/auth/refresh', { refreshToken: token }, { auth: false })
      if (getRefreshToken() !== token) return false
      if (!tokens?.accessToken || !tokens.refreshToken)
        throw new Error('Invalid token response')
      updateTokens(tokens.accessToken, tokens.refreshToken)
      window.dispatchEvent(new Event('auth:refreshed'))
      return true
    } catch {
      if (getRefreshToken() === token) expireSession()
      return false
    } finally {
      if (refreshing?.token === token) refreshing = null
    }
  })()
  refreshing = { token, promise }
  return promise
}
async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  options: RequestOptions = {},
  allowRetry = true,
): Promise<T> {
  const { auth = true, signal } = options
  const sentToken = auth ? getAccessToken() : null
  const sentUser = auth ? getStoredUser()?.id : undefined
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (sentToken) headers.Authorization = 'Bearer ' + sentToken
  const controller = new AbortController()
  const abort = () => controller.abort()
  if (signal?.aborted) controller.abort()
  signal?.addEventListener('abort', abort, { once: true })
  const timeout = setTimeout(abort, 30000)
  let response: Response
  let raw: string
  try {
    response = await fetch(BASE_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    })
    raw = await response.text()
  } catch (cause) {
    if (signal?.aborted) throw cause
    throw new ApiError(
      'Không kết nối được máy chủ hoặc yêu cầu quá thời gian chờ. Vui lòng thử lại.',
      0,
    )
  } finally {
    clearTimeout(timeout)
    signal?.removeEventListener('abort', abort)
  }
  let parsed: unknown
  try {
    parsed = raw ? JSON.parse(raw) : undefined
  } catch {
    if (response.ok)
      throw new ApiError('Máy chủ trả về dữ liệu không hợp lệ.', 502)
  }
  const status =
    response.ok && isEnvelope(parsed) && parsed.status >= 400
      ? parsed.status
      : response.status
  if (!response.ok || status >= 400) {
    if (auth && status === 401 && sentUser === getStoredUser()?.id) {
      if (allowRetry) {
        if (getAccessToken() && getAccessToken() !== sentToken)
          return request<T>(method, path, body, options, false)
        const refreshToken = getRefreshToken()
        if (refreshToken && (await refreshSession(refreshToken)))
          return request<T>(method, path, body, options, false)
      }
      if (getAccessToken() === sentToken) expireSession()
    }
    throw apiError(parsed, status)
  }
  if (auth && sentUser !== getStoredUser()?.id)
    throw new ApiError('Phiên đăng nhập đã thay đổi.', 401)
  return (isEnvelope(parsed) ? parsed.data : parsed) as T
}
export const http = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, body, options),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PATCH', path, body, options),
}
