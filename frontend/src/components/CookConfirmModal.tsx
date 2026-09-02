import { useEffect, useState } from 'react'
import { CheckCircle } from '@phosphor-icons/react'
import { Modal } from './Modal'
import { Button } from './Button'
import { Spinner } from './Spinner'
import { useRecipeAvailability } from '../hooks/useAvailability'
import { cookDeduct, type CookDeductLine } from '../hooks/useInventory'
import { ApiError } from '../lib/api'

interface EditableLine extends CookDeductLine {
  name: string
}

/**
 * Màn xác nhận "Đã nấu xong" — tự điền số lượng sắp trừ từ availability endpoint (theo đúng lượng
 * công thức cần), người dùng sửa lại theo thực tế đã dùng trước khi xác nhận (human-in-the-loop,
 * cùng nguyên tắc với Image Recognition — CLAUDE.md phần nguyên tắc chung). Nguyên liệu không quy
 * đổi được số lượng (unit lạ) bị bỏ qua khỏi danh sách trừ, không đoán bừa.
 */
export function CookConfirmModal({
  open,
  onClose,
  recipeId,
}: {
  open: boolean
  onClose: () => void
  recipeId: string
}) {
  const { availability, loading, error } = useRecipeAvailability(open ? recipeId : undefined)
  const [lines, setLines] = useState<EditableLine[]>([])
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!availability) return
    setLines(
      availability.ingredients
        .filter((i) => i.neededAmount !== null && i.neededUnit !== null)
        .map((i) => ({
          normalizedName: i.normalizedName,
          name: i.name,
          amount: i.neededAmount as number,
          unit: i.neededUnit as string,
        })),
    )
    setDone(false)
    setSaveError(null)
  }, [availability])

  async function confirm() {
    setSaving(true)
    setSaveError(null)
    try {
      await cookDeduct(lines.filter((l) => l.amount > 0))
      setDone(true)
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : 'Trừ kho thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Đã nấu xong?">
      {loading ? (
        <Spinner label="Đang tính lượng nguyên liệu…" />
      ) : error ? (
        <p className="py-6 text-center text-[15px] text-muted">{error}</p>
      ) : done ? (
        <div className="py-6 text-center">
          <CheckCircle weight="fill" className="mx-auto mb-3 size-10 text-fresh" />
          <p className="text-[15px] text-ink">Đã cập nhật tủ lạnh.</p>
          <Button className="mt-5" fullWidth onClick={onClose}>
            Xong
          </Button>
        </div>
      ) : lines.length === 0 ? (
        <div className="py-6 text-center">
          <p className="text-[15px] text-muted">
            Không có nguyên liệu nào tự trừ được số lượng — bạn có thể tự cập nhật tủ lạnh nếu cần.
          </p>
          <Button variant="secondary" className="mt-5" fullWidth onClick={onClose}>
            Đóng
          </Button>
        </div>
      ) : (
        <>
          <p className="mb-4 text-sm text-muted">
            Sửa lại số lượng thực tế đã dùng nếu khác công thức — chỉ áp dụng cho nguyên liệu đang
            có trong tủ lạnh.
          </p>
          <ul className="mb-5 flex flex-col gap-2.5">
            {lines.map((line, idx) => (
              <li key={line.normalizedName} className="flex items-center gap-3">
                <span className="min-w-0 flex-1 truncate text-[15px] text-ink">{line.name}</span>
                <input
                  type="number"
                  min={0}
                  step="0.1"
                  value={line.amount}
                  onChange={(e) => {
                    const amount = Number(e.target.value)
                    setLines((ls) => ls.map((l, i) => (i === idx ? { ...l, amount } : l)))
                  }}
                  className="w-20 shrink-0 rounded-xl border border-line bg-card px-3 py-2 text-right text-[15px] tabular-nums text-ink outline-none focus:border-accent focus:ring-2 focus:ring-accent/15"
                />
                <span className="w-12 shrink-0 text-[13px] text-faint">{line.unit}</span>
              </li>
            ))}
          </ul>
          {saveError && <p className="mb-3 text-xs text-expired">{saveError}</p>}
          <div className="flex gap-3">
            <Button type="button" variant="secondary" fullWidth onClick={onClose}>
              Huỷ
            </Button>
            <Button type="button" fullWidth loading={saving} onClick={() => void confirm()}>
              Xác nhận
            </Button>
          </div>
        </>
      )}
    </Modal>
  )
}
