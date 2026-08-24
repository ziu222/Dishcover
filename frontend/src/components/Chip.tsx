import { motion } from 'framer-motion'
import type { ReactNode } from 'react'
import { cn } from '../lib/cn'

interface ChipProps {
  active?: boolean
  onClick?: () => void
  children: ReactNode
}

/** Chip lọc bấm được. Active = nền đất nung, idle = viền nhạt. */
export function Chip({ active = false, onClick, children }: ChipProps) {
  return (
    <motion.button
      type="button"
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
      aria-pressed={active}
      className={cn(
        'whitespace-nowrap rounded-full border px-4 py-2 text-[13px] font-medium transition-colors',
        'outline-none focus-visible:ring-2 focus-visible:ring-accent/40',
        active
          ? 'border-accent bg-accent text-surface'
          : 'border-line bg-surface text-muted hover:border-mist',
      )}
    >
      {children}
    </motion.button>
  )
}
