import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  Basket,
  Bell,
  ChatCircle,
  Compass,
  MagnifyingGlass,
  SignOut,
  Sparkle,
  User,
  type Icon,
} from '@phosphor-icons/react'
import { useAuth } from '../auth/AuthContext'
import { cn } from '../lib/cn'

interface NavItem {
  to: string
  label: string
  icon: Icon
  enabled: boolean
}

const NAV: NavItem[] = [
  { to: '/', label: 'Khám phá', icon: Compass, enabled: true },
  { to: '/tim-kiem', label: 'Tìm kiếm', icon: MagnifyingGlass, enabled: true },
  { to: '/tu-lanh', label: 'Tủ lạnh ảo', icon: Basket, enabled: true },
  { to: '/goi-y', label: 'Gợi ý theo nguyên liệu', icon: Sparkle, enabled: true },
  { to: '/chatbot', label: 'Trợ lý AI', icon: ChatCircle, enabled: true },
  { to: '/tai-khoan', label: 'Tài khoản', icon: User, enabled: true },
]

function pageTitle(pathname: string): string {
  if (pathname.startsWith('/cong-thuc/')) return 'Công thức'
  if (pathname === '/tim-kiem') return 'Tìm kiếm'
  if (pathname === '/tu-lanh') return 'Tủ lạnh ảo'
  if (pathname === '/goi-y') return 'Gợi ý theo nguyên liệu'
  if (pathname === '/chatbot') return 'Trợ lý AI'
  if (pathname === '/tai-khoan') return 'Tài khoản'
  return pathname === '/' ? 'Khám phá' : ''
}

/** Khung ứng dụng sau đăng nhập: sidebar trái + topbar + nội dung route (Outlet).
 *  Tập trung cho desktop; màn nhỏ sidebar ẩn, nội dung tràn rộng. */
export function AppShell() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const initial = (user?.fullName || user?.email || '?').trim().charAt(0).toUpperCase()

  return (
    <div className="flex min-h-[100dvh] bg-card">
      {/* Sidebar */}
      <aside className="hidden w-59 shrink-0 flex-col border-r border-line-soft bg-surface px-5 py-8 lg:flex">
        <NavLink to="/" className="mb-9 px-3 font-display text-[26px] font-extralight tracking-tight text-ink">
          Larder<span className="text-accent">.</span>
        </NavLink>
        <nav className="flex flex-col gap-1">
          {NAV.map(({ to, label, icon: Ico, enabled }) =>
            enabled ? (
              <NavLink
                key={to}
                to={to}
                end
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-lg px-3.5 py-2.5 text-sm transition-colors',
                    isActive
                      ? 'bg-accent-wash font-medium text-accent-strong'
                      : 'text-muted hover:bg-line-soft/50',
                  )
                }
              >
                <Ico className="size-5" />
                {label}
              </NavLink>
            ) : (
              <span
                key={to}
                title="Sắp có"
                className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3.5 py-2.5 text-sm text-faint/60"
              >
                <Ico className="size-5" />
                {label}
              </span>
            ),
          )}
        </nav>
      </aside>

      {/* Vùng chính */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-18 items-center justify-between border-b border-line-soft px-6 lg:px-10">
          <span className="font-display text-xl font-extralight tracking-tight text-ink lg:hidden">
            Larder<span className="text-accent">.</span>
          </span>
          <span className="hidden text-xs font-medium tracking-[0.04em] text-mist lg:inline">
            {pageTitle(pathname)}
          </span>
          <div className="flex items-center gap-5">
            <Bell className="size-[22px] text-muted" />
            <div className="flex items-center gap-2.5">
              <span className="grid size-9 place-items-center rounded-full bg-accent text-sm font-semibold text-surface">
                {initial}
              </span>
              <span className="hidden text-sm font-medium text-muted sm:inline">
                {user?.fullName || user?.email}
              </span>
            </div>
            <button
              type="button"
              onClick={logout}
              aria-label="Đăng xuất"
              className="text-mist transition-colors hover:text-accent"
            >
              <SignOut className="size-5" />
            </button>
          </div>
        </header>

        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
