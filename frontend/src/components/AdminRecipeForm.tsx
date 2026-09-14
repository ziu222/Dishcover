import { useEffect, useState, type FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Plus, Trash, Warning } from '@phosphor-icons/react'
import { Modal } from './Modal'
import { Field } from './Field'
import { Button } from './Button'
import { Spinner } from './Spinner'
import { uploadRecipeImage, useRecipeAdmin, type RecipeInput } from '../hooks/useRecipeAdmin'
import type { RecipeSummary } from '../types'

const DIFFICULTIES = [
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Vừa' },
  { value: 'HARD', label: 'Khó' },
]

interface IngredientRow {
  name: string
  amount: string
  unit: string
  essential: boolean
}
interface StepRow {
  title: string
  content: string
  durationMinutes: string
}

const emptyIngredient = (): IngredientRow => ({ name: '', amount: '', unit: '', essential: true })
const emptyStep = (): StepRow => ({ title: '', content: '', durationMinutes: '' })

/** '' -> null, còn lại parse số; tránh gửi NaN xuống backend. */
function num(v: string): number | null {
  const t = v.trim()
  if (!t) return null
  const n = Number(t)
  return Number.isFinite(n) ? n : null
}

interface Props {
  open: boolean
  onClose: () => void
  /** null = tạo mới; có giá trị = sửa công thức đó. */
  editing: RecipeSummary | null
  onSaved: () => void
}

/**
 * Form tạo/sửa công thức cho khu quản trị. Chỉ gửi dữ liệu thô — mọi thứ suy ra được
 * (normalizedName, weight, slug, nutrition) đều do Recipe Service tự tính.
 */
