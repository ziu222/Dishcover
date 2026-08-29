import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { DietaryPreference, DietaryType } from '../types'

const BASE = '/user-service/users/me/dietary-preferences'

interface State {
  items: DietaryPreference[]
  loading: boolean
  error: string | null
}

/** Hồ sơ ăn uống (dị ứng/chế độ ăn) — Matching Service đọc để lọc công thức. */
export function useDietaryPreferences() {
  const [state, setState] = useState<State>({ items: [], loading: true, error: null })

  const load = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }))
    api<DietaryPreference[]>(BASE)
      .then((items) => setState({ items, loading: false, error: null }))
      .catch((err) => {
        const msg = err instanceof ApiError ? err.message : 'Không tải được hồ sơ ăn uống.'
        setState({ items: [], loading: false, error: msg })
      })
  }, [])

  useEffect(load, [load])

  const add = useCallback(async (type: DietaryType, value: string) => {
    const created = await api<DietaryPreference>(BASE, { method: 'POST', body: { type, value } })
    setState((s) => ({ ...s, items: [...s.items, created] }))
  }, [])

  const remove = useCallback(async (id: number) => {
    await api<void>(`${BASE}/${id}`, { method: 'DELETE' })
    setState((s) => ({ ...s, items: s.items.filter((i) => i.id !== id) }))
  }, [])

  return { ...state, reload: load, add, remove }
}
