import { useState } from 'react'
import { motion } from 'framer-motion'
import { MagnifyingGlass, SmileySad } from '@phosphor-icons/react'
import { useRecipeSearch } from '../hooks/useRecipeSearch'
import { useFavorites } from '../hooks/useFavorites'
import { SearchInput } from '../components/SearchInput'
import { Chip } from '../components/Chip'
import { RecipeCard } from '../components/RecipeCard'
import { Spinner } from '../components/Spinner'
import type { Difficulty } from '../types'

const DIFFICULTIES: Array<{ value: Difficulty | null; label: string }> = [
  { value: null, label: 'Tất cả' },
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Vừa' },
  { value: 'HARD', label: 'Khó' },
]

// Giá trị tiếng Anh thật khớp tags/dietary_flags do fetch-spoonacular.mjs sinh ra (xem
// specs/diet-direction-recommendation.md mục 7.4) — nhãn tiếng Việt chỉ để hiển thị.
const DIET_TAGS: Array<{ value: string; label: string }> = [
  { value: 'vegetarian', label: 'Chay' },
  { value: 'vegan', label: 'Thuần chay' },
  { value: 'high protein', label: 'Giàu đạm' },
  { value: 'gluten free', label: 'Không gluten' },
]

const grid = 'grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3'

export function Search() {
  const [query, setQuery] = useState('')
  const [difficulty, setDifficulty] = useState<Difficulty | null>(null)
  const [tag, setTag] = useState<string | null>(null)
  const { recipes, loading, error } = useRecipeSearch(query, difficulty, tag)
  const { favorites, toggle } = useFavorites()

  const idle = query.trim().length === 0 && difficulty === null && tag === null

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
        <div className="mt-2 flex flex-wrap gap-2">
          {DIET_TAGS.map((t) => (
            <Chip
              key={t.value}
              active={tag === t.value}
              onClick={() => setTag((current) => (current === t.value ? null : t.value))}
            >
              {t.label}
            </Chip>
          ))}
        </div>
      </div>

      <div className="mt-10">
        {idle ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <MagnifyingGlass className="mx-auto mb-4 size-10 text-mist" />
            <p className="font-display text-2xl font-light text-ink">Tìm món để nấu</p>
            <p className="mt-2 text-sm text-muted">
              Nhập tên món hoặc chọn độ khó để bắt đầu tìm.
            </p>
          </div>
        ) : loading ? (
          <Spinner label="Đang tìm…" />
        ) : error ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <p className="text-[15px] text-muted">{error}</p>
          </div>
        ) : recipes.length === 0 ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <SmileySad className="mx-auto mb-4 size-10 text-mist" />
            <p className="font-display text-2xl font-light text-ink">Không tìm thấy món nào</p>
            <p className="mt-2 text-sm text-muted">
              Thử từ khoá khác hoặc bỏ bớt bộ lọc độ khó/định hướng ăn uống.
            </p>
          </div>
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
