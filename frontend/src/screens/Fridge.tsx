import { useMemo, useState, type FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Camera, Plus, Snowflake, Warning } from '@phosphor-icons/react'
import { useInventory, type ItemInput } from '../hooks/useInventory'
import { Button } from '../components/Button'
import { Field } from '../components/Field'
import { Chip } from '../components/Chip'
import { Modal } from '../components/Modal'
import { IngredientRow } from '../components/IngredientRow'
import { ImageRecognitionModal } from '../components/ImageRecognitionModal'
import { Spinner } from '../components/Spinner'
import { ApiError } from '../lib/api'
import { ingredientIcon } from '../lib/ingredientIcon'
import { cn } from '../lib/cn'
import type { InventoryItem } from '../types'

const STATUS_FILTERS: Array<{ value: string | null; label: string }> = [
  { value: null, label: 'Tất cả' },
  { value: 'FRESH', label: 'Còn hạn' },
  { value: 'EXPIRING_SOON', label: 'Sắp hết hạn' },
  { value: 'EXPIRED', label: 'Hết hạn' },
]

function daysUntil(iso: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(`${iso}T00:00:00`)
  return Math.round((target.getTime() - today.getTime()) / 86_400_000)
}

/** Câu ưu tiên "giải cứu" — mục tiêu chính của trụ cột Tủ lạnh ảo (CLAUDE.md mục 1). */
function urgencyText(days: number): string {
  if (days < 0) return `Đã hết hạn ${Math.abs(days)} ngày trước`
  if (days === 0) return 'Hết hạn hôm nay'
  if (days === 1) return 'Hết hạn ngày mai'
  return `Còn ${days} ngày là hết hạn`
}

/** Khối nổi bật cho nguyên liệu cần dùng gấp nhất — điểm nhấn thị giác duy nhất của trang,
 *  thay cho lưới ô vuông trước đây. Chỉ hiện khi thật sự có nguyên liệu sắp/đã hết hạn. */
function UrgentSpotlight({ item }: { item: InventoryItem }) {
  const expired = item.status === 'EXPIRED'
  const Icon = ingredientIcon(item.normalizedName)
  const days = item.expiryDate ? daysUntil(item.expiryDate) : 0

  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.4, 0, 0.2, 1] }}
      className={cn(
        'mb-8 flex items-center gap-5 rounded-2xl border px-6 py-5',
        expired ? 'border-expired/25 bg-expired-bg' : 'border-amber/25 bg-amber-bg',
      )}
    >
      <span
        className={cn(
          'grid size-14 shrink-0 place-items-center rounded-full bg-white/70',
          expired ? 'text-expired' : 'text-amber',
        )}
      >
        <Icon weight="duotone" className="size-7" />
      </span>
      <div className="min-w-0">
        <p className={cn('text-[11px] font-semibold uppercase tracking-[0.14em]', expired ? 'text-expired' : 'text-amber')}>
          Ưu tiên dùng trước
        </p>
        <p className="mt-1 font-display text-2xl font-normal leading-tight text-ink sm:text-3xl">
          {item.ingredientName} — {urgencyText(days)}
        </p>
      </div>
    </motion.div>
  )
}

export function Fridge() {
  const { items, loading, error, reload, add, update, remove, addBatch } = useInventory()
  const [status, setStatus] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [scanOpen, setScanOpen] = useState(false)
  const [editing, setEditing] = useState<InventoryItem | null>(null)
  const [deleting, setDeleting] = useState<InventoryItem | null>(null)

  // Sắp hết hạn/hết hạn lên trước (ưu tiên "giải cứu"); không rõ hạn xuống cuối.
  const sorted = useMemo(
    () =>
      [...items].sort((a, b) => (a.expiryDate ?? '9999-99-99').localeCompare(b.expiryDate ?? '9999-99-99')),
    [items],
  )
  const shown = status ? sorted.filter((i) => i.status === status) : sorted
  const urgent = sorted.find((i) => i.status === 'EXPIRING_SOON' || i.status === 'EXPIRED') ?? null
  const expiringCount = items.filter((i) => i.status === 'EXPIRING_SOON' || i.status === 'EXPIRED').length

  function openAdd() {
    setEditing(null)
    setFormOpen(true)
  }
  function openEdit(item: InventoryItem) {
    setEditing(item)
    setFormOpen(true)
  }

  return (
    <div className="px-6 py-9 lg:px-10">
      {/* Header */}
      <div className="mb-8 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
            Tủ lạnh ảo
          </div>
          <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
            Bạn đang có gì?
          </h1>
          {!loading && !error && (
            <p className="mt-3 text-sm text-muted">
              {items.length} nguyên liệu
              {expiringCount > 0 && (
                <>
                  {' · '}
                  <span className="font-medium text-amber">{expiringCount} cần dùng sớm</span>
                </>
              )}
            </p>
          )}
        </div>
        <div className="flex gap-2.5 self-start sm:self-auto">
          <Button variant="secondary" onClick={() => setScanOpen(true)}>
            <Camera weight="bold" className="size-4" />
            Nhận diện từ ảnh
          </Button>
          <Button onClick={openAdd}>
            <Plus weight="bold" className="size-4" />
            Thêm nguyên liệu
          </Button>
        </div>
      </div>

      {loading ? (
        <Spinner label="Đang tải tủ lạnh…" />
      ) : error ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <p className="text-[15px] text-muted">{error}</p>
          <Button variant="secondary" className="mt-5" onClick={reload}>
            Thử lại
          </Button>
        </div>
      ) : items.length === 0 ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <Snowflake className="mx-auto mb-4 size-10 text-mist" />
          <p className="font-display text-2xl font-light text-ink">Tủ lạnh đang trống</p>
          <p className="mt-2 text-sm text-muted">Thêm nguyên liệu để nhận gợi ý món nấu được.</p>
          <Button className="mt-5" onClick={openAdd}>
            <Plus weight="bold" className="size-4" />
            Thêm nguyên liệu
          </Button>
        </div>
      ) : (
        <>
          {urgent && <UrgentSpotlight item={urgent} />}

          <div className="mb-5 flex flex-wrap gap-2">
            {STATUS_FILTERS.map((f) => (
              <Chip key={f.label} active={status === f.value} onClick={() => setStatus(f.value)}>
                {f.label}
              </Chip>
            ))}
          </div>

          {shown.length === 0 ? (
            <p className="py-16 text-center text-sm text-muted">Không có nguyên liệu ở trạng thái này.</p>
          ) : (
            <ul className="rounded-card border border-line-soft bg-white px-5">
              <AnimatePresence initial={false}>
                {shown.map((item) => (
                  <IngredientRow
                    key={item.id}
                    item={item}
                    onEdit={() => openEdit(item)}
                    onDelete={() => setDeleting(item)}
                  />
                ))}
              </AnimatePresence>
            </ul>
          )}
        </>
      )}

      <ItemFormModal
        open={formOpen}
        editing={editing}
        onClose={() => setFormOpen(false)}
        onSubmit={async (input) => {
          if (editing) await update(editing.id, input)
          else await add(input)
          setFormOpen(false)
        }}
      />

      <ConfirmDelete
        item={deleting}
        onClose={() => setDeleting(null)}
        onConfirm={async () => {
          if (deleting) await remove(deleting.id)
          setDeleting(null)
        }}
      />

      <ImageRecognitionModal
        open={scanOpen}
        onClose={() => setScanOpen(false)}
        onConfirm={addBatch}
      />
    </div>
  )
}

