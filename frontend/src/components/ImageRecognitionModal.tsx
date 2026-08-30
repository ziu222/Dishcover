import { useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Camera, CheckCircle, Image as ImageIcon, Warning } from '@phosphor-icons/react'
import { Modal } from './Modal'
import { Button } from './Button'
import { useImageRecognition } from '../hooks/useImageRecognition'
import { ingredientIcon } from '../lib/ingredientIcon'
import { ApiError } from '../lib/api'
import { cn } from '../lib/cn'
import type { ItemInput } from '../hooks/useInventory'
import type { RecognizedIngredient } from '../types'

const LOW_CONFIDENCE = 0.6

interface Row {
  included: boolean
  name: string
  normalizedName: string
  quantity: string
  unit: string
  expiryDate: string
  confidence: number
  quantityGuess: string | null
}

function toRows(items: RecognizedIngredient[]): Row[] {
  return items.map((i) => ({
    included: true,
    name: i.name,
    normalizedName: i.normalizedName,
    quantity: '',
    unit: '',
    expiryDate: i.suggestedExpiryDate ?? '',
    confidence: i.confidence,
    quantityGuess: i.quantityGuess,
  }))
}

/** Góc viewfinder — thuần trang trí, báo hiệu "đang quét". */
function ViewfinderCorner({ className }: { className: string }) {
  return (
    <motion.span
      aria-hidden
      className={cn('absolute size-6 border-white/80', className)}
      animate={{ opacity: [0.5, 1, 0.5] }}
      transition={{ duration: 1.8, repeat: Infinity, ease: 'easeInOut' }}
    />
  )
}

/** Ảnh xem trước + hiệu ứng quét khi đang gọi Vision API — 3 lớp chuyển động (khung góc + vệt
 *  quét + chữ nhấp nháy) theo skill motion-design, không dùng Lottie để khỏi thêm dependency mới
 *  cho 1 hiệu ứng đơn giản, nhất quán với phần còn lại của app (toàn framer-motion). */
function ScanningPreview({ src, scanning }: { src: string; scanning: boolean }) {
  return (
    <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl bg-line-soft">
      <img src={src} alt="Ảnh nguyên liệu" className="size-full object-cover" />
      {scanning && (
        <>
          <div className="absolute inset-0 bg-ink/25" />
          <ViewfinderCorner className="left-3 top-3 border-b-0 border-r-0 rounded-tl-lg" />
          <ViewfinderCorner className="right-3 top-3 border-b-0 border-l-0 rounded-tr-lg" />
          <ViewfinderCorner className="bottom-3 left-3 border-r-0 border-t-0 rounded-bl-lg" />
          <ViewfinderCorner className="bottom-3 right-3 border-l-0 border-t-0 rounded-br-lg" />
          <motion.div
            aria-hidden
            className="absolute inset-x-0 h-14 bg-gradient-to-b from-transparent via-accent/50 to-transparent"
            initial={{ top: '-10%' }}
            animate={{ top: ['-10%', '100%'] }}
            transition={{ duration: 1.7, repeat: Infinity, ease: 'easeInOut' }}
          />
          <motion.p
            className="absolute bottom-4 left-1/2 -translate-x-1/2 text-[13px] font-medium text-white"
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 1.4, repeat: Infinity, ease: 'easeInOut' }}
          >
            Đang nhận diện nguyên liệu…
          </motion.p>
        </>
      )}
    </div>
  )
}

function ConfirmRow({ row, onChange }: { row: Row; onChange: (next: Row) => void }) {
  const low = row.confidence < LOW_CONFIDENCE
  const Icon = ingredientIcon(row.normalizedName)
  return (
    <motion.li
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'rounded-xl border p-3.5',
        row.included ? 'border-line-soft bg-white' : 'border-line-soft bg-card opacity-60',
      )}
    >
      <div className="flex items-start gap-3">
        <button
          type="button"
          role="checkbox"
          aria-checked={row.included}
          aria-label={row.included ? `Bỏ ${row.name}` : `Thêm ${row.name}`}
          onClick={() => onChange({ ...row, included: !row.included })}
          className={cn(
            'mt-0.5 grid size-6 shrink-0 place-items-center rounded-full border transition-colors',
            row.included ? 'border-accent bg-accent text-surface' : 'border-line text-transparent',
          )}
        >
          <CheckCircle weight="fill" className="size-4" />
        </button>

        <span className="mt-0.5 grid size-8 shrink-0 place-items-center rounded-full bg-card text-muted" aria-hidden>
          <Icon weight="duotone" className="size-4" />
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <input
              value={row.name}
              onChange={(e) => onChange({ ...row, name: e.target.value })}
              disabled={!row.included}
              className="w-full min-w-0 rounded-lg border border-line bg-card px-2.5 py-1.5 text-[14px] font-medium text-ink outline-none focus:border-accent disabled:opacity-50"
            />
            {low && (
              <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-amber-bg px-2 py-0.5 text-[10.5px] font-semibold text-amber">
                <Warning weight="fill" className="size-3" />
                Chưa chắc
              </span>
            )}
          </div>

          <div className="mt-2 grid grid-cols-2 gap-2">
            <input
              value={row.quantity}
              onChange={(e) => onChange({ ...row, quantity: e.target.value })}
              disabled={!row.included}
              type="number"
              min={0}
              step="any"
              placeholder={row.quantityGuess ?? 'Số lượng'}
              className="w-full rounded-lg border border-line bg-card px-2.5 py-1.5 text-[13px] text-ink outline-none placeholder:text-faint focus:border-accent disabled:opacity-50"
            />
            <input
              value={row.unit}
              onChange={(e) => onChange({ ...row, unit: e.target.value })}
              disabled={!row.included}
              placeholder="Đơn vị"
              className="w-full rounded-lg border border-line bg-card px-2.5 py-1.5 text-[13px] text-ink outline-none placeholder:text-faint focus:border-accent disabled:opacity-50"
            />
          </div>
          <input
            value={row.expiryDate}
            onChange={(e) => onChange({ ...row, expiryDate: e.target.value })}
            disabled={!row.included}
            type="date"
            className="mt-2 w-full rounded-lg border border-line bg-card px-2.5 py-1.5 text-[13px] text-ink outline-none focus:border-accent disabled:opacity-50"
          />
        </div>
      </div>
    </motion.li>
  )
}

