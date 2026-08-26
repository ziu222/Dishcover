import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { motion } from 'framer-motion'
import { X } from '@phosphor-icons/react'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

/** Dialog dùng chung: backdrop mờ + panel trượt lên. Đóng = unmount tức thì (giữ animation lúc
 *  mở cho mượt — không dùng AnimatePresence exit để tránh kẹt node). Escape + click nền để đóng. */
export function Modal({ open, onClose, title, children }: ModalProps) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      <motion.div
        onClick={onClose}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.18 }}
        className="absolute inset-0 bg-ink/40 backdrop-blur-sm"
      />
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 28 }}
        className="relative w-full max-w-md rounded-t-3xl bg-card p-6 shadow-[0_-8px_40px_-12px_rgba(40,34,24,0.3)] sm:rounded-3xl sm:shadow-[0_30px_60px_-20px_rgba(40,34,24,0.4)]"
      >
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-2xl font-light text-ink">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="grid size-9 place-items-center rounded-full text-mist transition-colors hover:bg-line-soft hover:text-muted"
          >
            <X className="size-5" />
          </button>
        </div>
        {children}
      </motion.div>
    </div>,
    document.body,
  )
}
