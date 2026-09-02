import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { CalorieGoal } from '../types'

const BASE = '/user-service/users/me/calorie-goal'

interface State {
  goal: CalorieGoal | null
  loading: boolean
  error: string | null
}

/** Mục tiêu calo/macro/ngày — chưa đặt thì goal=null (opt-in, không phải lỗi). */
export function useCalorieGoal() {
  const [state, setState] = useState<State>({ goal: null, loading: true, error: null })

  const load = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }))
    api<CalorieGoal | undefined>(BASE)
      .then((goal) => setState({ goal: goal ?? null, loading: false, error: null }))
      .catch((err) => {
        const msg = err instanceof ApiError ? err.message : 'Không tải được mục tiêu calo.'
        setState({ goal: null, loading: false, error: msg })
      })
  }, [])

  useEffect(load, [load])

  const save = useCallback(async (goal: CalorieGoal) => {
    const saved = await api<CalorieGoal>(BASE, { method: 'PUT', body: goal })
    setState({ goal: saved, loading: false, error: null })
  }, [])

  return { ...state, reload: load, save }
}
