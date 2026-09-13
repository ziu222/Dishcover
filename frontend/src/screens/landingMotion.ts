import type { Transition, Variants } from 'framer-motion'

/* Bộ chuyển động dùng chung cho trang giới thiệu.
   Một đường cong duy nhất cho cả trang (nặng lúc bắt đầu, trôi dần về cuối) — không dùng
   ease mặc định của trình duyệt. Bản CSS tương ứng là --lp-ease trong landing.css. */
export const ease: [number, number, number, number] = [0.16, 1, 0.3, 1]

/** Lò xo cho tương tác bấm/chọn — đủ đằm để không nảy như đồ chơi. */
export const spring: Transition = { type: 'spring', stiffness: 420, damping: 34, mass: 0.7 }

/** Khối nội dung trôi lên. */
export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 22 },
  show: { opacity: 1, y: 0, transition: { duration: 0.75, ease } },
}

/** Phần tử nhỏ (thẻ, icon) hiện lên kèm co giãn nhẹ. */
export const popIn: Variants = {
  hidden: { opacity: 0, y: 14, scale: 0.97 },
  show: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.6, ease } },
}

/** Dòng chữ trượt lên từ trong khung che — bọc trong .landing-mask (overflow: hidden). */
export const maskLine: Variants = {
  hidden: { y: '110%' },
  show: { y: '0%', transition: { duration: 0.9, ease } },
}

/** Cha điều phối nhịp cho con (fadeUp/popIn/maskLine). */
export const group = (stagger = 0.08, delay = 0): Variants => ({
  hidden: {},
  show: { transition: { staggerChildren: stagger, delayChildren: delay } },
})

/** Props dùng lại cho khối chạy khi cuộn tới. */
export const inView = {
  initial: 'hidden',
  whileInView: 'show',
  viewport: { once: true, amount: 0.15 },
} as const

/** Khối vừa tự trôi lên, vừa điều phối nhịp cho con — dùng chung với {...inView}. */
export const revealBlock = (stagger = 0.08, delay = 0): Variants => ({
  hidden: { opacity: 0, y: 22 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.75, ease, staggerChildren: stagger, delayChildren: delay },
  },
})
