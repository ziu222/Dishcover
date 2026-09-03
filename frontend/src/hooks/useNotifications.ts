import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import type { NotificationListResponse } from '../types'

const POLL_MS = 60_000

/** Danh sách thông báo + badge unread — GET /notification-service/notifications (cần JWT cookie). */
export function useNotifications() {
  const [data, setData] = useState<NotificationListResponse>({ items: [], unreadCount: 0 })

  const reload = () => {
    api<NotificationListResponse>('/notification-service/notifications').then(setData).catch(() => {})
  }

  useEffect(() => {
    reload()
    const id = setInterval(reload, POLL_MS)
    return () => clearInterval(id)
  }, [])

  const markRead = async (id: number) => {
    await api(`/notification-service/notifications/${id}/read`, { method: 'PATCH' })
    reload()
  }

  const markAllRead = async () => {
    await api('/notification-service/notifications/read-all', { method: 'PATCH' })
    reload()
  }

  return { ...data, markRead, markAllRead }
}
