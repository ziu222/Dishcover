import { useState } from 'react'
import { motion } from 'framer-motion'
import { Lock, LockOpen, ShieldCheck, User as UserIcon, Warning } from '@phosphor-icons/react'
import { useAdminUsers } from '../hooks/useAdminUsers'
import { useAuth } from '../auth/AuthContext'
import { AdminNav } from '../components/AdminNav'
import { SearchInput } from '../components/SearchInput'
import { Button } from '../components/Button'
import { Spinner } from '../components/Spinner'
import { EmptyState } from '../components/EmptyState'
import type { AdminUser } from '../types'

const dateFmt = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })

/**
 * Quản lý tài khoản: xem, khoá/mở, đổi role.
 *
 * Hàng của chính admin đang đăng nhập được đánh dấu rõ và ẩn nút nguy hiểm — backend cũng đã
 * chặn (409 SELF_TARGET), nhưng để nút bấm được rồi mới báo lỗi là thiết kế tồi.
 */
export function AdminUsers() {
  const { user: me } = useAuth()
  const [query, setQuery] = useState('')
  const { users, total, loading, error, actionError, reload, setLocked, setRole } =
    useAdminUsers(query)

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <AdminNav
          title="Quản lý người dùng"
          description="Khoá tài khoản vi phạm và phân quyền quản trị. Thay đổi có hiệu lực ngay ở lần đăng nhập kế tiếp."
        />

        <SearchInput value={query} onChange={setQuery} placeholder="Lọc theo email..." />

        {actionError && (
          <div className="mt-5 flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{actionError}</span>
          </div>
        )}

        {loading ? (
          <Spinner label="Đang tải tài khoản…" />
        ) : error ? (
          <div className="mx-auto max-w-md py-16 text-center">
            <p className="text-[15px] text-muted">{error}</p>
            <Button variant="secondary" className="mt-5" onClick={reload}>
              Thử lại
            </Button>
          </div>
        ) : users.length === 0 ? (
          <EmptyState
            title={query.trim() ? `Không có tài khoản nào khớp “${query.trim()}”` : 'Chưa có tài khoản nào'}
            hint={query.trim() ? 'Thử một từ khoá khác.' : undefined}
          />
        ) : (
          <>
            <p className="mt-7 mb-1 text-[13px] text-faint">{total} tài khoản</p>
            <ul className="divide-y divide-line-soft border-y border-line-soft">
              {users.map((u) => (
                <UserRow
                  key={u.id}
                  user={u}
                  isMe={me?.id === u.id}
                  onToggleLock={() => void setLocked(u.id, !u.locked)}
                  onToggleRole={() => void setRole(u.id, u.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                />
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  )
}

function UserRow({
  user,
  isMe,
  onToggleLock,
  onToggleRole,
}: {
  user: AdminUser
  isMe: boolean
  onToggleLock: () => void
  onToggleRole: () => void
}) {
  return (
    <motion.li layout className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:gap-4">
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-center gap-2 truncate text-[15px] font-medium text-ink">
          {user.email}
          {isMe && (
            <span className="rounded-full bg-accent-wash px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-accent-strong">
              Bạn
            </span>
          )}
          {user.role === 'ADMIN' && (
            <span className="flex items-center gap-1 rounded-full border border-accent/30 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-accent">
              <ShieldCheck weight="fill" className="size-3" />
              Admin
            </span>
          )}
          {user.locked && (
            <span className="rounded-full bg-expired-bg px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-expired">
              Đã khoá
            </span>
          )}
        </p>
        <p className="mt-1 text-[11px] uppercase tracking-[0.1em] text-mist">
          {user.fullName || 'Chưa đặt tên'} · tạo {dateFmt.format(new Date(user.createdAt))}
          {!user.emailVerified && ' · chưa xác thực email'}
        </p>
      </div>

      {/* Hàng của chính mình: không có nút nào cả — backend chặn 409 nhưng để bấm được rồi
          mới báo lỗi là thiết kế tồi. */}
      {!isMe && (
        <div className="flex shrink-0 gap-2">
          <Button variant="secondary" onClick={onToggleRole}>
            {user.role === 'ADMIN' ? 'Thu quyền admin' : 'Cấp quyền admin'}
          </Button>
          <Button variant="secondary" onClick={onToggleLock}>
            {user.locked ? <LockOpen className="size-4" /> : <Lock className="size-4" />}
            {user.locked ? 'Mở khoá' : 'Khoá'}
          </Button>
        </div>
      )}
      {isMe && (
        <span className="flex shrink-0 items-center gap-1.5 text-[12px] text-faint">
          <UserIcon className="size-4" />
          Không thao tác lên chính mình
        </span>
      )}
    </motion.li>
  )
}
