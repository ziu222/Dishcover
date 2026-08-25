import { cn } from '../lib/cn'

// Trạng thái hạn dùng do Inventory Service tự derive theo expiryDate.
const MAP: Record<string, { label: string; className: string; dot: string }> = {
  FRESH: { label: 'Còn hạn', className: 'text-fresh bg-fresh-bg', dot: 'bg-fresh' },
  EXPIRING_SOON: { label: 'Sắp hết hạn', className: 'text-amber bg-amber-bg', dot: 'bg-amber' },
  EXPIRED: { label: 'Hết hạn', className: 'text-expired bg-expired-bg', dot: 'bg-expired' },
  USED: { label: 'Đã dùng', className: 'text-muted bg-line-soft/50', dot: 'bg-mist' },
}

export function StatusPill({ status }: { status: string }) {
  const s = MAP[status] ?? MAP.FRESH
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-[11px] font-semibold tracking-[0.02em]',
        s.className,
      )}
    >
      <span className={cn('size-1.5 rounded-full', s.dot)} />
      {s.label}
    </span>
  )
}
