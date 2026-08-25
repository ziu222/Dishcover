import { CircleNotch } from '@phosphor-icons/react'

/** Vòng tròn loading — dùng khi ĐANG fetch data (chưa có gì để hiện).
 *  Spinner là ca ngoại lệ được phép easing linear (quay đều). */
export function Spinner({ label = 'Đang tải công thức…' }: { label?: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-24" role="status" aria-live="polite">
      <CircleNotch weight="bold" className="size-8 animate-spin text-accent" />
      <span className="text-sm text-muted">{label}</span>
    </div>
  )
}
