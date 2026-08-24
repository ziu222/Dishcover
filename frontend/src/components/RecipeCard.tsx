import { motion } from 'framer-motion'
import { Heart } from '@phosphor-icons/react'
import type { Difficulty, RecipeSummary } from '../types'
import { cn } from '../lib/cn'

const DIFFICULTY_VI: Record<Difficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Vừa',
  HARD: 'Khó',
}

// Nền sọc chéo thay ảnh khi công thức chưa có imageUrl (giống mockup).
const PLACEHOLDER =
  'repeating-linear-gradient(135deg,#E9E1D2,#E9E1D2 11px,#E4DBC9 11px,#E4DBC9 22px)'

interface RecipeCardProps {
  recipe: RecipeSummary
  favorited: boolean
  onToggleFav: (id: string) => void
}

export function RecipeCard({ recipe, favorited, onToggleFav }: RecipeCardProps) {
  const cuisine = recipe.tags[0]
  return (
    <motion.article
      whileHover={{ y: -4 }}
      transition={{ type: 'spring', stiffness: 300, damping: 24 }}
      className="group cursor-pointer overflow-hidden rounded-card border border-line-soft bg-white"
    >
      <div className="relative h-49 overflow-hidden" style={{ background: PLACEHOLDER }}>
        {recipe.imageUrl ? (
          <img
            src={recipe.imageUrl}
            alt={recipe.name}
            loading="lazy"
            className="size-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <span className="absolute inset-0 grid place-items-center text-[10px] font-medium uppercase tracking-[0.18em] text-mist">
            ảnh · {recipe.name}
          </span>
        )}

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            onToggleFav(recipe.id)
          }}
          aria-label={favorited ? 'Bỏ yêu thích' : 'Yêu thích'}
          aria-pressed={favorited}
          className="absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-card/90 text-accent backdrop-blur-sm transition-transform active:scale-90"
        >
          <Heart weight={favorited ? 'fill' : 'regular'} className="size-[18px]" />
        </button>

        {cuisine && (
          <span className="absolute bottom-3 left-3 rounded-full bg-ink/70 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-white backdrop-blur-sm">
            {cuisine}
          </span>
        )}
      </div>

      <div className="p-5">
        <h3 className="mb-3 font-display text-[22px] font-normal leading-tight text-ink">
          {recipe.name}
        </h3>
        <div className={cn('text-[10.5px] font-semibold uppercase tracking-[0.1em] text-faint')}>
          {recipe.cookTimeMinutes} phút&nbsp;&nbsp;·&nbsp;&nbsp;{DIFFICULTY_VI[recipe.difficulty]}
        </div>
      </div>
    </motion.article>
  )
}
