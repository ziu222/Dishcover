import { motion } from 'framer-motion'

/* Mark chính thức của Larder — cái tủ đựng thức ăn, dựng đúng theo "Larder Logo.dc.html"
   trong Claude Design (mục 03 Biểu tượng): khung vòm hai lớp, hai chân, vạch kệ chia đôi
   "nguyên liệu bạn có / món bạn có thể nấu", và hạt giống màu đất nung lặp lại dấu chấm
   trong chữ "Larder.".

   Màu lấy từ context: nét theo `currentColor`, hạt giống theo biến CSS --dot (mặc định
   terracotta) — đúng cách bản thiết kế đổi màu cho 5 nền (giấy/mực/đất nung/mono). */
const ease = [0.16, 1, 0.3, 1] as const
const outline = {
  hidden: { pathLength: 0, opacity: 0 },
  show: {
    pathLength: 1,
    opacity: 1,
    transition: { pathLength: { duration: 1, ease }, opacity: { duration: 0.2 } },
  },
}
const seed = {
  hidden: { opacity: 0, scale: 0.4 },
  show: {
    opacity: 1,
    scale: 1,
    transition: { type: 'spring' as const, stiffness: 380, damping: 22, delay: 0.75 },
  },
}

interface MarkProps {
  size?: number
  className?: string
  /** Tự vẽ khung tủ rồi thả hạt giống vào — dùng cho trạng thái tủ lạnh rỗng. */
  draw?: boolean
}

export function LarderMark({ size = 28, className, draw = false }: MarkProps) {
  const run = draw ? ({ initial: 'hidden' as const, animate: 'show' as const }) : {}
  return (
    <motion.svg
      {...run}
      transition={{ staggerChildren: 0.14 }}
      viewBox="0 0 100 120"
      width={size}
      height={size * 1.2}
      className={className}
      aria-hidden="true"
      focusable="false"
      fill="none"
      stroke="currentColor"
      strokeLinejoin="round"
      strokeLinecap="round"
    >
      <motion.path d="M16,112 L16,48 A34,34 0 0 1 84,48 L84,112 Z" strokeWidth={4.5} variants={outline} />
      <motion.path d="M26,104 L26,50 A24,24 0 0 1 74,50 L74,104 Z" strokeWidth={2} opacity={0.55} variants={outline} />
      <motion.line x1="22" y1="112" x2="22" y2="119" strokeWidth={4.5} variants={outline} />
      <motion.line x1="78" y1="112" x2="78" y2="119" strokeWidth={4.5} variants={outline} />
      <motion.line x1="26" y1="82" x2="74" y2="82" strokeWidth={4.5} variants={outline} />
      <motion.g variants={seed} style={{ originX: '50px', originY: '64px' }}>
        <line x1="50" y1="52" x2="50" y2="60" stroke="var(--dot, #a85d42)" strokeWidth={2.4} />
        <circle cx="50" cy="68" r="8" fill="var(--dot, #a85d42)" stroke="none" />
      </motion.g>
    </motion.svg>
  )
}
