import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { AdminUser, Page } from '../types'

const BASE = '/user-service/admin/users'

/**
 * Danh sách tài khoản cho khu quản trị + các thao tác khoá/mở và đổi role.
 *
 * Lọc theo email chạy ở SERVER (khác màn công thức lọc phía client): số tài khoản có thể lớn
 * và không có lý do gì phải tải hết về máy chỉ để lọc.
 */
export function useAdminUsers(query: string) {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  useEffect(() => {
    let cancelled = false
    const handle = setTimeout(() => {
      setLoading(true)
      setError(null)
      api<Page<AdminUser>>(BASE, { params: { email: query.trim() || undefined, size: 50 } })
        .then((page) => {
          if (cancelled) return
          setUsers(page.content)
          setTotal(page.totalElements)
        })
        .catch((err) => {
          if (cancelled) return
          setError(err instanceof ApiError ? err.message : 'Không tải được danh sách tài khoản.')
        })
        .finally(() => {
          if (!cancelled) setLoading(false)
        })
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(handle)
    }
  }, [query, reloadKey])

  async function act(fn: () => Promise<AdminUser>) {
    setActionError(null)
    try {
      const updated = await fn()
      // Cập nhật tại chỗ thay vì nạp lại cả danh sách — giữ nguyên vị trí cuộn và bộ lọc.
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)))
      return true
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Thao tác không thành công.')
      return false
    }
  }

  return {
    users,
    total,
    loading,
    error,
    actionError,
    clearActionError: () => setActionError(null),
    reload,
    setLocked: (id: number, locked: boolean) =>
      act(() => api<AdminUser>(`${BASE}/${id}/lock`, { method: 'PATCH', body: { locked } })),
    setRole: (id: number, role: 'USER' | 'ADMIN') =>
      act(() => api<AdminUser>(`${BASE}/${id}/role`, { method: 'PATCH', body: { role } })),
  }
}
