import { Link, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowLeft, Clock, CookingPot, ForkKnife, Timer } from '@phosphor-icons/react'
import { useRecipe } from '../hooks/useRecipes'
import { Button } from '../components/Button'
import { DIFFICULTY_VI, dietaryLabel } from '../lib/labels'
import type { RecipeIngredient } from '../types'

const PLACEHOLDER =
  'repeating-linear-gradient(135deg,#E9E1D2,#E9E1D2 11px,#E4DBC9 11px,#E4DBC9 22px)'

/** "3 quả", "500 g", hoặc rỗng khi seed không ghi định lượng. */
function quantity(i: RecipeIngredient): string {
  return [i.amount ?? null, i.unit].filter((v) => v !== null && v !== '').join(' ')
}

function BackLink() {
  return (
    <Link
      to="/"
      className="mb-7 inline-flex items-center gap-2 text-sm text-muted transition-colors hover:text-accent"
    >
      <ArrowLeft className="size-4" />
      Khám phá
    </Link>
  )
}

function IngredientRow({ item }: { item: RecipeIngredient }) {
  const qty = quantity(item)
  return (
    <li className="flex items-baseline justify-between gap-4 border-b border-line-soft py-2.5 last:border-0">
      <span className="text-[15px] text-ink">{item.name}</span>
      {qty && <span className="shrink-0 text-[13px] tabular-nums text-faint">{qty}</span>}
    </li>
  )
}

export function RecipeDetail() {
  const { id } = useParams<{ id: string }>()
  const { recipe, loading, error, notFound, reload } = useRecipe(id)

  if (loading) {
    return (
      <div className="px-6 py-9 lg:px-10" aria-busy>
        <div className="mb-7 h-4 w-24 animate-pulse rounded bg-line-soft/70" />
        <div className="h-80 animate-pulse rounded-card bg-line-soft/60" />
        <div className="mt-8 h-10 w-2/3 animate-pulse rounded bg-line-soft/70" />
        <div className="mt-4 h-4 w-40 animate-pulse rounded bg-line-soft/50" />
      </div>
    )
  }

  if (notFound) {
    return (
      <div className="px-6 py-9 lg:px-10">
        <BackLink />
        <div className="mx-auto max-w-md py-20 text-center">
          <CookingPot className="mx-auto mb-4 size-10 text-mist" />
          <p className="font-display text-2xl font-light text-ink">Không tìm thấy công thức</p>
          <p className="mt-2 text-sm text-muted">
            Công thức này có thể đã bị xoá hoặc đường dẫn không đúng.
          </p>
          <Link to="/">
            <Button variant="secondary" className="mt-5">
              Về trang khám phá
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  if (error || !recipe) {
    return (
      <div className="px-6 py-9 lg:px-10">
        <BackLink />
        <div className="mx-auto max-w-md py-20 text-center">
          <p className="text-[15px] text-muted">{error ?? 'Không tải được công thức.'}</p>
          <Button variant="secondary" className="mt-5" onClick={reload}>
            Thử lại
          </Button>
        </div>
      </div>
    )
  }

  const essential = recipe.ingredients.filter((i) => i.essential)
  const optional = recipe.ingredients.filter((i) => !i.essential)
  const totalStepMinutes = recipe.steps.reduce((sum, s) => sum + s.durationMinutes, 0)

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', stiffness: 120, damping: 20 }}
      className="px-6 py-9 lg:px-10"
    >
      <BackLink />

      {/* Hero */}
      <div
        className="relative h-64 overflow-hidden rounded-card sm:h-80 lg:h-96"
        style={{ background: PLACEHOLDER }}
      >
        {recipe.imageUrl ? (
          <img src={recipe.imageUrl} alt={recipe.name} className="size-full object-cover" />
        ) : (
          <span className="absolute inset-0 grid place-items-center text-[10px] font-medium uppercase tracking-[0.18em] text-mist">
            ảnh · {recipe.name}
          </span>
        )}
      </div>

      {/* Tiêu đề + meta */}
      <div className="mt-8 max-w-3xl">
        {recipe.tags.length > 0 && (
          <div className="mb-3 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
            {recipe.tags.join(' · ')}
          </div>
        )}
        <h1 className="font-display text-4xl font-extralight leading-tight tracking-tight text-ink lg:text-5xl">
          {recipe.name}
        </h1>

        <div className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-2 text-[13px] text-muted">
          <span className="inline-flex items-center gap-2">
            <Clock className="size-[18px] text-mist" />
            {recipe.cookTimeMinutes} phút
          </span>
          <span className="inline-flex items-center gap-2">
            <ForkKnife className="size-[18px] text-mist" />
            {DIFFICULTY_VI[recipe.difficulty]}
          </span>
          <span className="inline-flex items-center gap-2">
            <CookingPot className="size-[18px] text-mist" />
            {recipe.ingredients.length} nguyên liệu
          </span>
        </div>

        {recipe.dietaryFlags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {recipe.dietaryFlags.map((f) => (
              <span
                key={f}
                className="rounded-full border border-line bg-surface px-3 py-1 text-[11.5px] font-medium text-muted"
              >
                {dietaryLabel(f)}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Nguyên liệu | Các bước */}
      <div className="mt-12 grid gap-10 lg:grid-cols-[minmax(0,300px)_minmax(0,1fr)] lg:gap-16">
        <aside className="lg:sticky lg:top-8 lg:self-start">
          <h2 className="mb-4 font-display text-2xl font-light text-ink">Nguyên liệu</h2>

          <ul className="rounded-card border border-line-soft bg-white px-5 py-2">
            {essential.map((i) => (
              <IngredientRow key={i.normalizedName} item={i} />
            ))}
          </ul>

          {optional.length > 0 && (
            <>
              <div className="mb-2 mt-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-faint">
                Gia vị / phụ liệu
              </div>
              <ul className="rounded-card border border-line-soft bg-white/60 px-5 py-2">
                {optional.map((i) => (
                  <IngredientRow key={i.normalizedName} item={i} />
                ))}
              </ul>
            </>
          )}
        </aside>

        <section>
          <div className="mb-5 flex items-baseline justify-between gap-4">
            <h2 className="font-display text-2xl font-light text-ink">Cách làm</h2>
            {totalStepMinutes > 0 && (
              <span className="text-[12px] text-faint">≈ {totalStepMinutes} phút thao tác</span>
            )}
          </div>

          <ol className="space-y-6">
            {recipe.steps.map((s, idx) => (
              <motion.li
                key={s.order}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.05 * idx, type: 'spring', stiffness: 120, damping: 20 }}
                className="flex gap-4"
              >
                <span className="mt-0.5 grid size-8 shrink-0 place-items-center rounded-full bg-accent-wash text-[13px] font-semibold text-accent-strong">
                  {s.order}
                </span>
                <div className="min-w-0">
                  <h3 className="mb-1 flex flex-wrap items-baseline gap-x-3 text-[15px] font-semibold text-ink">
                    {s.title}
                    {s.durationMinutes > 0 && (
                      <span className="inline-flex items-center gap-1 text-[11.5px] font-medium text-faint">
                        <Timer className="size-3.5" />
                        {s.durationMinutes} phút
                      </span>
                    )}
                  </h3>
                  <p className="text-[15px] leading-relaxed text-muted">{s.content}</p>
                </div>
              </motion.li>
            ))}
          </ol>
        </section>
      </div>
    </motion.div>
  )
}
