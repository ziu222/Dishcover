import { NavLink } from 'react-router-dom'
import { ShieldCheck } from '@phosphor-icons/react'
import { cn } from '../lib/cn'

const LINKS = [
  { to: '/admin/cong-thuc', label: 'Công thức' },
  { to: '/admin/nguoi-dung', label: 'Người dùng' },
  { to: '/admin/bao-tri', label: 'Bảo trì' },
  { to: '/admin/so-lieu', label: 'Số liệu' },
]

/** Đầu trang dùng chung cho khu quản trị: nhãn khu vực + chuyển giữa các mục. */
export function AdminNav({ title, description }: { title: string; description: string }) {
  return (
    <div className="mb-8">
      <div className="mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-accent">
        <ShieldCheck weight="fill" className="size-4" />
        Khu vực quản trị
      </div>
      <h1 className="font-display text-4xl font-extralight tracking-tight text-ink lg:text-5xl">
        {title}
      </h1>
      <p className="mt-3 max-w-xl text-sm leading-relaxed text-muted">{description}</p>

      <nav className="mt-6 flex gap-2" aria-label="Mục quản trị">
        {LINKS.map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'rounded-full border px-4 py-2 text-[13px] font-medium transition-colors',
                isActive
                  ? 'border-ink bg-ink text-surface'
                  : 'border-line bg-surface text-muted hover:border-mist',
              )
            }
          >
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