export function AdminRecipeForm({ open, onClose, editing, onSaved }: Props) {
  const { saving, error, clearError, create, update, load } = useRecipeAdmin()
  const [loading, setLoading] = useState(false)
  const [name, setName] = useState('')
  const [cookTime, setCookTime] = useState('')
  const [difficulty, setDifficulty] = useState('EASY')
  const [servings, setServings] = useState('')
  const [tags, setTags] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [ingredients, setIngredients] = useState<IngredientRow[]>([emptyIngredient()])
  const [steps, setSteps] = useState<StepRow[]>([emptyStep()])
  const [formError, setFormError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  // Nạp chi tiết khi mở form sửa — danh sách chỉ có summary, thiếu nguyên liệu và các bước.
  useEffect(() => {
    if (!open) return
    clearError()
    setFormError(null)
    if (!editing) {
      setName('')
      setCookTime('')
      setDifficulty('EASY')
      setServings('')
      setTags('')
      setImageUrl('')
      setIngredients([emptyIngredient()])
      setSteps([emptyStep()])
      return
    }
    let cancelled = false
    setLoading(true)
    load(editing.id)
      .then((r) => {
        if (cancelled || !r) return
        setName(r.name)
        setCookTime(String(r.cookTimeMinutes))
        setDifficulty(r.difficulty)
        setServings(r.servings != null ? String(r.servings) : '')
        setTags(r.tags.join(', '))
        setImageUrl(r.imageUrl ?? '')
        setIngredients(
          r.ingredients.length
            ? r.ingredients.map((i) => ({
                name: i.name,
                amount: i.amount != null ? String(i.amount) : '',
                unit: i.unit ?? '',
                essential: i.essential,
              }))
            : [emptyIngredient()],
        )
        setSteps(
          r.steps.length
            ? r.steps.map((s) => ({
                title: s.title,
                content: s.content,
                durationMinutes: s.durationMinutes ? String(s.durationMinutes) : '',
              }))
            : [emptyStep()],
        )
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing])

  async function submit(e: FormEvent) {
    e.preventDefault()
    const cleanIngredients = ingredients.filter((i) => i.name.trim())
    const cleanSteps = steps.filter((s) => s.title.trim() && s.content.trim())
    if (!name.trim()) return setFormError('Chưa nhập tên công thức.')
    if (!cleanIngredients.length) return setFormError('Cần ít nhất một nguyên liệu có tên.')
    if (!cleanSteps.length) return setFormError('Cần ít nhất một bước nấu có tiêu đề và nội dung.')
    setFormError(null)

    const input: RecipeInput = {
      name: name.trim(),
      cookTimeMinutes: num(cookTime) ?? 0,
      difficulty,
      tags: tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
      servings: num(servings),
      imageUrl: imageUrl.trim() || null,
      videoUrl: null,
      ingredients: cleanIngredients.map((i) => ({
        name: i.name.trim(),
        amount: num(i.amount),
        unit: i.unit.trim() || null,
        essential: i.essential,
      })),
      // order do form quyết định theo vị trí hiển thị — kéo thả sau này chỉ cần đổi mảng.
      steps: cleanSteps.map((s, idx) => ({
        order: idx + 1,
        title: s.title.trim(),
        content: s.content.trim(),
        durationMinutes: num(s.durationMinutes),
      })),
    }

    const saved = editing ? await update(editing.id, input) : await create(input)
    if (saved) {
      onSaved()
      onClose()
    }
  }

  /**
   * Upload cần id công thức nên chỉ bật khi đang sửa. Ảnh ghi thẳng lên S3 và Recipe Service
   * gán luôn imageUrl, nên sau khi upload xong form chỉ việc nhận URL mới.
   */
  async function pickImage(file: File | undefined) {
    if (!file || !editing) return
    setUploading(true)
    setFormError(null)
    try {
      setImageUrl(await uploadRecipeImage(editing.id, file))
      onSaved()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Không tải được ảnh lên.')
    } finally {
      setUploading(false)
    }
  }

  const busy = saving || loading || uploading

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Sửa công thức' : 'Thêm công thức'}>
      {loading ? (
        <Spinner label="Đang tải công thức…" />
      ) : (
        <form onSubmit={submit} className="flex flex-col gap-5">
          <Field label="Tên công thức" value={name} onChange={(e) => setName(e.target.value)} />

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <Field
              label="Thời gian (phút)"
              type="number"
              min={0}
              value={cookTime}
              onChange={(e) => setCookTime(e.target.value)}
            />
            <Field
              label="Khẩu phần"
              type="number"
              min={1}
              value={servings}
              onChange={(e) => setServings(e.target.value)}
            />
            <div className="flex flex-col gap-2">
              <label htmlFor="difficulty" className="text-[13px] font-medium text-muted">
                Độ khó
              </label>
              <select
                id="difficulty"
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                className="h-11 rounded-xl border border-line bg-surface px-3 text-sm text-ink outline-none focus-visible:border-accent"
              >
                {DIFFICULTIES.map((d) => (
                  <option key={d.value} value={d.value}>
                    {d.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <Field
            label="Tags"
            helperText="Ngăn cách bằng dấu phẩy, ví dụ: nhanh, bữa sáng, ít dầu mỡ"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
          />
          <div className="flex flex-col gap-2">
            <span className="text-[13px] font-medium text-muted">Ảnh công thức</span>
            {imageUrl && (
              <img
                src={imageUrl}
                alt="Ảnh công thức đang chọn"
                className="h-36 w-full rounded-xl border border-line object-cover"
              />
            )}
            <div className="flex flex-wrap items-center gap-3">
              <label className="cursor-pointer rounded-full border border-line px-4 py-2 text-[12px] font-medium text-muted transition-colors hover:border-accent hover:text-accent">
                {uploading ? 'Đang tải ảnh…' : imageUrl ? 'Đổi ảnh' : 'Chọn ảnh'}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="hidden"
                  disabled={uploading || !editing}
                  onChange={(e) => void pickImage(e.target.files?.[0])}
                />
              </label>
              {!editing && (
                <span className="text-[11px] text-faint">
                  Tạo công thức trước, rồi mới tải ảnh lên được.
                </span>
              )}
            </div>
            <Field
              label="Hoặc dán địa chỉ ảnh"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
            />
          </div>

          <RepeatableSection
            title="Nguyên liệu"
            onAdd={() => setIngredients((r) => [...r, emptyIngredient()])}
            rows={ingredients}
            onRemove={(i) => setIngredients((r) => (r.length > 1 ? r.filter((_, x) => x !== i) : r))}
            render={(row, i) => (
              <div className="grid grid-cols-[1fr_4.5rem_4.5rem] gap-2">
                <input
                  aria-label={`Tên nguyên liệu ${i + 1}`}
                  placeholder="Cà chua"
                  value={row.name}
                  onChange={(e) =>
                    setIngredients((r) => r.map((x, y) => (y === i ? { ...x, name: e.target.value } : x)))
                  }
                  className="h-10 rounded-lg border border-line bg-surface px-3 text-sm outline-none focus-visible:border-accent"
                />
                <input
                  aria-label={`Lượng nguyên liệu ${i + 1}`}
                  placeholder="2"
                  value={row.amount}
                  onChange={(e) =>
                    setIngredients((r) => r.map((x, y) => (y === i ? { ...x, amount: e.target.value } : x)))
                  }
                  className="h-10 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
                />
                <input
                  aria-label={`Đơn vị nguyên liệu ${i + 1}`}
                  placeholder="quả"
                  value={row.unit}
                  onChange={(e) =>
                    setIngredients((r) => r.map((x, y) => (y === i ? { ...x, unit: e.target.value } : x)))
                  }
                  className="h-10 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
                />
                <label className="col-span-3 flex items-center gap-2 text-[12px] text-muted">
                  <input
                    type="checkbox"
                    checked={row.essential}
                    onChange={(e) =>
                      setIngredients((r) =>
                        r.map((x, y) => (y === i ? { ...x, essential: e.target.checked } : x)),
                      )
                    }
                    className="size-4 accent-[var(--color-accent)]"
                  />
                  Nguyên liệu chính (thiếu là không nấu được)
                </label>
              </div>
            )}
          />

          <RepeatableSection
            title="Các bước nấu"
            onAdd={() => setSteps((r) => [...r, emptyStep()])}
            rows={steps}
            onRemove={(i) => setSteps((r) => (r.length > 1 ? r.filter((_, x) => x !== i) : r))}
            render={(row, i) => (
              <div className="flex flex-col gap-2">
                <div className="grid grid-cols-[1fr_5.5rem] gap-2">
                  <input
                    aria-label={`Tiêu đề bước ${i + 1}`}
                    placeholder={`Bước ${i + 1} — Sơ chế`}
                    value={row.title}
                    onChange={(e) =>
                      setSteps((r) => r.map((x, y) => (y === i ? { ...x, title: e.target.value } : x)))
                    }
                    className="h-10 rounded-lg border border-line bg-surface px-3 text-sm outline-none focus-visible:border-accent"
                  />
                  <input
                    aria-label={`Thời lượng bước ${i + 1} (phút)`}
                    placeholder="5 phút"
                    value={row.durationMinutes}
                    onChange={(e) =>
                      setSteps((r) =>
                        r.map((x, y) => (y === i ? { ...x, durationMinutes: e.target.value } : x)),
                      )
                    }
                    className="h-10 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
                  />
                </div>
                <textarea
                  aria-label={`Nội dung bước ${i + 1}`}
                  placeholder="Mô tả việc cần làm ở bước này."
                  rows={2}
                  value={row.content}
                  onChange={(e) =>
                    setSteps((r) => r.map((x, y) => (y === i ? { ...x, content: e.target.value } : x)))
                  }
                  className="rounded-lg border border-line bg-surface p-3 text-sm outline-none focus-visible:border-accent"
                />
              </div>
            )}
          />

          {(formError || error) && (
            <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
              <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
              <span>{formError ?? error}</span>
            </div>
          )}

          <div className="flex gap-3">
            <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
              Huỷ
            </Button>
            <Button type="submit" disabled={busy}>
              {saving ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo công thức'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}

/** Khối hàng lặp lại (nguyên liệu / các bước) — thêm, xoá, và vào/ra có chuyển động. */
function RepeatableSection<T>({
  title,
  rows,
  onAdd,
  onRemove,
  render,
}: {
  title: string
  rows: T[]
  onAdd: () => void
  onRemove: (index: number) => void
  render: (row: T, index: number) => React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <h3 className="font-display text-lg font-normal text-ink">{title}</h3>
        <button
          type="button"
          onClick={onAdd}
          className="flex items-center gap-1.5 rounded-full border border-line px-3 py-1.5 text-[12px] font-medium text-muted transition-colors hover:border-accent hover:text-accent"
        >
          <Plus weight="bold" className="size-3.5" />
          Thêm
        </button>
      </div>
      <ul className="flex flex-col gap-3">
        <AnimatePresence initial={false}>
          {rows.map((row, i) => (
            <motion.li
              key={i}
              layout
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, height: 0, transition: { duration: 0.2 } }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="flex items-start gap-2 rounded-xl border border-line-soft bg-card p-3"
            >
              <div className="min-w-0 flex-1">{render(row, i)}</div>
              <button
                type="button"
                onClick={() => onRemove(i)}
                aria-label={`Xoá dòng ${i + 1}`}
                disabled={rows.length === 1}
                className="grid size-9 shrink-0 place-items-center rounded-full text-mist transition-colors hover:bg-expired-bg hover:text-expired disabled:cursor-not-allowed disabled:opacity-40"
              >
                <Trash className="size-4" />
              </button>
            </motion.li>
          ))}
        </AnimatePresence>
      </ul>
    </div>
  )
}
