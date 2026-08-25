import { CaretLeft, CaretRight } from '@phosphor-icons/react'
import { cn } from '../lib/cn'

interface PaginationProps {
  /** trang hiện tại, 0-based */
  page: number
  totalPages: number
  onChange: (page: number) => void
}

/** Danh sách trang cần hiển thị: đầu, cuối, và cửa sổ ±1 quanh trang hiện tại, chèn 'gap'. */
function buildPages(current: number, total: number): Array<number | 'gap'> {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages: Array<number | 'gap'> = [0]
  const start = Math.max(1, current - 1)
  const end = Math.min(total - 2, current + 1)
  if (start > 1) pages.push('gap')
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 2) pages.push('gap')
  pages.push(total - 1)
  return pages
}

const btn =
  'grid h-10 min-w-10 place-items-center rounded-full px-3 text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-40'

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <nav className="mt-12 flex items-center justify-center gap-1.5" aria-label="Phân trang">
      <button
        type="button"
        className={cn(btn, 'text-muted hover:bg-line-soft/60')}
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        aria-label="Trang trước"
      >
        <CaretLeft className="size-4" />
      </button>

      {buildPages(page, totalPages).map((p, i) =>
        p === 'gap' ? (
          <span key={`gap-${i}`} className="px-1 text-faint">
            …
          </span>
        ) : (
          <button
            key={p}
            type="button"
            onClick={() => onChange(p)}
            aria-current={p === page ? 'page' : undefined}
            className={cn(
              btn,
              p === page ? 'bg-ink font-medium text-surface' : 'text-muted hover:bg-line-soft/60',
            )}
          >
            {p + 1}
          </button>
        ),
      )}

      <button
        type="button"
        className={cn(btn, 'text-muted hover:bg-line-soft/60')}
        onClick={() => onChange(page + 1)}
        disabled={page === totalPages - 1}
        aria-label="Trang sau"
      >
        <CaretRight className="size-4" />
      </button>
    </nav>
  )
}
