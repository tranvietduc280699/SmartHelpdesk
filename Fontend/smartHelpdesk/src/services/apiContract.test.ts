import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authService } from './authService'
import { requestService } from './requestService'
import { memberService } from './memberService'
import { http } from './http'
import { saveSession, getAccessToken, clearSession } from './tokenStorage'
const fetchMock = vi.fn<typeof fetch>()
const user = {
  id: 'user-test',
  name: 'Test user',
  email: 'test@example.invalid',
  role: 'client' as const,
  companyId: null,
}
const row = {
  id: 'request-test',
  title: 'Issue',
  description: null,
  category: 'BUG',
  priority: 'MEDIUM',
  status: 'PENDING',
  clientId: user.id,
  clientName: user.name,
  assignedDeveloperId: null,
  assignedDeveloperName: null,
  createdAt: '2026-09-11T00:00:00Z',
  updatedAt: '2026-09-11T00:00:00Z',
}
const response = (data: unknown, status = 200) =>
  new Response(
    JSON.stringify({
      status: status === 201 ? 200 : status,
      message: 'Result',
      data,
    }),
    { status },
  )
beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  saveSession({ user, accessToken: 'old-access', refreshToken: 'old-refresh' })
})
afterEach(() => vi.unstubAllGlobals())
describe('backend API contract', () => {
  it('preserves the selected partner ID in client registration', async () => {
    fetchMock.mockResolvedValue(
      response({ ...user, role: 'CLIENT', companyId: 'KR_NAVER' }, 201),
    )
    await authService.register({
      email: user.email,
      name: user.name,
      password: 'test-only',
      role: 'client',
      companyId: 'KR_NAVER',
      phone: null,
    })
    expect(
      JSON.parse(fetchMock.mock.calls[0]?.[1]?.body as string),
    ).toMatchObject({ role: 'CLIENT', companyId: 'KR_NAVER' })
  })
  it('rejects the test company without sending a registration request', async () => {
    await expect(
      authService.register({
        email: user.email,
        name: user.name,
        password: 'test-only',
        role: 'client',
        companyId: 'CHAT_TEST_COMPANY',
        phone: null,
      }),
    ).rejects.toMatchObject({ status: 400 })
    expect(fetchMock).not.toHaveBeenCalled()
  })
  it('maps DEVELOPER login role and sends no stale bearer token to public auth', async () => {
    fetchMock.mockResolvedValue(
      response({
        accessToken: 'a',
        refreshToken: 'r',
        user: { ...user, role: 'DEVELOPER' },
      }),
    )
    expect(
      (await authService.login({ email: user.email, password: 'test-only' }))
        .user.role,
    ).toBe('dev')
    expect(fetchMock.mock.calls[0]?.[1]?.headers).not.toHaveProperty(
      'Authorization',
    )
  })
  it('registers the exact DTO and accepts HTTP 201 with envelope 200', async () => {
    fetchMock.mockResolvedValue(response({ ...user, role: 'DEVELOPER' }, 201))
    await authService.register({
      email: user.email,
      name: user.name,
      password: 'test-only',
      role: 'dev',
      companyId: null,
      phone: null,
    })
    expect(JSON.parse(fetchMock.mock.calls[0]?.[1]?.body as string)).toEqual({
      email: user.email,
      name: user.name,
      password: 'test-only',
      role: 'DEVELOPER',
      companyId: 'KR_BZCOM',
      phone: null,
    })
  })
  it('maps flat request DTOs and Spring pagination, uses backend query names', async () => {
    fetchMock.mockResolvedValue(
      response({
        content: [row],
        number: 2,
        size: 10,
        totalElements: 21,
        totalPages: 3,
      }),
    )
    const page = await requestService.list({
      page: 2,
      size: 10,
      q: ' error ',
      sort: 'updatedAt,asc',
      status: 'PENDING',
      priority: '',
    })
    const url = new URL(String(fetchMock.mock.calls[0]?.[0]), 'http://test')
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: '2',
      size: '10',
      search: 'error',
      sortBy: 'updatedAt',
      sortDir: 'asc',
      status: 'PENDING',
    })
    expect(page.page).toBe(2)
    expect(page.content[0]?.client.name).toBe(user.name)
    expect(page.content[0]?.assignedDeveloper).toBeNull()
    expect(page.content[0]?.description).toBe('')
  })
  it('maps history author and KPI percentage without fabricating status counts', async () => {
    fetchMock
      .mockResolvedValueOnce(
        response([
          {
            id: 'h',
            changedBy: user.id,
            changedByName: user.name,
            action: 'CREATE',
          },
        ]),
      )
      .mockResolvedValueOnce(
        response({
          totalRequests: 12,
          completedRequests: 8,
          completionRate: 66.67,
          requestsByCategory: { BUG: 6, FEATURE: 4, INQUIRY: 2 },
          requestsByDeveloper: [],
        }),
      )
    expect((await requestService.history(row.id))[0]?.changedBy.name).toBe(
      user.name,
    )
    const stats = await requestService.stats()
    expect(stats.total).toBe(12)
    expect(stats.completed).toBe(8)
    expect(stats.completionRate).toBeCloseTo(0.6667)
    expect(stats.byStatus).toBeUndefined()
  })
  it('maps member roles before filtering developers and excludes deleted members', async () => {
    fetchMock.mockResolvedValue(
      response([
        { ...user, role: 'DEVELOPER', isDeleted: false },
        { ...user, id: 'deleted', role: 'DEVELOPER', isDeleted: true },
        { ...user, role: 'ADMIN' },
      ]),
    )
    expect(await memberService.list('dev')).toHaveLength(1)
  })
  it('sends auto assignment and forward status with memo to PATCH routes', async () => {
    fetchMock.mockImplementation(async () => response(row))
    await requestService.assign(row.id)
    await requestService.updateStatus(row.id, 'IN_PROGRESS', ' Investigating ')
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      '/api/requests/request-test/assign',
    )
    expect(fetchMock.mock.calls[0]?.[1]?.body).toBe('{}')
    expect(JSON.parse(fetchMock.mock.calls[1]?.[1]?.body as string)).toEqual({
      status: 'IN_PROGRESS',
      memo: 'Investigating',
    })
  })
  it('calls both AI endpoints and the summary route', async () => {
    fetchMock
      .mockResolvedValueOnce(
        response({ category: 'BUG', reason: 'Bug', confidence: 0.9 }),
      )
      .mockResolvedValueOnce(
        response({ priority: 'HIGH', reason: 'Urgent', confidence: 0.8 }),
      )
      .mockResolvedValueOnce(
        response({ requestId: row.id, summary: 'Summary' }),
      )
    expect(await requestService.suggest('Issue')).toMatchObject({
      category: 'BUG',
      priority: 'HIGH',
    })
    expect((await requestService.summary(row.id)).summary).toBe('Summary')
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/requests/classify',
      '/api/requests/suggest-priority',
      '/api/requests/request-test/summary',
    ])
  })
  it('reports field errors without losing backend keys', async () => {
    fetchMock.mockResolvedValue(response({ companyId: 'Invalid company' }, 400))
    await expect(
      http.post('/auth/register', {}, { auth: false }),
    ).rejects.toMatchObject({
      status: 400,
      fieldErrors: { companyId: 'Invalid company' },
    })
  })
  it('does not refresh or log out for forbidden access', async () => {
    fetchMock.mockResolvedValue(response(null, 403))
    await expect(requestService.stats()).rejects.toMatchObject({ status: 403 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBe('old-access')
  })
  it('shares token refresh between concurrent unauthorized requests', async () => {
    let refreshCount = 0
    fetchMock.mockImplementation(async (path, init) => {
      if (path === '/api/auth/refresh') {
        refreshCount++
        await new Promise((resolve) => setTimeout(resolve, 5))
        return response({
          accessToken: 'new-access',
          refreshToken: 'new-refresh',
        })
      }
      return new Headers(init?.headers).get('Authorization') ===
        'Bearer old-access'
        ? response(null, 401)
        : response([])
    })
    await Promise.all([http.get('/alerts'), http.get('/requests')])
    expect(refreshCount).toBe(1)
    expect(getAccessToken()).toBe('new-access')
    expect(fetchMock).toHaveBeenCalledTimes(5)
  })
  it('clears an expired session and emits the login redirect signal after refresh fails', async () => {
    const expired = vi.fn()
    window.addEventListener('auth:expired', expired, { once: true })
    fetchMock.mockImplementation(async () => response(null, 401))
    await expect(http.get('/alerts')).rejects.toMatchObject({ status: 401 })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(getAccessToken()).toBeNull()
    expect(expired).toHaveBeenCalledOnce()
  })
  it('does not restore a session when refresh finishes after logout', async () => {
    let finish: (value: Response) => void = () => {}
    fetchMock.mockImplementation(async (path) =>
      path === '/api/auth/refresh'
        ? new Promise((resolve) => {
            finish = resolve
          })
        : response(null, 401),
    )
    const pending = http.get('/alerts').catch((error) => error)
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    clearSession()
    finish(response({ accessToken: 'new', refreshToken: 'new' }))
    await pending
    expect(getAccessToken()).toBeNull()
  })
  it('accepts logout with an empty response and normalizes connection failures', async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockRejectedValueOnce(new TypeError('Offline'))
    await expect(authService.logout()).resolves.toBeUndefined()
    await expect(http.get('/requests')).rejects.toMatchObject({ status: 0 })
  })
})
