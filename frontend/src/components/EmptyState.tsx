import type { ReactNode } from 'react'
import { motion } from 'framer-motion'

interface EmptyStateProps {
  /** Dòng chính — Newsreader in nghiêng, theo mockup "Larder Foundation" (màn 05 Search). */
  title: ReactNode
  /** Câu chỉ đường, luôn nên nói rõ làm gì tiếp. */
  hint?: ReactNode
  /** Lối đi tiếp: chip gợi ý, nút, hoặc link. Bỏ trống nếu thật sự không có hành động nào. */
  children?: ReactNode
  /** Biểu tượng của bộ nhận diện (crest/con dấu). Chỉ dùng khi hình đó NÓI đúng thứ đang
   *  thiếu — VD cái tủ rỗng ở màn Tủ lạnh ảo; còn lại giữ thuần chữ như mockup gốc. */
  art?: ReactNode
}

/**
 * Trạng thái rỗng dùng chung. Bản thiết kế gốc cố ý KHÔNG dùng hình minh hoạ — chỉ một dòng
 * serif in nghiêng, một câu chỉ đường, rồi lối đi tiếp; nên ở đây cũng không có icon trang trí.
 */
export function EmptyState({ title, hint, children, art }: EmptyStateProps) {
  return (
    <motion.div
      className="mx-auto max-w-[460px] px-6 py-16 text-center sm:py-20"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
    >
      {art && <div className="mb-7 flex justify-center">{art}</div>}
      <p className="font-display text-2xl font-light italic text-muted sm:text-[30px] sm:leading-tight">
        {title}
      </p>
      {hint && <p className="mt-2.5 text-sm leading-relaxed text-faint">{hint}</p>}
      {children && (
        <div className="mt-7 flex flex-wrap items-center justify-center gap-2.5">{children}</div>
      )}
    </motion.div>
  )
}

/** Chip gợi ý trong trạng thái rỗng — nền wash, chữ đất nung đậm (mockup màn 05). */
export function EmptyStateChip({
  onClick,
  children,
}: {
  onClick: () => void
  children: ReactNode
}) {
  return (
    <motion.button
      type="button"
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
      className="rounded-full border border-accent/25 bg-accent-wash px-4 py-2.5 text-[13px] font-medium text-accent-strong transition-colors hover:border-accent/60 focus-visible:ring-2 focus-visible:ring-accent/40 focus-visible:outline-none"
    >
      {children}
    </motion.button>
  )
}
