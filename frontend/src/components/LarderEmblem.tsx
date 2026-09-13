import { useId } from 'react'
import { LarderCrest } from './LarderCrest'

/* Con dấu Larder — port từ hàm `emblem()` trong "Larder Logo.dc.html" (Claude Design, mục 02
   Con dấu & Monogram): hai vòng tròn đồng tâm, chữ LARDER chạy theo cung trên, hai nhánh lúa
   mì ôm dưới, crest ở giữa và ribbon "EST · 2026".

   Bản thiết kế có 3 biến thể màu (giấy / mực / đất nung) — ở đây nhận qua props thay vì
   hardcode, mặc định là biến thể giấy. */

/** Nhánh lúa mì: cọng thẳng + 5 cặp hạt toả ra. Dùng riêng được làm ornament ngăn mục. */
export function WheatSprig({ size = 40, className }: { size?: number; className?: string }) {
  return (
    <svg
      viewBox="-12 -34 24 40"
      width={size}
      height={size * 1.67}
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <SprigPaths />
    </svg>
  )
}

function SprigPaths() {
  const grains = []
  for (let i = 0; i < 5; i++) {
    const y = -i * 5.5
    grains.push(
      <path key={`l${i}`} d={`M0,${y} q-6,-1 -9,-6`} fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" />,
      <path key={`r${i}`} d={`M0,${y} q6,-1 9,-6`} fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" />,
    )
  }
  return (
    <>
      <path d="M0,4 L0,-30" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" />
      {grains}
    </>
  )
}

interface EmblemProps {
  size?: number
  /** Màu 2 vòng tròn và ribbon. */
  ring?: string
  /** Màu nét chính (chữ, lúa mì, crest). */
  ink?: string
  /** Màu điểm nhấn (chóp, đá đỉnh vòm, nắp hũ giữa, viên kim cương trên đỉnh). */
  dot?: string
  className?: string
}

export function LarderEmblem({
  size = 200,
  ring = '#c2a98f',
  ink = '#4b473e',
  dot = '#a85d42',
  className,
}: EmblemProps) {
  // textPath cần id thật sự duy nhất — 2 con dấu trên cùng một trang sẽ đụng nhau nếu hardcode.
  const arcId = useId()
  return (
    <svg
      viewBox="0 0 200 200"
      width={size}
      height={size}
      className={className}
      role="img"
      aria-label="Con dấu Larder"
    >
      <defs>
        <path id={arcId} d="M26,102 A74,74 0 0,1 174,102" />
      </defs>
      <circle cx={100} cy={100} r={90} fill="none" stroke={ring} strokeWidth={1.5} />
      <circle cx={100} cy={100} r={83} fill="none" stroke={ring} strokeWidth={0.75} opacity={0.6} />
      <text
        fill={ink}
        style={{ fontFamily: "'Be Vietnam Pro', sans-serif", fontSize: '14px', letterSpacing: '0.46em', fontWeight: 500 }}
      >
        <textPath href={`#${arcId}`} startOffset="50%" textAnchor="middle">
          LARDER
        </textPath>
      </text>
      <path d="M100,20 l3,4 -3,4 -3,-4 Z" fill={dot} />
      <g transform="translate(58,150) rotate(28)" style={{ color: ink }}>
        <SprigPaths />
      </g>
      <g transform="translate(142,150) rotate(-28) scale(-1,1)" style={{ color: ink }}>
        <SprigPaths />
      </g>
      <g transform="translate(66,44)" style={{ color: ink, ['--dot' as string]: dot }}>
        <LarderCrest size={68} />
      </g>
      <path d="M62,158 L138,158 L133,168 L138,178 L62,178 L67,168 Z" fill="none" stroke={ring} strokeWidth={1.3} />
      <text
        x={100}
        y={171.5}
        fill={ink}
        textAnchor="middle"
        style={{ fontFamily: "'Be Vietnam Pro', sans-serif", fontSize: '8px', letterSpacing: '0.3em', fontWeight: 500 }}
      >
        EST · 2026
      </text>
    </svg>
  )
}
