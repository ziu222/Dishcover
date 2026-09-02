import { useState, type FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Flame,
  ForkKnife,
  PencilSimple,
  Plus,
  ShieldWarning,
  SignOut,
  Warning,
  X,
} from '@phosphor-icons/react'
import { useAuth } from '../auth/AuthContext'
import { useCalorieGoal } from '../hooks/useCalorieGoal'
import { useDietaryPreferences } from '../hooks/useDietaryPreferences'
import { Button } from '../components/Button'
import { Chip } from '../components/Chip'
import { Field } from '../components/Field'
import { Spinner } from '../components/Spinner'
import { ApiError } from '../lib/api'
import { cn } from '../lib/cn'
import type { CalorieGoal, DietaryType } from '../types'

const ALLERGY_PRESETS = ['Hải sản', 'Cá', 'Trứng', 'Sữa', 'Đậu phộng', 'Đậu nành', 'Mè', 'Gluten', 'Hạt']
const DIET_PRESETS = ['Chay', 'Thuần chay']

// ponytail: số tham khảo khởi điểm, không phải tính từ BMR/cân nặng thật — người dùng luôn sửa lại
// được ngay trên form. Không lưu nhãn preset đã chọn, chỉ lưu 4 con số cuối cùng (xem CalorieGoalDtos).
const GOAL_PRESETS: Record<string, CalorieGoal> = {
  BULK: { calorieTarget: 2500, proteinTarget: 150, carbTarget: 300, fatTarget: 70 },
  CUT: { calorieTarget: 1800, proteinTarget: 140, carbTarget: 150, fatTarget: 50 },
  MAINTAIN: { calorieTarget: 2200, proteinTarget: 110, carbTarget: 250, fatTarget: 70 },
}
const GOAL_PRESET_LABELS: Record<string, string> = {
  BULK: 'Tăng cơ',
  CUT: 'Giảm mỡ',
  MAINTAIN: 'Duy trì',
  CUSTOM: 'Tự nhập',
}

function AddPreferenceForm({
  onAdd,
}: {
  onAdd: (type: DietaryType, value: string) => Promise<void>
}) {
  const [type, setType] = useState<DietaryType>('ALLERGY')
  const [value, setValue] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const presets = type === 'ALLERGY' ? ALLERGY_PRESETS : DIET_PRESETS

  async function submit(e: FormEvent) {
    e.preventDefault()
    const trimmed = value.trim()
    if (!trimmed || saving) return
    setSaving(true)
    setError(null)
    try {
      await onAdd(type, trimmed)
      setValue('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Thêm thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="rounded-card border border-line-soft bg-white p-5">
      <div className="mb-3 flex gap-2">
        <Chip active={type === 'ALLERGY'} onClick={() => setType('ALLERGY')}>
          Dị ứng
        </Chip>
        <Chip active={type === 'DIET'} onClick={() => setType('DIET')}>
          Chế độ ăn
        </Chip>
      </div>

      <div className="mb-3 flex flex-wrap gap-1.5">
        {presets.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => setValue(p)}
            className={cn(
              'rounded-full border px-3 py-1.5 text-[12.5px] font-medium transition-colors',
              value === p
                ? 'border-accent bg-accent-wash text-accent-strong'
                : 'border-line-soft text-muted hover:border-mist',
            )}
          >
            {p}
          </button>
        ))}
      </div>

      <div className="flex gap-2">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder={type === 'ALLERGY' ? 'VD: hải sản' : 'VD: chay'}
          maxLength={50}
          className="w-full rounded-xl border border-line bg-card px-4 py-2.5 text-[15px] text-ink placeholder:text-mist outline-none focus:border-accent focus:ring-2 focus:ring-accent/15"
        />
        <Button type="submit" loading={saving} disabled={!value.trim()}>
          <Plus weight="bold" className="size-4" />
          Thêm
        </Button>
      </div>
      {error && <p className="mt-2 text-xs text-expired">{error}</p>}
    </form>
  )
}

