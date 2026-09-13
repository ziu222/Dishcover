/* Monogram Larder — port từ hàm `monogram()` trong "Larder Logo.dc.html" (Claude Design,
   mục 02): chữ L kiểu Newsreader mảnh, dấu chấm tròn nằm sát chân bên phải. Bản thiết kế
   dựng bằng chữ thật chứ không phải đường vẽ, nên ở đây giữ nguyên cách đó — chữ co giãn
   theo font, dấu chấm tính theo em nên tỉ lệ không đổi ở mọi cỡ. */
export function LarderMonogram({ size = 22, className }: { size?: number; className?: string }) {
  return (
    <span
      className={className}
      aria-hidden="true"
      style={{
        fontFamily: "'Newsreader', Georgia, serif",
        fontWeight: 200,
        fontSize: size,
        lineHeight: 1,
        position: 'relative',
        display: 'inline-block',
      }}
    >
      L
      <span
        style={{
          position: 'absolute',
          right: '-0.14em',
          bottom: '0.06em',
          width: '0.13em',
          height: '0.13em',
          borderRadius: '50%',
          background: 'var(--dot, #a85d42)',
        }}
      />
    </span>
  )
}

/** Monogram đặt trong ô bo tròn — bản compact cho sidebar/header, kiểu icon ứng dụng. */
export function LarderMonogramTile({ size = 34, className }: { size?: number; className?: string }) {
  return (
    <span
      className={className}
      style={{
        width: size,
        height: size,
        borderRadius: size * 0.3,
        display: 'inline-grid',
        placeItems: 'center',
      }}
    >
      <LarderMonogram size={size * 0.62} />
    </span>
  )
}
