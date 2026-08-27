import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Plus, Snowflake, Warning } from '@phosphor-icons/react'
import { useInventory, type ItemInput } from '../hooks/useInventory'
import { Button } from '../components/Button'
import { Field } from '../components/Field'
import { Chip } from '../components/Chip'
import { Modal } from '../components/Modal'
import { IngredientTile } from '../components/IngredientTile'
import { Spinner } from '../components/Spinner'
import { ApiError } from '../lib/api'
import type { InventoryItem } from '../types'

// Hiệu ứng "mở cửa tủ lạnh" chỉ chạy LẦN ĐẦU vào màn trong mỗi phiên (sessionStorage).
const DOOR_KEY = 'larder_fridge_opened'
function doorAlreadyOpened(): boolean {
  try {
    return !!sessionStorage.getItem(DOOR_KEY)
  } catch {
    return true // storage lỗi (private mode…) → bỏ qua animation, không ép chạy
  }
}
function markDoorOpened() {
  try {
    sessionStorage.setItem(DOOR_KEY, '1')
  } catch {
    /* bỏ qua */
  }
}

const STATUS_FILTERS: Array<{ value: string | null; label: string }> = [
  { value: null, label: 'Tất cả' },
  { value: 'FRESH', label: 'Còn hạn' },
  { value: 'EXPIRING_SOON', label: 'Sắp hết hạn' },
  { value: 'EXPIRED', label: 'Hết hạn' },
]

export function Fridge() {
  const { items, loading, error, reload, add, update, remove } = useInventory()
  const [status, setStatus] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<InventoryItem | null>(null)
  const [deleting, setDeleting] = useState<InventoryItem | null>(null)
  const [showDoor, setShowDoor] = useState(() => !doorAlreadyOpened())

  // Sắp hết hạn/hết hạn lên trước (ưu tiên "giải cứu"); không rõ hạn xuống cuối.
  const sorted = useMemo(
    () =>
      [...items].sort((a, b) => (a.expiryDate ?? '9999-99-99').localeCompare(b.expiryDate ?? '9999-99-99')),
    [items],
  )
  const shown = status ? sorted.filter((i) => i.status === status) : sorted
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
      {/* Không bọc AnimatePresence: gỡ thẳng khi xong để overlay không bao giờ kẹt che tủ lạnh
          (exit animation phụ thuộc rAF, có thể đứng yên khi tab ẩn). */}
      {showDoor && (
        <FridgeDoorReveal
          onDone={() => {
            markDoorOpened()
            setShowDoor(false)
          }}
        />
      )}

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
        <Button onClick={openAdd} className="self-start sm:self-auto">
          <Plus weight="bold" className="size-4" />
          Thêm nguyên liệu
        </Button>
      </div>

      {loading ? (
        <Spinner label="Đang mở tủ lạnh…" />
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
          <div className="mb-6 flex flex-wrap gap-2">
            {STATUS_FILTERS.map((f) => (
              <Chip key={f.label} active={status === f.value} onClick={() => setStatus(f.value)}>
                {f.label}
              </Chip>
            ))}
          </div>

          {shown.length === 0 ? (
            <p className="py-16 text-center text-sm text-muted">Không có nguyên liệu ở trạng thái này.</p>
          ) : (
            <motion.ul
              key={status ?? 'all'}
              className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5"
              initial="hidden"
              animate="show"
              variants={{ show: { transition: { staggerChildren: 0.04 } } }}
            >
              <AnimatePresence mode="popLayout">
                {shown.map((item) => (
                  <IngredientTile
                    key={item.id}
                    item={item}
                    onEdit={() => openEdit(item)}
                    onDelete={() => setDeleting(item)}
                  />
                ))}
              </AnimatePresence>
            </motion.ul>
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

/**
 * Hiệu ứng vào màn: hai cánh cửa tủ lạnh tách ra hai bên lộ kệ đồ phía sau + luồng hơi lạnh toả ra.
 * Gỡ overlay bằng setTimeout (KHÔNG dựa onAnimationComplete) để chắc chắn biến mất kể cả khi
 * trình duyệt tạm dừng rAF (tab ẩn) — không bao giờ để cửa kẹt che mất tủ lạnh.
 */
function FridgeDoorReveal({ onDone }: { onDone: () => void }) {
  useEffect(() => {
    const t = setTimeout(onDone, 1700)
    return () => clearTimeout(t)
  }, [onDone])

  const ease = [0.7, 0, 0.2, 1] as const
  const slide = { duration: 1.0, ease, delay: 0.4 }
  // Chất "thép/kính mờ" — CỐ Ý khác tông kem ấm của nội dung để lúc cửa tách ra thấy rõ.
  const steel = { background: 'linear-gradient(135deg,#f4f7f9 0%,#dbe3e7 55%,#c6d1d6 100%)' }

  return (
    <motion.div className="fixed inset-0 z-50 overflow-hidden">
      {/* hơi lạnh toả ra rồi tan */}
      <motion.div
        className="absolute inset-0 bg-white"
        initial={{ opacity: 0.75 }}
        animate={{ opacity: 0 }}
        transition={{ duration: 1.4 }}
      />
      {/* nhãn giữa khe cửa, mờ đi trước khi cửa mở */}
      <motion.div
        className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3"
        initial={{ opacity: 1 }}
        animate={{ opacity: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
      >
        <Snowflake weight="light" className="size-12 text-[#5b6b73]" />
        <span className="font-display text-2xl font-light tracking-tight text-[#39454b]">
          Tủ lạnh của bạn
        </span>
      </motion.div>
      {/* cánh trái */}
      <motion.div
        className="absolute left-0 top-0 h-full w-1/2"
        style={steel}
        initial={{ x: 0 }}
        animate={{ x: '-101%' }}
        transition={slide}
      >
        <span className="absolute inset-y-8 right-0 w-px bg-white/50" />
        <span className="absolute right-4 top-1/2 h-24 w-2 -translate-y-1/2 rounded-full bg-[#9aa7ad] shadow-sm" />
      </motion.div>
      {/* cánh phải */}
      <motion.div
        className="absolute right-0 top-0 h-full w-1/2"
        style={steel}
        initial={{ x: 0 }}
        animate={{ x: '101%' }}
        transition={slide}
      >
        <span className="absolute inset-y-8 left-0 w-px bg-black/10" />
        <span className="absolute left-4 top-1/2 h-24 w-2 -translate-y-1/2 rounded-full bg-[#9aa7ad] shadow-sm" />
      </motion.div>
    </motion.div>
  )
}
