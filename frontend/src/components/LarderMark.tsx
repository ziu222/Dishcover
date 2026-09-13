/* Mark chính thức của Larder — cái tủ đựng thức ăn, dựng đúng theo "Larder Logo.dc.html"
   trong Claude Design (mục 03 Biểu tượng): khung vòm hai lớp, hai chân, vạch kệ chia đôi
   "nguyên liệu bạn có / món bạn có thể nấu", và hạt giống màu đất nung lặp lại dấu chấm
   trong chữ "Larder.".

   Màu lấy từ context: nét theo `currentColor`, hạt giống theo biến CSS --dot (mặc định
   terracotta) — đúng cách bản thiết kế đổi màu cho 5 nền (giấy/mực/đất nung/mono). */
export function LarderMark({ size = 28, className }: { size?: number; className?: string }) {
  return (
    <svg
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
      <path d="M16,112 L16,48 A34,34 0 0 1 84,48 L84,112 Z" strokeWidth={4.5} />
      <path d="M26,104 L26,50 A24,24 0 0 1 74,50 L74,104 Z" strokeWidth={2} opacity={0.55} />
      <line x1="22" y1="112" x2="22" y2="119" strokeWidth={4.5} />
      <line x1="78" y1="112" x2="78" y2="119" strokeWidth={4.5} />
      <line x1="26" y1="82" x2="74" y2="82" strokeWidth={4.5} />
      <line x1="50" y1="52" x2="50" y2="60" stroke="var(--dot, #a85d42)" strokeWidth={2.4} />
      <circle cx="50" cy="68" r="8" fill="var(--dot, #a85d42)" stroke="none" />
    </svg>
  )
}
