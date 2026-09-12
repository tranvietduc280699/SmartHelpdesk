import { http } from './http'

export type ChatRole = 'ADMIN' | 'DEVELOPER' | 'CLIENT'

export interface ChatMessage {
  id: string
  requestId: string
  requestTitle: string | null
  senderId: string
  senderName: string | null
  senderRole: ChatRole | null
  message: string
  messageType: string
  isRead: boolean
  createdAt: string
  persisted: boolean
}

const messagesPath = (requestId: string) =>
  '/chat/requests/' + encodeURIComponent(requestId) + '/messages'

export const chatService = {
  list: (requestId: string) => http.get<ChatMessage[]>(messagesPath(requestId)),
  send: (requestId: string, message: string, messageType = 'TEXT') =>
    http.post<ChatMessage>(messagesPath(requestId), { message, messageType }),
  inbox: () => http.get<ChatMessage[]>('/chat/inbox'),
  markRead: (messageId: string) =>
    http.patch<ChatMessage>(
      '/chat/messages/' + encodeURIComponent(messageId) + '/read',
    ),
}
