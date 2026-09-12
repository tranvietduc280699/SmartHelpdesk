import { afterEach, expect, it, vi } from 'vitest'
import {
  companyService,
  DEVELOPER_COMPANY_ID,
  isClientCompany,
} from './companyService'
afterEach(() => vi.unstubAllGlobals())
it('loads the public directory from the API without a bearer token', async () => {
  const data = [{ companyId: 'from-api', companyName: 'API company' }]
  const fetchMock = vi
    .fn()
    .mockResolvedValue(
      new Response(JSON.stringify({ status: 200, message: 'Success', data })),
    )
  vi.stubGlobal('fetch', fetchMock)
  expect(await companyService.list()).toEqual(data)
  expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/companies')
  expect(
    new Headers(fetchMock.mock.calls[0]?.[1]?.headers).has('Authorization'),
  ).toBe(false)
})
it('keeps an empty directory empty rather than introducing fallback data', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValue(
        new Response(
          JSON.stringify({ status: 200, message: 'Success', data: [] }),
        ),
      ),
  )
  expect(await companyService.list()).toEqual([])
})
it('rejects incompatible company responses', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 200,
          message: 'Success',
          data: [{ id: 'wrong-shape' }],
        }),
      ),
    ),
  )
  await expect(companyService.list()).rejects.toMatchObject({ status: 502 })
})

it('restricts client eligibility to the three partner IDs and uses the new developer ID', () => {
  expect(DEVELOPER_COMPANY_ID).toBe('KR_BZCOM')
  for (const id of ['KR_SAMSUNG', 'KR_NAVER', 'KR_KAKAO'])
    expect(isClientCompany(id)).toBe(true)
  for (const id of [
    'KR_BZCOM',
    'BZCOM',
    'CHAT_TEST_COMPANY',
    'KR_CLIENT_Ss',
    '',
  ])
    expect(isClientCompany(id)).toBe(false)
})
