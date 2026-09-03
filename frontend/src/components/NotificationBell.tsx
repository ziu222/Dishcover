import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Bell } from '@phosphor-icons/react'
import { useNotifications } from '../hooks/useNotifications'

const PANEL_SPRING = { type: 'spring', stiffness: 420, damping: 32, mass: 0.6 } as const

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
        <AnimatePresence>
          {unreadCount > 0 && (
            <motion.span
              key="badge"
              initial={{ opacity: 0, scale: 0.4 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.4 }}
              transition={PANEL_SPRING}
              className="absolute -right-1 -top-1 grid size-4 place-items-center rounded-full bg-accent text-[10px] font-semibold text-surface"
            >
              {unreadCount > 9 ? '9+' : unreadCount}
            </motion.span>
          )}
        </AnimatePresence>
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -8 }}
            transition={PANEL_SPRING}
            style={{ transformOrigin: 'top right' }}
            className="fixed inset-x-4 top-16 z-10 rounded-lg border border-line-soft bg-surface shadow-lg lg:absolute lg:inset-x-auto lg:top-full lg:right-0 lg:mt-2 lg:w-80"
          >
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
              {items.map((n, i) => (
                <motion.button
                  key={n.id}
                  type="button"
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.22, delay: i * 0.035, ease: [0.4, 0, 0.2, 1] }}
                  onClick={() => onSelect(n.id, n.actionUrl)}
                  className={`block w-full border-b border-line-soft px-4 py-3 text-left text-sm last:border-0 hover:bg-line-soft/40 ${n.isRead ? 'text-muted' : 'font-medium text-ink'}`}
                >
                  <p>{n.title}</p>
                  <p className="mt-0.5 text-xs text-muted">{n.message}</p>
                </motion.button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
