import type { Member, UserRole } from '@/types/user'
import type {
  SupportRequest,
  RequestHistory,
  RequestStats,
  RequestCategory,
} from '@/types/request'
export type ApiRole = 'ADMIN' | 'DEVELOPER' | 'CLIENT'
export interface MemberResponse extends Omit<Member, 'role'> {
  role: ApiRole
}
export function normalizeRole(role: ApiRole): UserRole {
  const roles: Record<ApiRole, UserRole> = {
    ADMIN: 'admin',
    DEVELOPER: 'dev',
    CLIENT: 'client',
  }
  if (!roles[role]) throw new Error('Invalid member role')
  return roles[role]
}
export const toMember = (member: MemberResponse): Member => ({
  ...member,
  role: normalizeRole(member.role),
  companyId: member.companyId ?? null,
  companyName: member.companyName ?? null,
  phone: member.phone ?? null,
  createdAt: member.createdAt ?? null,
})
export interface RequestResponse extends Omit<
  SupportRequest,
  'client' | 'assignedDeveloper' | 'description'
> {
  description: string | null
  clientId: string
  clientName: string | null
  clientEmail?: string | null
  assignedDeveloperId: string | null
  assignedDeveloperName: string | null
  assignedDeveloperEmail?: string | null
}
export function toRequest(row: RequestResponse): SupportRequest {
  return {
    ...row,
    description: row.description ?? '',
    client: {
      id: row.clientId,
      name: row.clientName ?? row.clientId,
      email: row.clientEmail ?? '',
      role: 'client',
      companyId: row.companyId ?? null,
    },
    assignedDeveloper: row.assignedDeveloperId
      ? {
          id: row.assignedDeveloperId,
          name: row.assignedDeveloperName ?? row.assignedDeveloperId,
          email: row.assignedDeveloperEmail ?? '',
          role: 'dev',
          companyId: null,
        }
      : null,
  }
}
export interface HistoryResponse extends Omit<RequestHistory, 'changedBy'> {
  changedBy: string
  changedByName: string | null
}
export const toHistory = (row: HistoryResponse): RequestHistory => ({
  ...row,
  changedBy: { id: row.changedBy, name: row.changedByName ?? row.changedBy },
})
export interface StatsResponse {
  totalRequests: number
  completedRequests: number
  completionRate: number
  requestsByCategory: Record<RequestCategory, number>
  requestsByDeveloper: {
    developerId: string
    developerName: string
    completedCount: number
    inProgressCount: number
    pendingCount: number
    totalAssigned: number
  }[]
}
export function toStats(row: StatsResponse): RequestStats {
  return {
    total: row.totalRequests,
    completed: row.completedRequests,
    completionRate: row.completionRate / 100,
    byCategory: row.requestsByCategory,
    byDeveloper: row.requestsByDeveloper.map((dev) => ({
      developer: { id: dev.developerId, name: dev.developerName },
      assigned: dev.totalAssigned,
      done: dev.completedCount,
      pending: dev.pendingCount,
      inProgress: dev.inProgressCount,
    })),
  }
}
