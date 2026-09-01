import { motion } from 'framer-motion'
import { PencilSimple, Trash } from '@phosphor-icons/react'
import { cn } from '../lib/cn'
import { ingredientIcon } from '../lib/ingredientIcon'
import type { InventoryItem, InventoryStatus } from '../types'

const ICON_TINT: Record<InventoryStatus, string> = {
  FRESH: 'bg-card text-muted',
  EXPIRING_SOON: 'bg-amber-bg text-amber',
  EXPIRED: 'bg-expired-bg text-expired',
  USED: 'bg-line-soft/60 text-mist',
}

const EXPIRY_TEXT: Record<InventoryStatus, string> = {
  FRESH: 'text-mist',
  EXPIRING_SOON: 'text-amber',
  EXPIRED: 'text-expired',
  USED: 'text-mist',
}

function fmtExpiry(iso: string | null, status: InventoryStatus): string {
  if (!iso) return 'Không rõ hạn'
  const [y, m, d] = iso.split('-')
  const date = `${d}/${m}`
  if (status === 'EXPIRED') return `Hết hạn ${date}`
  if (status === 'EXPIRING_SOON') return `HSD ${date}`
  return `HSD ${date}/${y.slice(2)}`
}

function qtyLabel(i: InventoryItem): string {
  return [i.quantity ?? null, i.unit].filter((v) => v !== null && v !== '').join(' ')
}

/** 1 hàng trong bảng ledger tủ lạnh — icon nhóm nguyên liệu (đơn sắc, không emoji), tên, số
 *  lượng, hạn dùng canh phải. Motion trầm (fade+lift ngắn), không dùng spring nảy như trước —
 *  khớp tông editorial của phần còn lại trong app. */
export function IngredientRow({
  item,
  onEdit,
  onDelete,
}: {
  item: InventoryItem
  onEdit: () => void
  onDelete: () => void
}) {
  const status = item.status
  const Icon = ingredientIcon(item.normalizedName)
  const qty = qtyLabel(item)

  return (
    <motion.li
      layout
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
      className={cn(
        'group flex items-center gap-2 border-b border-line-soft px-1 py-3.5 last:border-0 sm:gap-4',
        status === 'USED' && 'opacity-50',
      )}
    >
      <span className={cn('grid size-9 shrink-0 place-items-center rounded-full sm:size-11', ICON_TINT[status])}>
        <Icon weight="duotone" className="size-5" />
      </span>

      <div className="min-w-0 flex-1">
        <p className="truncate text-[15px] font-medium text-ink">{item.ingredientName}</p>
        {qty && <p className="text-[12.5px] text-faint">{qty}</p>}
      </div>

      <span className={cn('shrink-0 text-[13px] font-medium tabular-nums', EXPIRY_TEXT[status])}>
        {fmtExpiry(item.expiryDate, status)}
      </span>

      {/* Dưới lg (chạm, không hover) luôn hiện; từ lg mới ẩn chờ hover — khớp quy ước breakpoint
          "lg = có chuột" đã dùng xuyên suốt app (sidebar/bottom-tab-bar). */}
      <div className="flex shrink-0 gap-0.5 opacity-100 transition-opacity lg:opacity-0 lg:group-hover:opacity-100 lg:focus-within:opacity-100">
        <button
          type="button"
          onClick={onEdit}
          aria-label={`Sửa ${item.ingredientName}`}
          className="grid size-8 place-items-center rounded-full text-mist transition-colors hover:bg-line-soft hover:text-accent"
        >
          <PencilSimple className="size-[15px]" />
        </button>
        <button
          type="button"
          onClick={onDelete}
          aria-label={`Xoá ${item.ingredientName}`}
          className="grid size-8 place-items-center rounded-full text-mist transition-colors hover:bg-line-soft hover:text-expired"
        >
          <Trash className="size-[15px]" />
        </button>
      </div>
    </motion.li>
  )
}
