import { motion } from 'framer-motion'
import { PencilSimple, Trash } from '@phosphor-icons/react'
import { cn } from '../lib/cn'
import { ingredientEmoji } from '../lib/ingredientEmoji'
import type { InventoryItem, InventoryStatus } from '../types'

// Một viên nguyên liệu trên kệ tủ lạnh. Độ tươi (status) kể chuyện bằng thị giác (concept ②):
//  FRESH        → ánh sương lạnh, tông bình thường
//  EXPIRING_SOON → quầng hổ phách ĐẬP nhẹ liên tục ("kêu cứu, dùng sớm")
//  EXPIRED      → xám hoá + nghiêng nhẹ như đã héo
//  USED         → mờ, không nhấn

// Pop-in so le: parent (<motion.ul>) điều phối qua staggerChildren, tile chỉ khai báo variant.
//  hidden → show : RƠI từ trên xuống + nảy (spring) "vào kệ"
//  exit          : bay LÊN + co lại + mờ dần "bốc hơi" (khi xoá, qua AnimatePresence)
export const tileVariants = {
  hidden: { opacity: 0, scale: 0.7, y: -24 },
  show: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: { type: 'spring' as const, stiffness: 300, damping: 18 },
  },
  exit: {
    opacity: 0,
    scale: 0.6,
    y: -28,
    transition: { duration: 0.3, ease: 'easeIn' as const },
  },
}

const RING: Record<InventoryStatus, string> = {
  FRESH: 'border-line-soft',
  EXPIRING_SOON: 'border-amber/50',
  EXPIRED: 'border-expired/30',
  USED: 'border-line-soft',
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

export function IngredientTile({
  item,
  onEdit,
  onDelete,
}: {
  item: InventoryItem
  onEdit: () => void
  onDelete: () => void
}) {
  const status = item.status
  const expired = status === 'EXPIRED'
  const expiring = status === 'EXPIRING_SOON'
  const qty = qtyLabel(item)

  return (
    <motion.li
      variants={tileVariants}
      exit="exit"
      layout
      className={cn(
        'group relative flex flex-col items-center gap-1.5 rounded-card border bg-card px-3 pb-3 pt-5 text-center',
        RING[status],
        status === 'USED' && 'opacity-60',
      )}
    >
      {/* Sương lạnh: đốm sáng mờ góc trên (chỉ đồ còn tươi) */}
      {status === 'FRESH' && (
        <span
          aria-hidden
          className="pointer-events-none absolute inset-0 rounded-card bg-[radial-gradient(120%_80%_at_25%_0%,rgba(255,255,255,0.85),transparent_55%)]"
        />
      )}

      {/* Quầng hổ phách đập nhẹ cho đồ sắp hết hạn */}
      {expiring && (
        <motion.span
          aria-hidden
          className="pointer-events-none absolute -inset-px rounded-card ring-2 ring-amber"
          initial={{ opacity: 0.25 }}
          animate={{ opacity: [0.25, 0.7, 0.25] }}
          transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
        />
      )}

      {/* Nút sửa/xoá — hiện khi hover */}
      <div className="absolute right-1.5 top-1.5 z-10 flex gap-0.5 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
        <button
          type="button"
          onClick={onEdit}
          aria-label={`Sửa ${item.ingredientName}`}
          className="grid size-7 place-items-center rounded-full bg-surface/80 text-mist backdrop-blur-sm transition-colors hover:text-accent"
        >
          <PencilSimple className="size-[15px]" />
        </button>
        <button
          type="button"
          onClick={onDelete}
          aria-label={`Xoá ${item.ingredientName}`}
          className="grid size-7 place-items-center rounded-full bg-surface/80 text-mist backdrop-blur-sm transition-colors hover:text-expired"
        >
          <Trash className="size-[15px]" />
        </button>
      </div>

      <span
        className={cn(
          'select-none text-[40px] leading-none drop-shadow-sm transition-transform',
          expired && 'rotate-[-8deg] opacity-70 grayscale',
        )}
        style={{ fontFamily: '"Segoe UI Emoji","Apple Color Emoji","Noto Color Emoji",sans-serif' }}
      >
        {ingredientEmoji(item.normalizedName)}
      </span>

      <span className="mt-1 line-clamp-2 w-full text-[13px] font-medium leading-snug text-ink">
        {item.ingredientName}
      </span>
      {qty && <span className="text-[11px] text-faint">{qty}</span>}
      <span className={cn('text-[11px] font-medium', EXPIRY_TEXT[status])}>
        {fmtExpiry(item.expiryDate, status)}
      </span>
    </motion.li>
  )
}
