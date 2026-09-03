import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { Difficulty, Page, RecipeSummary } from '../types'

interface State {
  recipes: RecipeSummary[]
  loading: boolean
  error: string | null
}

const IDLE: State = { recipes: [], loading: false, error: null }

/** Tìm công thức phía server (`GET /recipes?q&difficulty&tag`), debounce 300ms theo từ khoá.
 *  Không có tiêu chí nào (q rỗng + không chọn độ khó/tag) thì không gọi API — màn hiện lời mời nhập.
 *  `tag` khớp CẢ `tags` lẫn `dietary_flags` phía server (xem RecipeRepository — "vegetarian" nằm
 *  ở dietary_flags chứ không phải tags cho batch Spoonacular, client không cần biết field nào). */
export function useRecipeSearch(query: string, difficulty: Difficulty | null, tag: string | null) {
  const [state, setState] = useState<State>(IDLE)
  const q = query.trim()
  const hasCriteria = q.length > 0 || difficulty !== null || tag !== null

  useEffect(() => {
    if (!hasCriteria) {
      setState(IDLE)
      return
    }
    let cancelled = false
    const handle = setTimeout(() => {
      setState((s) => ({ ...s, loading: true, error: null }))
      api<Page<RecipeSummary>>('/recipe-service/recipes', {
        params: { q: q || undefined, difficulty: difficulty ?? undefined, tag: tag ?? undefined, size: 40 },
      })
        .then((page) => {
          if (!cancelled) setState({ recipes: page.content, loading: false, error: null })
        })
        .catch((err) => {
          if (cancelled) return
          const msg = err instanceof ApiError ? err.message : 'Không tìm được công thức.'
          setState({ recipes: [], loading: false, error: msg })
        })
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(handle)
    }
  }, [q, difficulty, tag, hasCriteria])

  return state
}
