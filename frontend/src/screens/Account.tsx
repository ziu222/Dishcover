import { useState, type FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ForkKnife, Plus, ShieldWarning, SignOut, Warning, X } from '@phosphor-icons/react'
import { useAuth } from '../auth/AuthContext'
import { useDietaryPreferences } from '../hooks/useDietaryPreferences'
import { Button } from '../components/Button'
import { Chip } from '../components/Chip'
import { Spinner } from '../components/Spinner'
import { ApiError } from '../lib/api'
import { cn } from '../lib/cn'
import type { DietaryType } from '../types'

const ALLERGY_PRESETS = ['Hải sản', 'Cá', 'Trứng', 'Sữa', 'Đậu phộng', 'Đậu nành', 'Mè', 'Gluten', 'Hạt']
const DIET_PRESETS = ['Chay', 'Thuần chay']

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

export function Account() {
  const { user, logout } = useAuth()
  const { items, loading, error, reload, add, remove } = useDietaryPreferences()
  const [removingId, setRemovingId] = useState<number | null>(null)
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
        <div className="flex items-center justify-between gap-4 rounded-card border border-line-soft bg-white p-6">
          <div className="flex items-center gap-4">
            <span className="grid size-14 shrink-0 place-items-center rounded-full bg-accent text-xl font-semibold text-surface">
              {initial}
            </span>
            <div className="min-w-0">
              <p className="truncate text-lg font-medium text-ink">
                {user?.fullName || 'Chưa đặt tên'}
              </p>
              <p className="truncate text-sm text-muted">{user?.email}</p>
            </div>
          </div>
          <Button variant="secondary" onClick={() => void logout()}>
            <SignOut className="size-4" />
            Đăng xuất
          </Button>
        </div>

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
      </div>
    </div>
  )
}