function EditProfileForm({ onClose }: { onClose: () => void }) {
  const { user, updateProfile } = useAuth()
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await updateProfile({ fullName: fullName.trim(), avatarUrl: avatarUrl.trim() })
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Lưu thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4 rounded-card border border-line-soft bg-white p-6">
      <Field
        label="Họ tên"
        autoFocus
        placeholder="Nguyễn Minh"
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
      />
      <Field
        label="URL ảnh đại diện"
        placeholder="https://…"
        helperText="Để trống sẽ dùng chữ cái đầu của tên làm avatar"
        value={avatarUrl}
        onChange={(e) => setAvatarUrl(e.target.value)}
      />
      {error && <p className="text-xs text-expired">{error}</p>}
      <div className="mt-1 flex gap-3">
        <Button type="button" variant="secondary" fullWidth onClick={onClose}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth loading={saving}>
          Lưu
        </Button>
      </div>
    </form>
  )
}

const GOAL_FIELDS: Array<{ key: keyof CalorieGoal; label: string; unit: string }> = [
  { key: 'calorieTarget', label: 'Calo', unit: 'kcal' },
  { key: 'proteinTarget', label: 'Đạm', unit: 'g' },
  { key: 'carbTarget', label: 'Tinh bột', unit: 'g' },
  { key: 'fatTarget', label: 'Béo', unit: 'g' },
]

