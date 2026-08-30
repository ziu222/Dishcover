import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, Basket, CheckCircle, Circle, Sparkle } from '@phosphor-icons/react'
import { useMatching } from '../hooks/useMatching'
import { Button } from '../components/Button'
import { Spinner } from '../components/Spinner'
import type { RecipeMatch } from '../types'

const PLACEHOLDER =
  'repeating-linear-gradient(135deg,#E9E1D2,#E9E1D2 11px,#E4DBC9 11px,#E4DBC9 22px)'

function coverage(m: RecipeMatch): number {
  const total = m.matchedIngredients.length + m.missingIngredients.length
  return total === 0 ? 0 : m.matchedIngredients.length / total
}

function MatchCard({ match, best }: { match: RecipeMatch; best: boolean }) {
  const pct = coverage(match)

  return (
    <motion.article
      layout
      className="group relative overflow-hidden rounded-card border border-line-soft bg-white shadow-[0_1px_2px_rgba(40,34,24,0.05)] transition-shadow duration-300 hover:border-line hover:shadow-[0_22px_44px_-20px_rgba(40,34,24,0.32)]"
    >
      <Link
        to={`/cong-thuc/${match.recipeId}`}
        className="flex flex-col outline-none focus-visible:ring-2 focus-visible:ring-accent/40 sm:flex-row"
      >
        <div
          className="relative aspect-[4/3] shrink-0 overflow-hidden bg-line-soft/50 sm:aspect-square sm:w-56"
          style={match.imageUrl ? undefined : { background: PLACEHOLDER }}
        >
          {match.imageUrl ? (
            <img
              src={match.imageUrl}
              alt={match.name}
              loading="lazy"
              className="size-full object-cover transition-transform duration-500 ease-out group-hover:scale-[1.06]"
            />
          ) : (
            <span className="absolute inset-0 grid place-items-center text-[10px] font-medium uppercase tracking-[0.18em] text-mist">
              ảnh · {match.name}
            </span>
          )}
          {best && (
            <span className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-accent px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-surface shadow-sm">
              <Sparkle weight="fill" className="size-3" />
              Phù hợp nhất
            </span>
          )}
        </div>

        <div className="flex min-w-0 flex-1 flex-col gap-4 p-6">
          <div className="flex items-start justify-between gap-4">
            <h3 className="font-display text-2xl font-normal leading-tight text-ink transition-colors group-hover:text-accent-strong">
              {match.name}
            </h3>
            <ArrowRight className="mt-1 size-4 shrink-0 -translate-x-1 text-accent opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100" />
          </div>

          {/* Thanh độ khớp — transform-only (scaleX) để không kích layout reflow */}
          <div>
            <div className="mb-1.5 flex items-center justify-between text-[11px] font-semibold uppercase tracking-[0.1em] text-faint">
              <span>Độ khớp tủ lạnh</span>
              <span className="tabular-nums text-accent-strong">{Math.round(pct * 100)}%</span>
            </div>
            <div className="h-1.5 overflow-hidden rounded-full bg-line-soft">
              <motion.div
                className="h-full origin-left rounded-full bg-accent"
                initial={{ scaleX: 0 }}
                animate={{ scaleX: pct }}
                transition={{ type: 'spring', stiffness: 90, damping: 20, delay: 0.1 }}
              />
            </div>
          </div>

          <div className="flex flex-wrap gap-1.5">
            {match.matchedIngredients.map((name) => (
              <span
                key={`m-${name}`}
                className="inline-flex items-center gap-1 rounded-full bg-fresh-bg px-2.5 py-1 text-[11.5px] font-medium text-fresh"
              >
                <CheckCircle weight="fill" className="size-3.5" />
                {name}
              </span>
            ))}
            {match.missingIngredients.map((name) => (
              <span
                key={`x-${name}`}
                className="inline-flex items-center gap-1 rounded-full border border-dashed border-line px-2.5 py-1 text-[11.5px] font-medium text-faint"
              >
                <Circle className="size-3.5" />
                {name}
              </span>
            ))}
          </div>
        </div>
      </Link>
    </motion.article>
  )
}

export function Matching() {
  const { matches, loading, error, reload } = useMatching()

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mb-8">
        <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent">
          Gợi ý theo nguyên liệu
        </div>
        <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
          Nấu được gì hôm nay?
        </h1>
        {!loading && !error && matches.length > 0 && (
          <p className="mt-3 text-sm text-muted">
            {matches.length} công thức khớp với nguyên liệu đang có trong tủ lạnh.
          </p>
        )}
      </div>

      {loading ? (
        <Spinner label="Đang tìm công thức phù hợp…" />
      ) : error ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <p className="text-[15px] text-muted">{error}</p>
          <Button variant="secondary" className="mt-5" onClick={reload}>
            Thử lại
          </Button>
        </div>
      ) : matches.length === 0 ? (
        <div className="mx-auto max-w-md py-20 text-center">
          <Basket className="mx-auto mb-4 size-10 text-mist" />
          <p className="font-display text-2xl font-light text-ink">Chưa có gợi ý nào</p>
          <p className="mt-2 text-sm text-muted">
            Thêm nguyên liệu vào tủ lạnh ảo để nhận công thức phù hợp.
          </p>
          <Link to="/tu-lanh">
            <Button className="mt-5">Đi đến tủ lạnh ảo</Button>
          </Link>
        </div>
      ) : (
        <motion.div
          className="mx-auto flex max-w-3xl flex-col gap-5"
          initial="hidden"
          animate="show"
          variants={{ show: { transition: { staggerChildren: 0.06 } } }}
        >
          {matches.map((m, idx) => (
            <motion.div
              key={m.recipeId}
              variants={{
                hidden: { opacity: 0, y: 16 },
                show: {
                  opacity: 1,
                  y: 0,
                  transition: { type: 'spring', stiffness: 120, damping: 20 },
                },
              }}
            >
              <MatchCard match={m} best={idx === 0} />
            </motion.div>
          ))}
        </motion.div>
      )}
    </div>
  )
}
