import { useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { CookingPot } from '@phosphor-icons/react'
import { useRecipes } from '../hooks/useRecipes'
import { useFavorites } from '../hooks/useFavorites'
import { useAuth } from '../auth/AuthContext'
import { RecipeCard } from '../components/RecipeCard'
import { Chip } from '../components/Chip'
import { Button } from '../components/Button'

function greeting(name: string): string {
  const h = new Date().getHours()
  const part = h < 11 ? 'sáng' : h < 14 ? 'trưa' : h < 18 ? 'chiều' : 'tối'
  return `Chào buổi ${part}, ${name}`
}

const grid = 'grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3'

export function Home() {
  const { user } = useAuth()
  const { recipes, loading, error, reload } = useRecipes()
  const { favorites, toggle: toggleFav } = useFavorites()
  const [activeTag, setActiveTag] = useState<string | null>(null)

  const topTags = useMemo(() => {
    const counts = new Map<string, number>()
    for (const r of recipes) for (const t of r.tags) counts.set(t, (counts.get(t) ?? 0) + 1)
    return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6).map(([t]) => t)
  }, [recipes])

  const filtered = useMemo(
    () => (activeTag ? recipes.filter((r) => r.tags.includes(activeTag)) : recipes),
    [recipes, activeTag],
  )

  const name = user?.fullName?.split(' ').at(-1) || 'bạn'

  return (
    <div className="px-6 py-9 lg:px-10">
      {/* Header — bất đối xứng: lời chào lớn bên trái, chip lọc bên phải */}
      <div className="mb-8 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
            {greeting(name)}
          </div>
          <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
            Hôm nay nấu gì?
          </h1>
        </div>
        {topTags.length > 0 && (
          <div className="flex flex-wrap gap-2 lg:justify-end lg:max-w-xl">
            <Chip active={activeTag === null} onClick={() => setActiveTag(null)}>
              Tất cả
            </Chip>
            {topTags.map((t) => (
              <Chip key={t} active={activeTag === t} onClick={() => setActiveTag(t)}>
                {t}
              </Chip>
            ))}
          </div>
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
            {activeTag ? 'Thử bỏ bộ lọc để xem tất cả công thức.' : 'Danh sách công thức đang trống.'}
          </p>
          {activeTag && (
            <Button variant="secondary" className="mt-5" onClick={() => setActiveTag(null)}>
              Xem tất cả
            </Button>
          )}
        </div>
      ) : (
        <motion.div
          className={grid}
          initial="hidden"
          animate="show"
          variants={{ show: { transition: { staggerChildren: 0.05 } } }}
        >
          {filtered.map((r) => (
            <motion.div
              key={r.id}
              variants={{
                hidden: { opacity: 0, y: 16 },
                show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 120, damping: 20 } },
              }}
            >
              <RecipeCard recipe={r} favorited={favorites.has(r.id)} onToggleFav={toggleFav} />
            </motion.div>
          ))}
        </motion.div>
      )}
    </div>
  )
}
