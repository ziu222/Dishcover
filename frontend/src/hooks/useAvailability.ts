import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { RecipeAvailability } from '../types'

interface State {
  availability: RecipeAvailability | null
  loading: boolean
  error: string | null
}

/**
 * So đủ/thiếu nguyên liệu công thức với tủ lạnh hiện tại — GET
 * /matching/recipes/{id}/availability (cần JWT cookie). Dùng cho Chi tiết công thức và màn xác
 * nhận "Đã nấu xong" (tự điền số lượng sắp trừ).
 */
export function useRecipeAvailability(recipeId: string | undefined) {
  const [state, setState] = useState<State>({ availability: null, loading: true, error: null })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!recipeId) {
      setState({ availability: null, loading: false, error: null })
      return
    }
    let cancelled = false
    setState({ availability: null, loading: true, error: null })
    api<RecipeAvailability>(`/matching-service/matching/recipes/${encodeURIComponent(recipeId)}/availability`)
      .then((availability) => {
        if (!cancelled) setState({ availability, loading: false, error: null })
      })
      .catch((err) => {
        if (cancelled) return
        const msg = err instanceof ApiError ? err.message : 'Không so được nguyên liệu với tủ lạnh.'
        setState({ availability: null, loading: false, error: msg })
      })
    return () => {
      cancelled = true
    }
  }, [recipeId, reloadKey])

  return { ...state, reload: () => setReloadKey((k) => k + 1) }
}
