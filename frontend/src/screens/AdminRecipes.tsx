import { useMemo, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { PencilSimple, Plus, ShieldCheck, Trash, Warning } from '@phosphor-icons/react'
import { useRecipes } from '../hooks/useRecipes'
import { api, ApiError } from '../lib/api'
import { SearchInput } from '../components/SearchInput'
import { Button } from '../components/Button'
import { Modal } from '../components/Modal'
import { Spinner } from '../components/Spinner'
import { EmptyState } from '../components/EmptyState'
import { AdminRecipeForm } from '../components/AdminRecipeForm'
import type { RecipeSummary } from '../types'

const DIFFICULTY_LABEL: Record<string, string> = { EASY: 'Dễ', MEDIUM: 'Vừa', HARD: 'Khó' }

/** Bỏ dấu để tìm theo tên không cần gõ dấu — cùng cách IngredientCombobox đang làm. */
function fold(s: string) {
  return s
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .toLowerCase()
}

/**
 * Màn quản lý công thức cho ADMIN — bản tối giản đúng phạm vi spec
 * (docs/specs/admin-recipe-authorization.md mục 3.2): chỉ liệt kê và xoá, KHÔNG có form
 * tạo/sửa. Quyền thật do Recipe Service giữ; màn này chỉ là giao diện cho việc đó.
 */
export function AdminRecipes() {
  const { recipes, loading, error, reload } = useRecipes()
  const [query, setQuery] = useState('')
  const [target, setTarget] = useState<RecipeSummary | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<RecipeSummary | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const filtered = useMemo(() => {
    const q = fold(query.trim())
    if (!q) return recipes
    return recipes.filter((r) => fold(r.name).includes(q))
  }, [recipes, query])

  async function confirmDelete() {
    if (!target) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await api<void>(`/recipe-service/recipes/${target.id}`, { method: 'DELETE' })
      setTarget(null)
      reload()
    } catch (err) {
      // 403 nghĩa là token không còn quyền ADMIN — nói thẳng thay vì báo lỗi chung chung.
      const msg =
        err instanceof ApiError
          ? err.status === 403
            ? 'Tài khoản này không còn quyền quản trị. Đăng nhập lại rồi thử tiếp.'
            : err.message
          : 'Không xoá được công thức.'
      setDeleteError(msg)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <div className="mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-accent">
          <ShieldCheck weight="fill" className="size-4" />
          Khu vực quản trị
        </div>
        <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
          Quản lý công thức
        </h1>
        <p className="mt-3 max-w-xl text-sm leading-relaxed text-muted">
          Thêm, sửa và xoá công thức trong kho chung. Mọi thay đổi có hiệu lực ngay với tất cả
          người dùng.
        </p>

        <div className="mt-6">
          <Button
            onClick={() => {
              setEditing(null)
              setFormOpen(true)
            }}
          >
            <Plus weight="bold" className="size-4" />
            Thêm công thức
          </Button>
        </div>

        <div className="mt-8">
          <SearchInput value={query} onChange={setQuery} placeholder="Lọc theo tên công thức..." />
        </div>

        {loading ? (
          <Spinner label="Đang tải công thức…" />
        ) : error ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <p className="text-[15px] text-muted">{error}</p>
            <Button variant="secondary" className="mt-5" onClick={reload}>
              Thử lại
            </Button>
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title={query.trim() ? `Không có công thức nào khớp “${query.trim()}”` : 'Kho đang trống'}
            hint={query.trim() ? 'Thử một từ khoá khác.' : 'Chưa có công thức nào trong hệ thống.'}
          />
        ) : (
          <>
            <div className="mt-7 mb-1 flex items-baseline justify-between text-[13px] text-faint">
              <span>
                {filtered.length} công thức
                {query.trim() && ` / ${recipes.length}`}
              </span>
            </div>
            <ul className="divide-y divide-line-soft border-y border-line-soft">
              <AnimatePresence initial={false}>
                {filtered.map((r) => (
                  <motion.li
                    key={r.id}
                    layout
                    exit={{ opacity: 0, height: 0, transition: { duration: 0.25 } }}
                    className="flex items-center gap-4 py-4"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-display text-lg font-normal text-ink">{r.name}</p>
                      <p className="mt-1 text-[11px] uppercase tracking-[0.1em] text-mist">
                        {r.cookTimeMinutes} phút · {DIFFICULTY_LABEL[r.difficulty] ?? r.difficulty}
                        {r.tags.length > 0 && ` · ${r.tags.slice(0, 3).join(', ')}`}
                      </p>
                    </div>
                    <motion.button
                      type="button"
                      whileTap={{ scale: 0.92 }}
                      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                      onClick={() => {
                        setEditing(r)
                        setFormOpen(true)
                      }}
                      aria-label={`Sửa công thức ${r.name}`}
                      className="grid size-10 shrink-0 place-items-center rounded-full text-mist transition-colors hover:bg-accent-wash hover:text-accent focus-visible:ring-2 focus-visible:ring-accent/40 focus-visible:outline-none"
                    >
                      <PencilSimple className="size-[18px]" />
                    </motion.button>
                    <motion.button
                      type="button"
                      whileTap={{ scale: 0.92 }}
                      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                      onClick={() => {
                        setDeleteError(null)
                        setTarget(r)
                      }}
                      aria-label={`Xoá công thức ${r.name}`}
                      className="grid size-10 shrink-0 place-items-center rounded-full text-mist transition-colors hover:bg-expired-bg hover:text-expired focus-visible:ring-2 focus-visible:ring-expired/40 focus-visible:outline-none"
                    >
                      <Trash className="size-[18px]" />
                    </motion.button>
                  </motion.li>
                ))}
              </AnimatePresence>
            </ul>
          </>
        )}
      </div>

      <AdminRecipeForm
        open={formOpen}
        editing={editing}
        onClose={() => setFormOpen(false)}
        onSaved={reload}
      />

      <Modal open={target !== null} onClose={() => setTarget(null)} title="Xoá công thức?">
        <p className="text-sm leading-relaxed text-muted">
          Công thức <span className="font-medium text-ink">{target?.name}</span> sẽ biến mất khỏi
          kho chung của mọi người dùng. Không hoàn tác được.
        </p>
        {deleteError && (
          <div className="mt-4 flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{deleteError}</span>
          </div>
        )}
        <div className="mt-6 flex gap-3">
          <Button variant="secondary" onClick={() => setTarget(null)} disabled={deleting}>
            Huỷ
          </Button>
          <Button onClick={() => void confirmDelete()} disabled={deleting}>
            {deleting ? 'Đang xoá…' : 'Xoá công thức'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}
