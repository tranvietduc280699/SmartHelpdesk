import { http } from './http'
import {
  toRequest,
  toHistory,
  toStats,
  type RequestResponse,
  type HistoryResponse,
  type StatsResponse,
} from './apiAdapters'
import type {
  Alert,
  Page,
  RequestCategory,
  RequestPriority,
  RequestStatus,
  SupportRequest,
} from '@/types/request'
export interface ListParams {
  page?: number
  size?: number
  sort?: string
  status?: RequestStatus | ''
  category?: RequestCategory | ''
  priority?: RequestPriority | ''
  q?: string
}
export interface CreateRequestPayload {
  title: string
  description?: string
  category: RequestCategory
  priority?: RequestPriority
}
export interface Suggestion {
  category?: RequestCategory
  priority?: RequestPriority
  reason: string
  confidence?: number
}
function toQuery(params: ListParams): string {
  const { q, sort, ...rest } = params
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(rest))
    if (value !== undefined && value !== '') query.set(key, String(value))
  if (q?.trim()) query.set('search', q.trim())
  if (sort) {
    const [field, direction] = sort.split(',')
    query.set('sortBy', field || 'createdAt')
    query.set('sortDir', direction || 'desc')
  }
  return '?' + query.toString()
}
const requestPath = (id: string) => '/requests/' + encodeURIComponent(id)
export const requestService = {
  async list(params: ListParams): Promise<Page<SupportRequest>> {
    const page = await http.get<
      Omit<Page<RequestResponse>, 'page'> & { number: number }
    >('/requests' + toQuery(params))
    return { ...page, page: page.number, content: page.content.map(toRequest) }
  },
  async get(id: string) {
    return toRequest(await http.get<RequestResponse>(requestPath(id)))
  },
  async create(payload: CreateRequestPayload) {
    return toRequest(await http.post<RequestResponse>('/requests', payload))
  },
  async assign(id: string, developerId?: string) {
    const result = await http.patch<RequestResponse>(
      requestPath(id) + '/assign',
      developerId ? { developerId } : {},
    )
    return toRequest(result)
  },
  async updateStatus(id: string, status: RequestStatus, memo?: string) {
    const result = await http.patch<RequestResponse>(
      requestPath(id) + '/status',
      { status, memo: memo?.trim() || undefined },
    )
    return toRequest(result)
  },
  async history(id: string) {
    return (
      await http.get<HistoryResponse[]>(requestPath(id) + '/history')
    ).map(toHistory)
  },
  async stats() {
    return toStats(await http.get<StatsResponse>('/requests/stats'))
  },
  listAlerts: (unreadOnly = false) =>
    http.get<Alert[]>('/alerts?unreadOnly=' + unreadOnly),
  markAlertRead: (id: string) =>
    http.patch<Alert>('/alerts/' + encodeURIComponent(id) + '/read'),
  classify: (description: string) =>
    http.post<Suggestion>('/requests/classify', { description }),
  suggestPriority: (description: string) =>
    http.post<Suggestion>('/requests/suggest-priority', { description }),
  async suggest(description: string): Promise<Suggestion> {
    const results = await Promise.allSettled([
      this.classify(description),
      this.suggestPriority(description),
    ])
    const values = results.flatMap((result) =>
      result.status === 'fulfilled' ? [result.value] : [],
    )
    if (!values.length) throw (results[0] as PromiseRejectedResult).reason
    return {
      category: values.find((value) => value.category)?.category,
      priority: values.find((value) => value.priority)?.priority,
      reason: values.map((value) => value.reason).join(' '),
    }
  },
  summary: (id: string) =>
    http.get<{ requestId: string; summary: string }>(
      requestPath(id) + '/summary',
    ),
}
