import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { CookingPot } from '@phosphor-icons/react'
import { useRecipes } from '../hooks/useRecipes'
import { useFavorites } from '../hooks/useFavorites'
import { useAuth } from '../auth/AuthContext'
import { RecipeCard } from '../components/RecipeCard'
import { Select, type SelectOption } from '../components/Select'
import { Pagination } from '../components/Pagination'
import { Button } from '../components/Button'

function greeting(name: string): string {
  const h = new Date().getHours()
  const part = h < 11 ? 'sáng' : h < 14 ? 'trưa' : h < 18 ? 'chiều' : 'tối'
  return `Chào buổi ${part}, ${name}`
}

const grid = 'grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3'
const PAGE_SIZE = 12

export function Home() {
  const { user } = useAuth()
  const { recipes, loading, error, reload } = useRecipes()
  const { favorites, toggle: toggleFav } = useFavorites()
  const [tag, setTag] = useState('')
  const [page, setPage] = useState(0)

  // Dropdown: tất cả tag, xếp theo tần suất giảm dần. Tag trong DB lộn xộn hoa/thường
  // (tên vùng viết HOA, tag mô tả viết thường) — viết hoa chữ đầu CHỈ để hiển thị,
  // value giữ nguyên tag gốc để lọc vẫn khớp recipe.tags.
  const tagOptions = useMemo<SelectOption[]>(() => {
    const counts = new Map<string, number>()
    for (const r of recipes) for (const t of r.tags) counts.set(t, (counts.get(t) ?? 0) + 1)
    const sorted = [...counts.entries()].sort((a, b) => b[1] - a[1]).map(([t]) => t)
    const titleCase = (t: string) => t.charAt(0).toUpperCase() + t.slice(1)
    return [
      { value: '', label: 'Tất cả món' },
      ...sorted.map((t) => ({ value: t, label: titleCase(t) })),
    ]
  }, [recipes])

  const filtered = useMemo(
    () => (tag ? recipes.filter((r) => r.tags.includes(tag)) : recipes),
    [recipes, tag],
  )

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const safePage = Math.min(page, Math.max(0, totalPages - 1))
  const pageItems = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE)

  // Đổi trang -> cuộn lên đầu để không bị đứng ở giữa danh sách.
  useEffect(() => {
    if (page > 0) window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [page])

  const name = user?.fullName?.split(' ').at(-1) || 'bạn'

  return (
    <div className="px-6 py-9 lg:px-10">
      {/* Header — lời chào trái, dropdown lọc phải */}
      <div className="mb-8 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
            {greeting(name)}
          </div>
          <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
            Hôm nay nấu gì?
          </h1>
        </div>
        {tagOptions.length > 1 && (
          <Select
            value={tag}
            ariaLabel="Lọc theo loại món"
            options={tagOptions}
            onChange={(v) => {
              setTag(v)
              setPage(0)
            }}
          />
        )}
      </div>

      {/* Trạng thái */}
      {loading ? (
        <div className={grid} aria-busy>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="overflow-hidden rounded-card border border-line-soft bg-white">
              <div className="h-49 animate-pulse bg-line-soft/60" />
              <div className="space-y-3 p-5">
                <div className="h-5 w-3/4 animate-pulse rounded bg-line-soft/70" />
                <div className="h-3 w-1/2 animate-pulse rounded bg-line-soft/50" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <p className="text-[15px] text-muted">{error}</p>
          <Button variant="secondary" className="mt-5" onClick={reload}>
            Thử lại
          </Button>
        </div>
      ) : filtered.length === 0 ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <CookingPot className="mx-auto mb-4 size-10 text-mist" />
          <p className="font-display text-2xl font-light text-ink">Chưa có món phù hợp</p>
          <p className="mt-2 text-sm text-muted">
            {tag ? 'Thử chọn loại món khác.' : 'Danh sách công thức đang trống.'}
          </p>
          {tag && (
            <Button variant="secondary" className="mt-5" onClick={() => setTag('')}>
              Xem tất cả
            </Button>
          )}
        </div>
      ) : (
        <>
          <motion.div
            key={`${tag}-${safePage}`}
            className={grid}
            initial="hidden"
            animate="show"
            variants={{ show: { transition: { staggerChildren: 0.04 } } }}
          >
            {pageItems.map((r) => (
              <motion.div
                key={r.id}
                variants={{
                  hidden: { opacity: 0, y: 16 },
                  show: {
                    opacity: 1,
                    y: 0,
                    transition: { type: 'spring', stiffness: 120, damping: 20 },
                  },
                }}
              >
                <RecipeCard recipe={r} favorited={favorites.has(r.id)} onToggleFav={toggleFav} />
              </motion.div>
            ))}
          </motion.div>

          <Pagination page={safePage} totalPages={totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
