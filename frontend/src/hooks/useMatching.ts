import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { RecipeMatch } from '../types'

interface State {
  matches: RecipeMatch[]
  loading: boolean
  error: string | null
}

/** Gợi ý công thức theo tủ lạnh hiện tại — GET /matching/suggestions (cần JWT cookie). */
export function useMatching() {
  const [state, setState] = useState<State>({ matches: [], loading: true, error: null })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: null }))
    api<RecipeMatch[]>('/matching-service/matching/suggestions')
      .then((matches) => {
        if (!cancelled) setState({ matches, loading: false, error: null })
      })
      .catch((err) => {
        if (cancelled) return
        const msg = err instanceof ApiError ? err.message : 'Không gợi ý được công thức, thử lại.'
        setState({ matches: [], loading: false, error: msg })
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  return { ...state, reload: () => setReloadKey((k) => k + 1) }
}
