import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

interface AuthLayoutProps {
  title: string
  subtitle: string
  children: ReactNode
  /** Dòng chuyển đổi ở chân form (VD "Chưa có tài khoản? Đăng ký"). */
  footer: ReactNode
}

const PROMISES = [
  'Gợi ý món theo nguyên liệu còn trong tủ lạnh',
  'Ưu tiên giải cứu đồ sắp hết hạn',
  'Hỏi đáp với trợ lý nấu ăn AI',
]

/** Khung đăng nhập/đăng ký dạng chia đôi: bảng thương hiệu trái · form phải.
 *  Mobile chỉ hiện cột form (bảng trái ẩn) — tránh vỡ layout (skill mobile override). */
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="grid min-h-[100dvh] lg:grid-cols-2">
      {/* Bảng thương hiệu — editorial, chỉ desktop */}
      <aside className="relative hidden overflow-hidden bg-gradient-to-br from-accent-strong to-ink px-14 py-16 lg:flex lg:flex-col lg:justify-between">
        <Link to="/" className="font-display text-3xl font-extralight tracking-tight text-surface">
          Larder<span className="text-accent">.</span>
        </Link>
        <div>
          <h2 className="max-w-md font-display text-5xl font-extralight leading-[1.05] tracking-tight text-surface">
            Nấu từ những gì <span className="italic text-white/80">bạn đang có</span>.
          </h2>
          <ul className="mt-10 flex flex-col gap-3">
            {PROMISES.map((p, i) => (
              <motion.li
                key={p}
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.15 + i * 0.1, type: 'spring', stiffness: 90, damping: 18 }}
                className="flex items-center gap-3 text-[15px] text-surface/75"
              >
                <span className="size-1.5 rounded-full bg-accent" />
                {p}
              </motion.li>
            ))}
          </ul>
        </div>
        <p className="text-xs uppercase tracking-[0.2em] text-surface/40">Leftover Recipe Matcher</p>
      </aside>

      {/* Cột form */}
      <main className="flex items-center justify-center bg-bg px-6 py-12 sm:px-10">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 90, damping: 18 }}
          className="w-full max-w-sm"
        >
          <Link
            to="/"
            className="font-display text-2xl font-extralight tracking-tight text-ink lg:hidden"
          >
            Larder<span className="text-accent">.</span>
          </Link>
          <h1 className="mt-8 font-display text-4xl font-light tracking-tight text-ink lg:mt-0">
            {title}
          </h1>
          <p className="mt-3 text-[15px] leading-relaxed text-muted">{subtitle}</p>

          <div className="mt-9">{children}</div>

          <div className="mt-8 text-sm text-muted">{footer}</div>
        </motion.div>
      </main>
    </div>
  )
}
