import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { CaretLeft, Plus, Trash, Warning } from '@phosphor-icons/react'
import { Modal } from './Modal'
import { Field } from './Field'
import { Button } from './Button'
import { Spinner } from './Spinner'
import { uploadRecipeImage, useRecipeAdmin, type RecipeInput } from '../hooks/useRecipeAdmin'
import { cn } from '../lib/cn'
import type { RecipeSummary } from '../types'

const DIFFICULTIES = [
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Vừa' },
  { value: 'HARD', label: 'Khó' },
]

/** 4 bước ngắn thay vì 1 form dài — mỗi bước chỉ hỏi đúng nhóm dữ liệu của nó. */
const STEP_TITLES = ['Thông tin cơ bản', 'Ảnh công thức', 'Nguyên liệu', 'Các bước nấu']

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
 * Form tạo/sửa công thức cho khu quản trị, chia 4 bước ngắn (thay vì 1 form dài cuộn liên tục).
 * Chỉ gửi dữ liệu thô — mọi thứ suy ra được (normalizedName, weight, slug, nutrition) đều do
 * Recipe Service tự tính.
 */
export function AdminRecipeForm({ open, onClose, editing, onSaved }: Props) {
  const { saving, error, clearError, create, update, load } = useRecipeAdmin()
  const [loading, setLoading] = useState(false)
  const [step, setStep] = useState(0)
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
    setStep(0)
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

  const isLastStep = step === STEP_TITLES.length - 1

  /** Chặn đúng lỗi của bước hiện tại trước khi cho qua bước sau — đỡ để user đi hết 4 bước rồi mới báo lỗi ở cuối. */
  function next() {
    if (step === 0 && !name.trim()) {
      setFormError('Chưa nhập tên công thức.')
      return
    }
    if (step === 2 && !ingredients.some((i) => i.name.trim())) {
      setFormError('Cần ít nhất một nguyên liệu có tên.')
      return
    }
    setFormError(null)
    setStep((s) => Math.min(STEP_TITLES.length - 1, s + 1))
  }

  function back() {
    setFormError(null)
    setStep((s) => Math.max(0, s - 1))
  }

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
          <StepIndicator step={step} />

          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={step}
              initial={{ opacity: 0, x: 16 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -16 }}
              transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
              className="flex flex-col gap-5"
            >
              {step === 0 && (
                <>
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
                </>
              )}

              {step === 1 && (
                <div className="flex flex-col gap-2">
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
              )}

              {step === 2 && (
                <RepeatableSection
                  onAdd={() => setIngredients((r) => [...r, emptyIngredient()])}
                  rows={ingredients}
                  onRemove={(i) => setIngredients((r) => (r.length > 1 ? r.filter((_, x) => x !== i) : r))}
                  addLabel="Thêm nguyên liệu"
                  render={(row, i) => (
                    <div className="flex flex-col gap-2">
                      <input
                        aria-label={`Tên nguyên liệu ${i + 1}`}
                        placeholder="Cà chua"
                        value={row.name}
                        onChange={(e) =>
                          setIngredients((r) =>
                            r.map((x, y) => (y === i ? { ...x, name: e.target.value } : x)),
                          )
                        }
                        className="h-10 w-full min-w-0 rounded-lg border border-line bg-surface px-3 text-sm outline-none focus-visible:border-accent"
                      />
                      {/* Đơn vị dữ liệu thật đôi khi dài (VD "finely sliced" từ nguồn nhập cũ) —
                          để riêng dòng, cho unit chiếm phần còn lại thay vì cột cố định hẹp dễ tràn. */}
                      <div className="grid grid-cols-[4.5rem_minmax(0,1fr)] gap-2">
                        <input
                          aria-label={`Lượng nguyên liệu ${i + 1}`}
                          placeholder="2"
                          value={row.amount}
                          onChange={(e) =>
                            setIngredients((r) =>
                              r.map((x, y) => (y === i ? { ...x, amount: e.target.value } : x)),
                            )
                          }
                          className="h-10 min-w-0 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
                        />
                        <input
                          aria-label={`Đơn vị nguyên liệu ${i + 1}`}
                          placeholder="quả"
                          value={row.unit}
                          onChange={(e) =>
                            setIngredients((r) =>
                              r.map((x, y) => (y === i ? { ...x, unit: e.target.value } : x)),
                            )
                          }
                          className="h-10 min-w-0 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
                        />
                      </div>
                      <label className="flex items-center gap-2 text-[12px] text-muted">
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
              )}

              {step === 3 && (
                <RepeatableSection
                  onAdd={() => setSteps((r) => [...r, emptyStep()])}
                  rows={steps}
                  onRemove={(i) => setSteps((r) => (r.length > 1 ? r.filter((_, x) => x !== i) : r))}
                  addLabel="Thêm bước"
                  render={(row, i) => (
                    <div className="flex flex-col gap-2">
                      <div className="grid grid-cols-[minmax(0,1fr)_5.5rem] gap-2">
                        <input
                          aria-label={`Tiêu đề bước ${i + 1}`}
                          placeholder={`Bước ${i + 1} — Sơ chế`}
                          value={row.title}
                          onChange={(e) =>
                            setSteps((r) => r.map((x, y) => (y === i ? { ...x, title: e.target.value } : x)))
                          }
                          className="h-10 min-w-0 rounded-lg border border-line bg-surface px-3 text-sm outline-none focus-visible:border-accent"
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
                          className="h-10 min-w-0 rounded-lg border border-line bg-surface px-2 text-sm outline-none focus-visible:border-accent"
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
              )}
            </motion.div>
          </AnimatePresence>

          {(formError || error) && (
            <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
              <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
              <span>{formError ?? error}</span>
            </div>
          )}

          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={busy}
              className="text-[13px] font-medium text-faint transition-colors hover:text-muted disabled:opacity-50"
            >
              Huỷ
            </button>
            <div className="flex gap-2">
              {step > 0 && (
                <Button type="button" variant="secondary" onClick={back} disabled={busy}>
                  <CaretLeft weight="bold" className="size-4" />
                </Button>
              )}
              {isLastStep ? (
                <Button type="submit" disabled={busy}>
                  {saving ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo công thức'}
                </Button>
              ) : (
                <Button type="button" onClick={next} disabled={busy}>
                  Tiếp tục
                </Button>
              )}
            </div>
          </div>
        </form>
      )}
    </Modal>
  )
}

/** Chấm tiến trình + tên bước — cùng ngôn ngữ chuyển động với wizard Onboarding. */
function StepIndicator({ step }: { step: number }) {
  return (
    <div>
      <div className="mb-3 flex justify-center gap-2">
        {STEP_TITLES.map((title, i) => (
          <motion.span
            key={title}
            layout
            animate={{ width: i === step ? 24 : 6 }}
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
            className={cn('h-1.5 rounded-full transition-colors', i <= step ? 'bg-accent' : 'bg-line-soft')}
          />
        ))}
      </div>
      <p className="text-center text-[11px] font-semibold uppercase tracking-[0.14em] text-faint">
        Bước {step + 1}/{STEP_TITLES.length} · {STEP_TITLES[step]}
      </p>
    </div>
  )
}

/** Khối hàng lặp lại (nguyên liệu / các bước) — thêm, xoá, và vào/ra có chuyển động. */
function RepeatableSection<T>({
  rows,
  onAdd,
  onRemove,
  addLabel,
  render,
}: {
  rows: T[]
  onAdd: () => void
  onRemove: (index: number) => void
  addLabel: string
  render: (row: T, index: number) => ReactNode
}) {
  return (
    <div className="flex flex-col gap-3">
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
      <button
        type="button"
        onClick={onAdd}
        className="flex items-center justify-center gap-1.5 self-start rounded-full border border-line px-3 py-1.5 text-[12px] font-medium text-muted transition-colors hover:border-accent hover:text-accent"
      >
        <Plus weight="bold" className="size-3.5" />
        {addLabel}
      </button>
    </div>
  )
}
