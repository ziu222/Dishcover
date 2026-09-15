import type { ReactNode } from 'react'
import { Warning } from '@phosphor-icons/react'
import { AdminNav } from '../components/AdminNav'
import { Button } from '../components/Button'
import { Spinner } from '../components/Spinner'
import { useAdminStats } from '../hooks/useAdminStats'

const numberFmt = new Intl.NumberFormat('vi-VN')

function StatTile({ value, label }: { value: number; label: string }) {
  return (
    <div className="py-5">
      <p className="font-display text-3xl font-light tracking-tight text-ink">
        {numberFmt.format(value)}
      </p>
      <p className="mt-1 text-[13px] text-faint">{label}</p>
    </div>
  )
}

function StatSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-9 first:mt-0">
      <h2 className="text-[11px] font-semibold uppercase tracking-[0.14em] text-mist">{title}</h2>
      <div className="mt-1 grid grid-cols-2 divide-x divide-line-soft border-t border-line-soft sm:grid-cols-4">
        {children}
      </div>
    </section>
  )
}

/** Số liệu tổng quan, gộp từ User Service + Recipe Service — mỗi service tự đếm dữ liệu của mình. */
export function AdminAnalytics() {
  const { stats, loading, error, reload } = useAdminStats()

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <AdminNav
          title="Số liệu tổng quan"
          description="Ảnh chụp nhanh dữ liệu hiện có, đếm trực tiếp tại thời điểm mở trang — không lưu lịch sử theo thời gian."
        />

        {error && (
          <div className="mt-5 flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
            <Button variant="secondary" className="ml-auto shrink-0" onClick={reload}>
              Thử lại
            </Button>
          </div>
        )}

        {loading ? (
          <Spinner label="Đang tải số liệu…" />
        ) : (
          stats && (
            <>
              <StatSection title="Người dùng">
                <StatTile value={stats.users.totalUsers} label="Tổng tài khoản" />
                <StatTile value={stats.users.adminCount} label="Quản trị viên" />
                <StatTile value={stats.users.lockedCount} label="Đang bị khoá" />
                <StatTile value={stats.users.newUsersLast7Days} label="Mới trong 7 ngày" />
              </StatSection>

              <StatSection title="Công thức">
                <StatTile value={stats.recipes.totalRecipes} label="Tổng công thức" />
                <StatTile value={stats.recipes.easyCount} label="Độ khó: Dễ" />
                <StatTile value={stats.recipes.mediumCount} label="Độ khó: Trung bình" />
                <StatTile value={stats.recipes.hardCount} label="Độ khó: Khó" />
              </StatSection>
            </>
          )
        )}
      </div>
    </div>
  )
}
