import { DEVELOPER_COMPANY_ID, isClientCompany } from './companyService'
import { ApiError, http } from './http'
import { toMember, type MemberResponse } from './apiAdapters'
import type { AuthUser, Member, SelectableRole } from '@/types/user'
export interface LoginPayload {
  email: string
  password: string
}
export interface RegisterPayload {
  email: string
  password: string
  name: string
  phone: string | null
  role: SelectableRole
  companyId: string | null
}
export interface LoginResult {
  accessToken: string
  refreshToken: string
  user: AuthUser
}
export interface RefreshResult {
  accessToken: string
  refreshToken: string
  accessTokenTtl: number
  refreshTokenTtl: number
}
export interface AuthService {
  login(payload: LoginPayload): Promise<LoginResult>
  register(payload: RegisterPayload): Promise<Member>
  refresh(refreshToken: string): Promise<RefreshResult>
  logout(): Promise<void>
}
export const authService: AuthService = {
  async login(payload) {
    const result = await http.post<
      Omit<LoginResult, 'user'> & { user: MemberResponse }
    >('/auth/login', payload, { auth: false })
    return { ...result, user: toMember(result.user) }
  },
  async register(payload) {
    const role = payload.role === 'dev' ? 'DEVELOPER' : 'CLIENT'
    const companyId =
      payload.role === 'dev' ? DEVELOPER_COMPANY_ID : payload.companyId
    if (
      payload.role === 'client' &&
      (!companyId || !isClientCompany(companyId))
    ) {
      throw new ApiError('Vui lòng chọn công ty hợp lệ.', 400, 'VAL_400', {
        companyId: 'Vui lòng chọn công ty hợp lệ.',
      })
    }
    return toMember(
      await http.post<MemberResponse>(
        '/auth/register',
        { ...payload, role, companyId },
        { auth: false },
      ),
    )
  },
  refresh: (refreshToken) =>
    http.post<RefreshResult>(
      '/auth/refresh',
      { refreshToken },
      { auth: false },
    ),
  logout: () => http.post<void>('/auth/logout'),
}
