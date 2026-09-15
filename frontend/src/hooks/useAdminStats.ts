import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'

interface UserStats {
  totalUsers: number
  adminCount: number
  lockedCount: number
  newUsersLast7Days: number
}

interface RecipeStats {
  totalRecipes: number
  easyCount: number
  mediumCount: number
  hardCount: number
  newRecipesLast7Days: number
}

export interface AdminStats {
  users: UserStats
  recipes: RecipeStats
}

/** Số liệu tổng quan cho màn /admin/so-lieu — mỗi service tự đếm dữ liệu của mình, gộp ở đây. */
export function useAdminStats() {
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(() => {
    setLoading(true)
    setError(null)
    Promise.all([api<UserStats>('/user-service/admin/stats'), api<RecipeStats>('/recipe-service/admin/stats')])
      .then(([users, recipes]) => setStats({ users, recipes }))
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Không tải được số liệu.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(reload, [reload])

  return { stats, loading, error, reload }
}
