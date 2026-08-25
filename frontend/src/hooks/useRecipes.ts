import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { Page, RecipeDetail, RecipeSummary } from '../types'

interface State {
  recipes: RecipeSummary[]
  loading: boolean
  error: string | null
}

// ponytail: nạp 60 công thức 1 lần rồi lọc phía client — đúng ở quy mô ~62 công thức seed.
// Khi catalog lớn lên thì chuyển sang phân trang + lọc phía server (backend đã hỗ trợ tag/difficulty).
export function useRecipes() {
  const [state, setState] = useState<State>({ recipes: [], loading: true, error: null })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: null }))
    api<Page<RecipeSummary>>('/recipe-service/recipes', { params: { size: 60, sort: 'name,asc' } })
      .then((page) => {
        if (!cancelled) setState({ recipes: page.content, loading: false, error: null })
      })
      .catch((err) => {
        if (cancelled) return
        const msg = err instanceof ApiError ? err.message : 'Không tải được danh sách công thức.'
        setState({ recipes: [], loading: false, error: msg })
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  return { ...state, reload: () => setReloadKey((k) => k + 1) }
}

interface DetailState {
  recipe: RecipeDetail | null
  loading: boolean
  error: string | null
  /** Tách riêng 404 để màn hiển thị "không tìm thấy" thay vì lỗi tải chung. */
  notFound: boolean
}

/** Chi tiết 1 công thức. GET /recipes/{id} là endpoint công khai — không cần token. */
export function useRecipe(id: string | undefined) {
  const [state, setState] = useState<DetailState>({
    recipe: null,
    loading: true,
    error: null,
    notFound: false,
  })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!id) {
      setState({ recipe: null, loading: false, error: null, notFound: true })
      return
    }
    let cancelled = false
    setState({ recipe: null, loading: true, error: null, notFound: false })
    api<RecipeDetail>(`/recipe-service/recipes/${encodeURIComponent(id)}`)
      .then((recipe) => {
        if (!cancelled) setState({ recipe, loading: false, error: null, notFound: false })
      })
      .catch((err) => {
        if (cancelled) return
        const notFound = err instanceof ApiError && err.status === 404
        const msg = err instanceof ApiError ? err.message : 'Không tải được công thức.'
        setState({ recipe: null, loading: false, error: notFound ? null : msg, notFound })
      })
    return () => {
      cancelled = true
    }
  }, [id, reloadKey])

  return { ...state, reload: () => setReloadKey((k) => k + 1) }
}