function ItemFormModal({
  open,
  editing,
  onClose,
  onSubmit,
}: {
  open: boolean
  editing: InventoryItem | null
  onClose: () => void
  onSubmit: (input: ItemInput) => Promise<void>
}) {
  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState('')
  const [expiry, setExpiry] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  // Nạp giá trị mỗi lần mở (key trên Modal buộc remount nên state khởi tạo lại).
  const initKey = `${open}-${editing?.id ?? 'new'}`
  const [seenKey, setSeenKey] = useState('')
  if (open && seenKey !== initKey) {
    setSeenKey(initKey)
    setName(editing?.ingredientName ?? '')
    setQuantity(editing?.quantity != null ? String(editing.quantity) : '')
    setUnit(editing?.unit ?? '')
    setExpiry(editing?.expiryDate ?? '')
    setError(null)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSaving(true)
    try {
      const input: ItemInput = {
        quantity: quantity ? Number(quantity) : undefined,
        unit: unit.trim() || undefined,
        expiryDate: expiry || undefined,
      }
      if (!editing) input.ingredientName = name.trim()
      await onSubmit(input)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Lưu thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu'}>
      <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
        {editing ? (
          <div>
            <div className="mb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-faint">
              Nguyên liệu
            </div>
            <div className="text-[15px] font-medium text-ink">{editing.ingredientName}</div>
          </div>
        ) : (
          <Field
            label="Tên nguyên liệu"
            required
            autoFocus
            placeholder="VD: Cà chua"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        )}
        <div className="grid grid-cols-2 gap-4">
          <Field
            label="Số lượng"
            type="number"
            min={0}
            step="any"
            placeholder="3"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
          />
          <Field
            label="Đơn vị"
            placeholder="quả, g, ml…"
            value={unit}
            onChange={(e) => setUnit(e.target.value)}
          />
        </div>
        <Field
          label="Hạn dùng"
          type="date"
          helperText="Để trống sẽ tự suy hạn theo loại nguyên liệu"
          value={expiry}
          onChange={(e) => setExpiry(e.target.value)}
        />

        {error && (
          <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="mt-1 flex gap-3">
          <Button type="button" variant="secondary" fullWidth onClick={onClose}>
            Huỷ
          </Button>
          <Button type="submit" fullWidth loading={saving} disabled={!editing && !name.trim()}>
            {editing ? 'Lưu' : 'Thêm'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function ConfirmDelete({
  item,
  onClose,
  onConfirm,
}: {
  item: InventoryItem | null
  onClose: () => void
  onConfirm: () => Promise<void>
}) {
  const [busy, setBusy] = useState(false)
  return (
    <Modal open={item !== null} onClose={onClose} title="Xoá nguyên liệu?">
      <p className="text-[15px] leading-relaxed text-muted">
        Xoá <span className="font-medium text-ink">{item?.ingredientName}</span> khỏi tủ lạnh? Không thể hoàn tác.
      </p>
      <div className="mt-6 flex gap-3">
        <Button type="button" variant="secondary" fullWidth onClick={onClose}>
          Huỷ
        </Button>
        <Button
          type="button"
          variant="accent"
          fullWidth
          loading={busy}
          onClick={async () => {
            setBusy(true)
            try {
              await onConfirm()
            } finally {
              setBusy(false)
            }
          }}
        >
          Xoá
        </Button>
      </div>
    </Modal>
  )
}
