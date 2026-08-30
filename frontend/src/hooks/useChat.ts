import { useCallback, useState } from 'react'
import { api, ApiError } from '../lib/api'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  sourceRecipeIds?: string[]
  fallback?: boolean
  error?: boolean
  /** Nguyên liệu vi phạm đặc điểm ăn uống đã khai báo — tính sẵn ở backend, không parse từ content. */
  dietaryWarnings?: string[]
}

interface ChatResponse {
  answer: string
  sourceRecipeIds: string[]
  fallback: boolean
  dietaryWarnings: string[]
}

/** Chat RAG — POST /rag-service/chat. 1 conversationId cố định suốt phiên chat (lịch sử lưu server-side TTL 30'). */
export function useChat() {
  const [conversationId] = useState(() => crypto.randomUUID())
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [sending, setSending] = useState(false)

  const send = useCallback(
    async (text: string) => {
      const trimmed = text.trim()
      if (!trimmed || sending) return

      setMessages((m) => [...m, { id: crypto.randomUUID(), role: 'user', content: trimmed }])
      setSending(true)
      try {
        const res = await api<ChatResponse>('/rag-service/chat', {
          method: 'POST',
          body: { message: trimmed, conversationId },
        })
        setMessages((m) => [
          ...m,
          {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: res.answer,
            sourceRecipeIds: res.sourceRecipeIds,
            fallback: res.fallback,
            dietaryWarnings: res.dietaryWarnings,
          },
        ])
      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'Không kết nối được trợ lý, thử lại.'
        setMessages((m) => [
          ...m,
          { id: crypto.randomUUID(), role: 'assistant', content: msg, error: true },
        ])
      } finally {
        setSending(false)
      }
    },
    [conversationId, sending],
  )

  return { messages, sending, send }
}
