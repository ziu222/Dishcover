import { motion, type HTMLMotionProps } from 'framer-motion'
import { CircleNotch } from '@phosphor-icons/react'
import type { ReactNode } from 'react'
import { cn } from '../lib/cn'

type Variant = 'primary' | 'secondary' | 'accent'
type Size = 'md' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ink text-surface hover:bg-ink/90',
  secondary: 'bg-transparent text-ink border border-line hover:border-mist',
  accent: 'bg-accent text-surface hover:bg-accent-strong',
}

const SIZES: Record<Size, string> = {
  md: 'px-6 py-3 text-sm',
  lg: 'px-7 py-4 text-[15px]',
}

interface ButtonProps extends Omit<HTMLMotionProps<'button'>, 'children'> {
  variant?: Variant
  size?: Size
  loading?: boolean
  fullWidth?: boolean
  children: ReactNode
}

/** Nút bấm dùng chung. Phản hồi xúc giác (scale khi nhấn) theo MOTION 6. */
export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  const isDisabled = disabled || loading
  return (
    <motion.button
      whileTap={isDisabled ? undefined : { scale: 0.97 }}
      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
      disabled={isDisabled}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-full font-medium tracking-[0.01em]',
        'transition-colors outline-none focus-visible:ring-2 focus-visible:ring-accent/40',
        'disabled:cursor-not-allowed disabled:opacity-55',
        VARIANTS[variant],
        SIZES[size],
        fullWidth && 'w-full',
        className,
      )}
      {...rest}
    >
      {loading && <CircleNotch weight="bold" className="size-4 animate-spin" />}
      {children}
    </motion.button>
  )
}