export function ImageRecognitionModal({
  open,
  onClose,
  onConfirm,
}: {
  open: boolean
  onClose: () => void
  onConfirm: (items: ItemInput[]) => Promise<void>
}) {
  const { loading, error, items, recognize, reset } = useImageRecognition()
  const [preview, setPreview] = useState<string | null>(null)
  const [rows, setRows] = useState<Row[] | null>(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function handleFile(file: File) {
    setPreview(URL.createObjectURL(file))
    setRows(null)
    setSaveError(null)
    void recognize(file)
  }

  function handleClose() {
    reset()
    setPreview(null)
    setRows(null)
    setSaveError(null)
    onClose()
  }

  // items vừa về (mảng mới, khác null cũ) -> khởi tạo bảng xác nhận đúng 1 lần.
  if (items && !rows) {
    setRows(toRows(items))
  }

  function updateRow(idx: number, next: Row) {
    setRows((prev) => prev?.map((r, i) => (i === idx ? next : r)) ?? null)
  }

  async function confirm() {
    if (!rows) return
    const chosen = rows.filter((r) => r.included && r.name.trim())
    if (chosen.length === 0) return
    setSaving(true)
    setSaveError(null)
    try {
      await onConfirm(
        chosen.map((r) => ({
          ingredientName: r.name.trim(),
          quantity: r.quantity ? Number(r.quantity) : undefined,
          unit: r.unit.trim() || undefined,
          expiryDate: r.expiryDate || undefined,
        })),
      )
      handleClose()
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : 'Thêm thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  const title = rows ? 'Xác nhận nguyên liệu' : 'Nhận diện từ ảnh'
  const checkedCount = rows?.filter((r) => r.included).length ?? 0

  return (
    <Modal open={open} onClose={handleClose} title={title}>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) handleFile(file)
        }}
      />

      {!preview ? (
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="flex w-full flex-col items-center gap-3 rounded-2xl border-2 border-dashed border-line py-12 text-center transition-colors hover:border-accent/50"
        >
          <span className="grid size-14 place-items-center rounded-full bg-accent-wash">
            <Camera weight="fill" className="size-6 text-accent" />
          </span>
          <span className="text-[15px] font-medium text-ink">Chụp hoặc chọn ảnh</span>
          <span className="text-[13px] text-muted">Hệ thống sẽ đề xuất nguyên liệu, bạn xác nhận trước khi lưu</span>
        </button>
      ) : (
        <div className="flex flex-col gap-4">
          <ScanningPreview src={preview} scanning={loading} />

          {error && (
            <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
              <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {!loading && rows && rows.length === 0 && (
            <div className="py-6 text-center">
              <ImageIcon className="mx-auto mb-3 size-8 text-mist" />
              <p className="text-[15px] text-ink">Không nhận diện được nguyên liệu nào trong ảnh.</p>
              <p className="mt-1 text-[13px] text-muted">Thử ảnh khác hoặc nhập tay.</p>
            </div>
          )}

          {!loading && rows && rows.length > 0 && (
            <>
              <ul className="flex max-h-72 flex-col gap-2 overflow-y-auto pr-1">
                <AnimatePresence initial={false}>
                  {rows.map((row, idx) => (
                    <ConfirmRow key={idx} row={row} onChange={(next) => updateRow(idx, next)} />
                  ))}
                </AnimatePresence>
              </ul>

              {saveError && (
                <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
                  <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
                  <span>{saveError}</span>
                </div>
              )}
            </>
          )}

          <div className="flex gap-3">
            <Button type="button" variant="secondary" fullWidth onClick={handleClose}>
              Huỷ
            </Button>
            {(error || (rows && rows.length === 0)) && !loading ? (
              <Button type="button" fullWidth onClick={() => fileInputRef.current?.click()}>
                Chọn ảnh khác
              </Button>
            ) : (
              <Button
                type="button"
                fullWidth
                loading={saving}
                disabled={loading || !rows || checkedCount === 0}
                onClick={confirm}
              >
                Xác nhận thêm{rows ? ` (${checkedCount})` : ''}
              </Button>
            )}
          </div>
        </div>
      )}
    </Modal>
  )
}
