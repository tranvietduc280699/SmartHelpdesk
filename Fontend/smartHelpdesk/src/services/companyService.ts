import { ApiError, http } from './http'
/** Public company directory used before registration: GET /api/companies. */
export const DEVELOPER_COMPANY_ID = 'KR_BZCOM'
// Business eligibility only; company labels and available rows still come from the API.
const CLIENT_COMPANY_IDS: readonly string[] = [
  'KR_SAMSUNG',
  'KR_NAVER',
  'KR_KAKAO',
]
export function isClientCompany(companyId: string): boolean {
  return CLIENT_COMPANY_IDS.includes(companyId)
}
export interface Company {
  companyId: string
  companyName: string
}
export const companyService = {
  async list(): Promise<Company[]> {
    const data = await http.get<Company[]>('/companies', { auth: false })
    if (
      !Array.isArray(data) ||
      data.some(
        (company) =>
          !company ||
          typeof company.companyId !== 'string' ||
          !company.companyId ||
          typeof company.companyName !== 'string' ||
          !company.companyName,
      )
    ) {
      throw new ApiError('Dữ liệu danh sách công ty không hợp lệ.', 502)
    }
    return data
  },
}
