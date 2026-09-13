import { motion } from 'framer-motion'

/* Crest chi tiết của Larder — port nguyên từ hàm `larder()` trong "Larder Logo.dc.html"
   (Claude Design, mục 03). Khác `LarderMark` ở chỗ đây là bản đầy đủ: mái vòm có chóp,
   cornice, hốc vòm, 3 tầng kệ có hũ/vại/chai/bánh xe, chân tủ xoè.

   Màu: nét theo `currentColor`, điểm nhấn theo biến CSS --dot.
   `draw`: tự vẽ nét khi xuất hiện — viền tủ chạy theo pathLength, rồi kệ, rồi đồ trên kệ.
   Không bật `draw` thì mọi phần render tĩnh (không có variants nào được kích hoạt). */

const ease = [0.16, 1, 0.3, 1] as const

const outline = {
  hidden: { pathLength: 0, opacity: 0 },
  show: { pathLength: 1, opacity: 1, transition: { pathLength: { duration: 1.1, ease }, opacity: { duration: 0.2 } } },
}
const shelf = {
  hidden: { scaleX: 0, opacity: 0 },
  show: { scaleX: 1, opacity: 1, transition: { duration: 0.5, ease } },
}
const settle = {
  hidden: { opacity: 0, y: 6 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease } },
}

interface JarProps {
  cx: number
  baseY: number
  w: number
  hh: number
  terra?: boolean
}

function Jar({ cx, baseY, w, hh, terra }: JarProps) {
  const x = cx - w / 2
  return (
    <g fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinejoin="round" strokeLinecap="round">
      <rect x={x} y={baseY - hh} width={w} height={hh} rx={2.4} />
      <line x1={x} y1={baseY - hh + 4.5} x2={x + w} y2={baseY - hh + 4.5} />
      <rect
        x={x - 1}
        y={baseY - hh - 4.4}
        width={w + 2}
        height={4.6}
        rx={1.4}
        fill={terra ? 'var(--dot, #a85d42)' : 'none'}
        stroke={terra ? 'var(--dot, #a85d42)' : 'currentColor'}
      />
    </g>
  )
}

function Crock({ cx, baseY, w, hh }: { cx: number; baseY: number; w: number; hh: number }) {
  const x = cx - w / 2
  return (
    <g fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinejoin="round" strokeLinecap="round">
      <path
        d={`M${x},${baseY - hh + 4} Q${x - 3},${baseY - hh * 0.5} ${x + 2},${baseY} L${x + w - 2},${baseY} Q${x + w + 3},${baseY - hh * 0.5} ${x + w},${baseY - hh + 4} Z`}
      />
      <line x1={x - 1.5} y1={baseY - hh + 4} x2={x + w + 1.5} y2={baseY - hh + 4} />
      <line
        x1={x + 2}
        y1={baseY - hh + 9}
        x2={x + w - 2}
        y2={baseY - hh + 9}
        stroke="var(--dot, #a85d42)"
        strokeWidth={2.4}
      />
    </g>
  )
}

function Bottle({ cx, baseY, hh }: { cx: number; baseY: number; hh: number }) {
  return (
    <g fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinejoin="round" strokeLinecap="round">
      <rect x={cx - 4.5} y={baseY - hh} width={9} height={hh} rx={2} />
      <path
        d={`M${cx - 2.4},${baseY - hh} L${cx - 2.4},${baseY - hh - 6} Q${cx - 2.4},${baseY - hh - 8.5} ${cx},${baseY - hh - 8.5} Q${cx + 2.4},${baseY - hh - 8.5} ${cx + 2.4},${baseY - hh - 6} L${cx + 2.4},${baseY - hh}`}
      />
      <line x1={cx - 2.4} y1={baseY - hh - 8.5} x2={cx + 2.4} y2={baseY - hh - 8.5} />
    </g>
  )
}

interface CrestProps {
  size?: number
  className?: string
  /** Tự vẽ nét khi xuất hiện. Tắt (mặc định) thì render tĩnh. */
  draw?: boolean
}

export function LarderCrest({ size = 120, className, draw = false }: CrestProps) {
  const run = draw ? ({ initial: 'hidden' as const, animate: 'show' as const }) : {}
  return (
    <motion.svg
      viewBox="0 0 120 158"
      width={size}
      height={size * 1.32}
      className={className}
      aria-hidden="true"
      focusable="false"
      {...run}
      transition={{ staggerChildren: 0.12 }}
    >
      {/* chóp + mái vòm + cornice */}
      <motion.g variants={settle}>
        <circle cx={60} cy={9} r={3} fill="var(--dot, #a85d42)" />
        <line x1={60} y1={12} x2={60} y2={19} stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
        <path d="M20,28 Q60,6 100,28" fill="none" stroke="currentColor" strokeWidth={2.4} strokeLinecap="round" />
        <rect x={20} y={28} width={80} height={8} rx={1.5} fill="none" stroke="currentColor" strokeWidth={2.4} />
        <line x1={24} y1={24} x2={96} y2={24} stroke="currentColor" strokeWidth={1.4} opacity={0.6} />
      </motion.g>

      {/* thân tủ + hốc vòm — hai nét này chạy theo pathLength */}
      <motion.rect
        x={24}
        y={36}
        width={72}
        height={96}
        rx={3.5}
        fill="none"
        stroke="currentColor"
        strokeWidth={2.6}
        variants={outline}
      />
      <motion.path
        d="M33,128 L33,66 A27,27 0 0 1 87,66 L87,128 Z"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.8}
        variants={outline}
      />
      <motion.path d="M55,40 L65,40 L63,49 L57,49 Z" fill="var(--dot, #a85d42)" variants={settle} />

      {/* 3 tầng kệ — mở ra từ giữa */}
      <motion.g variants={shelf} style={{ originX: '60px', originY: '0px' }}>
        <line x1={33} y1={86} x2={87} y2={86} stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" />
        <line x1={33} y1={108} x2={87} y2={108} stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" />
        <line x1={33} y1={128} x2={87} y2={128} stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" />
      </motion.g>

      {/* đế + chân xoè */}
      <motion.g variants={settle}>
        <rect x={20} y={130} width={80} height={5} rx={1.5} fill="none" stroke="currentColor" strokeWidth={2} />
        <path d="M30,135 L27,146 M36,135 L38,146" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
        <path d="M90,135 L93,146 M84,135 L82,146" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
      </motion.g>

      {/* đồ trên kệ — vào sau cùng, như vừa được xếp vào tủ */}
      <motion.g variants={settle}>
        <Jar cx={46} baseY={86} w={12} hh={18} />
        <Jar cx={60} baseY={86} w={12} hh={20} terra />
        <Jar cx={74} baseY={86} w={12} hh={18} />
        <Crock cx={52} baseY={108} w={26} hh={17} />
        <Bottle cx={76} baseY={108} hh={17} />
        <Jar cx={42} baseY={128} w={11} hh={15} />
        <circle cx={60} cy={121} r={7} fill="none" stroke="currentColor" strokeWidth={1.7} />
        <line x1={60} y1={114} x2={60} y2={128} stroke="currentColor" strokeWidth={1.2} opacity={0.55} />
        <Jar cx={78} baseY={128} w={11} hh={15} />
      </motion.g>
    </motion.svg>
  )
}
