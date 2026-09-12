import { http } from './http'
import { toMember, type MemberResponse } from './apiAdapters'
import type { UserRole } from '@/types/user'
export const memberService = {
  async list(role?: UserRole) {
    const members = (await http.get<MemberResponse[]>('/members')).map(toMember)
    return members.filter(
      (member) => !member.isDeleted && (!role || member.role === role),
    )
  },
  async get(id: string) {
    return toMember(
      await http.get<MemberResponse>('/members/' + encodeURIComponent(id)),
    )
  },
}
