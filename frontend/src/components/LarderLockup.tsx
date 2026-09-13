import { LarderCrest } from './LarderCrest'

/* Logo chính (lockup) — bố cục lấy đúng mục "01 Logo chính" trong "Larder Logo.dc.html":
   crest bên trái, vạch dọc mảnh ngăn giữa, wordmark + dòng chữ thưa "COOK WHAT YOU HAVE"
   bên phải. Nền giấy có gradient toả + viền mảnh + bóng trong, cũng theo bản thiết kế. */
export function LarderLockup({ className }: { className?: string }) {
  return (
    <div
      className={className}
      style={{
        background: 'radial-gradient(120% 140% at 50% 0%, #FBF8F2 0%, #F4EFE5 60%, #EDE6D8 100%)',
        border: '1px solid var(--color-line)',
        borderRadius: 14,
        boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.6)',
      }}
    >
      <div className="flex items-center justify-center gap-8 px-8 py-10 sm:gap-13 sm:px-14 sm:py-14">
        <LarderCrest size={78} draw className="text-ink [--dot:var(--color-accent)]" />
        <span className="h-25 w-px shrink-0 bg-line sm:h-32" />
        <div>
          <div className="font-display text-[44px] font-extralight leading-[0.9] tracking-tight text-ink sm:text-[64px]">
            Larder<span className="text-accent">.</span>
          </div>
          <div className="mt-3 text-[10px] uppercase tracking-[0.42em] text-mist sm:mt-4 sm:text-[12px]">
            Cook what you have
          </div>
        </div>
      </div>
    </div>
  )
}
