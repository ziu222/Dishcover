import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell } from '@phosphor-icons/react'
import { useNotifications } from '../hooks/useNotifications'

export function NotificationBell() {
  const { items, unreadCount, markRead, markAllRead } = useNotifications()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const onSelect = async (id: number, actionUrl: string | null) => {
    await markRead(id)
    setOpen(false)
    if (actionUrl) navigate(actionUrl)
  }

  return (
    <div className="relative">
      <button
        type="button"
        aria-label="Thông báo"
        onClick={() => setOpen((v) => !v)}
        className="relative text-muted transition-colors hover:text-accent"
      >
        <Bell className="size-[22px]" />
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 grid size-4 place-items-center rounded-full bg-accent text-[10px] font-semibold text-surface">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>
      {open && (
        <div className="fixed inset-x-4 top-16 z-10 rounded-lg border border-line-soft bg-surface shadow-lg lg:absolute lg:inset-x-auto lg:top-full lg:right-0 lg:mt-2 lg:w-80">
          <div className="flex items-center justify-between border-b border-line-soft px-4 py-2.5">
            <span className="text-sm font-medium text-ink">Thông báo</span>
            {unreadCount > 0 && (
              <button type="button" onClick={markAllRead} className="text-xs text-accent hover:underline">
                Đánh dấu tất cả đã đọc
              </button>
            )}
          </div>
          <div className="max-h-80 overflow-y-auto">
            {items.length === 0 && (
              <p className="px-4 py-6 text-center text-sm text-muted">Chưa có thông báo nào.</p>
            )}
            {items.map((n) => (
              <button
                key={n.id}
                type="button"
                onClick={() => onSelect(n.id, n.actionUrl)}
                className={`block w-full border-b border-line-soft px-4 py-3 text-left text-sm last:border-0 hover:bg-line-soft/40 ${n.isRead ? 'text-muted' : 'font-medium text-ink'}`}
              >
                <p>{n.title}</p>
                <p className="mt-0.5 text-xs text-muted">{n.message}</p>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
