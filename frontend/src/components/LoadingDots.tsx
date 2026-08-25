import { motion, useReducedMotion } from 'framer-motion'

/** Ba chấm nảy nhẹ so le — dùng khi ĐANG fetch data (chưa có gì để hiện).
 *  Ambient loop: y + opacity, sine easing, stagger 0.15s. Tôn trọng reduced-motion. */
export function LoadingDots({ label = 'Đang tải công thức…' }: { label?: string }) {
  const reduce = useReducedMotion()
  return (
    <div className="flex flex-col items-center gap-4 py-24" role="status" aria-live="polite">
      <div className="flex items-center gap-2">
        {[0, 1, 2].map((i) => (
          <motion.span
            key={i}
            className="size-2.5 rounded-full bg-accent"
            animate={reduce ? { opacity: 0.6 } : { y: [0, -7, 0], opacity: [0.35, 1, 0.35] }}
            transition={
              reduce
                ? undefined
                : { duration: 0.9, repeat: Infinity, ease: 'easeInOut', delay: i * 0.15 }
            }
          />
        ))}
      </div>
      <span className="text-sm text-muted">{label}</span>
    </div>
  )
}
