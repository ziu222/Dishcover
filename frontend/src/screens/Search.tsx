import { useState } from 'react'
import { motion } from 'framer-motion'
import { useRecipeSearch } from '../hooks/useRecipeSearch'
import { usePopularRecipes } from '../hooks/usePopularRecipes'
import { useFavorites } from '../hooks/useFavorites'
import { SearchInput } from '../components/SearchInput'
import { Chip } from '../components/Chip'
import { RecipeCard } from '../components/RecipeCard'
import { Spinner } from '../components/Spinner'
import { EmptyState, EmptyStateChip } from '../components/EmptyState'
import type { Difficulty } from '../types'

const DIFFICULTIES: Array<{ value: Difficulty | null; label: string }> = [
  { value: null, label: 'Tất cả' },
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Vừa' },
  { value: 'HARD', label: 'Khó' },
]

const grid = 'grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3'

export function Search() {
  const [query, setQuery] = useState('')
  const [difficulty, setDifficulty] = useState<Difficulty | null>(null)
  const { recipes, loading, error } = useRecipeSearch(query, difficulty)
  const { favorites, toggle } = useFavorites()
  const popular = usePopularRecipes()

  const idle = query.trim().length === 0 && difficulty === null

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mx-auto max-w-3xl">
        <h1 className="mb-6 font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
          Tìm kiếm
        </h1>
        <SearchInput value={query} onChange={setQuery} autoFocus />
        <div className="mt-4 flex flex-wrap gap-2">
          {DIFFICULTIES.map((d) => (
            <Chip
              key={d.label}
              active={difficulty === d.value}
              onClick={() => setDifficulty(d.value)}
            >
              {d.label}
            </Chip>
          ))}
        </div>
      </div>

      <div className="mt-10">
        {idle ? (
          <EmptyState
            title="Hôm nay tìm món gì?"
            hint="Nhập tên món, chọn độ khó, hoặc bắt đầu từ vài món có sẵn bên dưới."
          >
            {popular.map((name) => (
              <EmptyStateChip key={name} onClick={() => setQuery(name)}>
                {name}
              </EmptyStateChip>
            ))}
          </EmptyState>
        ) : loading ? (
          <Spinner label="Đang tìm…" />
        ) : error ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <p className="text-[15px] text-muted">{error}</p>
          </div>
        ) : recipes.length === 0 ? (
          <EmptyState
            title={query.trim() ? `Không tìm thấy “${query.trim()}”` : 'Không tìm thấy món nào'}
            hint="Thử từ khoá khác, bỏ bớt bộ lọc độ khó, hoặc bắt đầu từ vài món phổ biến."
          >
            {popular.map((name) => (
              <EmptyStateChip key={name} onClick={() => setQuery(name)}>
                {name}
              </EmptyStateChip>
            ))}
          </EmptyState>
        ) : (
          <>
            <div className="mb-5 text-[13px] text-faint">{recipes.length} kết quả</div>
            <motion.div
              className={grid}
              initial="hidden"
              animate="show"
              variants={{ show: { transition: { staggerChildren: 0.04 } } }}
            >
              {recipes.map((r) => (
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
                  <RecipeCard recipe={r} favorited={favorites.has(r.id)} onToggleFav={toggle} />
                </motion.div>
              ))}
            </motion.div>
          </>
        )}
      </div>
    </div>
  )
}