function CalorieGoalForm({
  goal,
  onSave,
}: {
  goal: CalorieGoal | null
  onSave: (goal: CalorieGoal) => Promise<void>
}) {
  const [values, setValues] = useState<CalorieGoal>(
    goal ?? { calorieTarget: 0, proteinTarget: 0, carbTarget: 0, fatTarget: 0 },
  )
  const [preset, setPreset] = useState('CUSTOM')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // goal tải xong SAU lần render đầu (fetch async) — đồng bộ lại form khi có, chỉ 1 lần lúc mới có
  // dữ liệu để không ghi đè chỉnh sửa đang gõ dở của người dùng ở lần render sau.
  const [synced, setSynced] = useState(false)
  if (goal && !synced) {
    setValues(goal)
    setSynced(true)
  }

  function applyPreset(key: string) {
    setPreset(key)
    if (GOAL_PRESETS[key]) setValues(GOAL_PRESETS[key])
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await onSave(values)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Lưu thất bại, thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="rounded-card border border-line-soft bg-white p-5">
      <div className="mb-4 flex flex-wrap gap-2">
        {Object.keys(GOAL_PRESET_LABELS).map((key) => (
          <Chip key={key} active={preset === key} onClick={() => applyPreset(key)}>
            {GOAL_PRESET_LABELS[key]}
          </Chip>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {GOAL_FIELDS.map(({ key, label, unit }) => (
          <label key={key} className="flex flex-col gap-1.5">
            <span className="text-[11px] font-semibold uppercase tracking-[0.12em] text-faint">
              {label}
            </span>
            <div className="flex items-center gap-1.5 rounded-xl border border-line bg-card px-3 py-2 focus-within:border-accent focus-within:ring-2 focus-within:ring-accent/15">
              <input
                type="number"
                min={0}
                value={values[key]}
                onChange={(e) => {
                  setPreset('CUSTOM')
                  setValues((v) => ({ ...v, [key]: Number(e.target.value) }))
                }}
                className="w-full min-w-0 bg-transparent text-[15px] text-ink outline-none"
              />
              <span className="shrink-0 text-[11px] text-mist">{unit}</span>
            </div>
          </label>
        ))}
      </div>

      {error && <p className="mt-3 text-xs text-expired">{error}</p>}
      <Button type="submit" className="mt-4" loading={saving}>
        Lưu mục tiêu
      </Button>
    </form>
  )
}

export function Account() {
  const { user, logout } = useAuth()
  const { items, loading, error, reload, add, remove } = useDietaryPreferences()
  const { goal, save: saveGoal } = useCalorieGoal()
  const [removingId, setRemovingId] = useState<number | null>(null)
  const [editingProfile, setEditingProfile] = useState(false)
  const initial = (user?.fullName || user?.email || '?').trim().charAt(0).toUpperCase()

  async function handleRemove(id: number) {
    setRemovingId(id)
    try {
      await remove(id)
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mb-8">
        <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
          Tài khoản
        </div>
        <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
          Hồ sơ của bạn
        </h1>
      </div>

      <div className="mx-auto flex max-w-2xl flex-col gap-8">
        {/* Hồ sơ */}
        {editingProfile ? (
          <EditProfileForm onClose={() => setEditingProfile(false)} />
        ) : (
          <div className="flex items-center justify-between gap-4 rounded-card border border-line-soft bg-white p-6">
            <div className="flex min-w-0 items-center gap-4">
              {user?.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt=""
                  className="size-14 shrink-0 rounded-full object-cover"
                />
              ) : (
                <span className="grid size-14 shrink-0 place-items-center rounded-full bg-accent text-xl font-semibold text-surface">
                  {initial}
                </span>
              )}
              <div className="min-w-0">
                <p className="truncate text-lg font-medium text-ink">
                  {user?.fullName || 'Chưa đặt tên'}
                </p>
                <p className="truncate text-sm text-muted">{user?.email}</p>
              </div>
            </div>
            <div className="flex shrink-0 gap-2">
              <button
                type="button"
                onClick={() => setEditingProfile(true)}
                aria-label="Sửa hồ sơ"
                className="grid size-10 place-items-center rounded-full text-mist transition-colors hover:bg-line-soft hover:text-accent"
              >
                <PencilSimple className="size-[18px]" />
              </button>
              <Button variant="secondary" onClick={() => void logout()}>
                <SignOut className="size-4" />
                Đăng xuất
              </Button>
            </div>
          </div>
        )}

        {/* Hồ sơ ăn uống */}
        <div>
          <div className="mb-1 flex items-center gap-2">
            <ForkKnife className="size-5 text-accent" />
            <h2 className="font-display text-2xl font-light text-ink">Hồ sơ ăn uống</h2>
          </div>
          <p className="mb-5 text-sm text-muted">
            Dùng để lọc công thức và gợi ý không phù hợp với bạn.
          </p>

          {loading ? (
            <Spinner label="Đang tải hồ sơ ăn uống…" />
          ) : error ? (
            <div className="py-8 text-center">
              <p className="text-[15px] text-muted">{error}</p>
              <Button variant="secondary" className="mt-4" onClick={reload}>
                Thử lại
              </Button>
            </div>
          ) : (
            <>
              {items.length === 0 ? (
                <p className="mb-5 text-sm text-faint">Chưa có thông tin dị ứng hoặc chế độ ăn nào.</p>
              ) : (
                <motion.div
                  className="mb-5 flex flex-wrap gap-2"
                  initial="hidden"
                  animate="show"
                  variants={{ show: { transition: { staggerChildren: 0.04 } } }}
                >
                  <AnimatePresence mode="popLayout">
                    {items.map((item) => (
                      <motion.span
                        key={item.id}
                        layout
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.9 }}
                        transition={{ type: 'spring', stiffness: 300, damping: 24 }}
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-[13px] font-medium',
                          item.type === 'ALLERGY'
                            ? 'bg-expired-bg text-expired'
                            : 'bg-fresh-bg text-fresh',
                        )}
                      >
                        {item.type === 'ALLERGY' && <Warning weight="fill" className="size-3.5" />}
                        {item.value}
                        <button
                          type="button"
                          onClick={() => void handleRemove(item.id)}
                          disabled={removingId === item.id}
                          aria-label={`Xoá ${item.value}`}
                          className="ml-0.5 rounded-full opacity-60 transition-opacity hover:opacity-100 disabled:opacity-30"
                        >
                          <X weight="bold" className="size-3" />
                        </button>
                      </motion.span>
                    ))}
                  </AnimatePresence>
                </motion.div>
              )}

              <AddPreferenceForm onAdd={add} />
            </>
          )}
        </div>

        <div className="flex items-start gap-2.5 rounded-card border border-dashed border-line px-4 py-3.5 text-[13px] leading-relaxed text-faint">
          <ShieldWarning className="mt-0.5 size-4 shrink-0" />
          Thông tin dị ứng chỉ dùng để lọc gợi ý — luôn kiểm tra kỹ nguyên liệu trước khi nấu.
        </div>

        {/* Mục tiêu calo */}
        <div>
          <div className="mb-1 flex items-center gap-2">
            <Flame className="size-5 text-accent" />
            <h2 className="font-display text-2xl font-light text-ink">Mục tiêu calo/ngày</h2>
          </div>
          <p className="mb-5 text-sm text-muted">
            Dùng để ưu tiên gợi ý công thức gần mục tiêu của bạn — chọn mẫu tham khảo rồi sửa lại
            cho đúng nhu cầu thật.
          </p>
          <CalorieGoalForm goal={goal} onSave={saveGoal} />
        </div>
      </div>
    </div>
  )
}
