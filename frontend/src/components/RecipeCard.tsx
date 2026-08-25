import { useState } from 'react'
import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { ArrowRight, Clock, Heart } from '@phosphor-icons/react'
import type { RecipeSummary } from '../types'
import { DIFFICULTY_VI } from '../lib/labels'
import { cn } from '../lib/cn'

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
  const [imgLoaded, setImgLoaded] = useState(false)
  return (
    <motion.article
      whileHover={{ y: -6 }}
      transition={{ type: 'spring', stiffness: 300, damping: 24 }}
      className="group relative overflow-hidden rounded-card border border-line-soft bg-white shadow-[0_1px_2px_rgba(40,34,24,0.05)] transition-shadow duration-300 hover:border-line hover:shadow-[0_22px_44px_-20px_rgba(40,34,24,0.32)]"
    >
      <Link
        to={`/cong-thuc/${recipe.id}`}
        className="block outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
      >
        {/* Ảnh — tỉ lệ cố định tránh nhảy layout, scrim tối đáy để badge nổi + có chiều sâu */}
        <div
          className="relative aspect-[4/3] overflow-hidden bg-line-soft/50"
          style={recipe.imageUrl ? undefined : { background: PLACEHOLDER }}
        >
          {recipe.imageUrl ? (
            <>
              <img
                src={recipe.imageUrl}
                alt={recipe.name}
                loading="lazy"
                onLoad={() => setImgLoaded(true)}
                className={cn(
                  'size-full object-cover transition-[opacity,transform] duration-500 ease-out group-hover:scale-[1.06]',
                  imgLoaded ? 'opacity-100' : 'opacity-0',
                )}
              />
              {/* Skeleton shimmer trong lúc ảnh đang tải — vệt sáng quét ngang */}
              {!imgLoaded && (
                <div className="pointer-events-none absolute inset-0 overflow-hidden">
                  <div className="absolute inset-0 animate-[shimmer_1.4s_infinite] bg-gradient-to-r from-transparent via-white/55 to-transparent motion-reduce:animate-none" />
                </div>
              )}
            </>
          ) : (
            <span className="absolute inset-0 grid place-items-center text-[10px] font-medium uppercase tracking-[0.18em] text-mist">
              ảnh · {recipe.name}
            </span>
          )}

          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-ink/55 to-transparent" />

          {cuisine && (
            <span className="absolute bottom-3 left-3 rounded-full bg-ink/45 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-white backdrop-blur-md">
              {cuisine}
            </span>
          )}
        </div>

        <div className="p-5">
          <h3 className="mb-3 line-clamp-2 min-h-[3.1rem] font-display text-[22px] font-normal leading-tight text-ink transition-colors group-hover:text-accent-strong">
            {recipe.name}
          </h3>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.1em] text-faint">
              <Clock weight="bold" className="size-3.5 text-mist" />
              {recipe.cookTimeMinutes} phút
              <span className="text-line">·</span>
              {DIFFICULTY_VI[recipe.difficulty]}
            </div>
            <ArrowRight
              className="size-4 -translate-x-1 text-accent opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100"
            />
          </div>
        </div>
      </Link>

      {/* Ngoài <Link> — button lồng trong anchor là HTML không hợp lệ. */}
      <motion.button
        type="button"
        whileTap={{ scale: 0.85 }}
        onClick={() => onToggleFav(recipe.id)}
        aria-label={favorited ? 'Bỏ yêu thích' : 'Yêu thích'}
        aria-pressed={favorited}
        className="absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-white/85 shadow-sm backdrop-blur-md transition-colors hover:bg-white"
      >
        <Heart
          weight={favorited ? 'fill' : 'regular'}
          className={favorited ? 'size-[18px] text-accent' : 'size-[18px] text-ink/70'}
        />
      </motion.button>
    </motion.article>
  )
}
