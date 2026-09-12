/** UI roles; API roles are mapped at the service boundary. */
export type UserRole = 'admin' | 'dev' | 'client'
export type SelectableRole = Exclude<UserRole, 'admin'>
export interface AuthUser {
  id: string
  email: string
  name: string
  role: UserRole
  companyId: string | null
  companyName?: string | null
  phone?: string | null
  status?: string
}
export interface Member extends AuthUser {
  phone: string | null
  status: string
  createdAt: string | null
  isDeleted?: boolean | null
}
export const ROLE_LABEL: Record<UserRole, string> = {
  admin: 'Admin',
  dev: 'Developer',
  client: 'Client',
}
