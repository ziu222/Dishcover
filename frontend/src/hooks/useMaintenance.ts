import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'

const BASE = '/admin/maintenance'

/** Trạng thái bảo trì + toggle, cho màn quản trị /admin/bao-tri. */
export function useMaintenance() {
  const [enabled, setEnabled] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [toggling, setToggling] = useState(false)

  const reload = useCallback(() => {
    setLoading(true)
    setError(null)
    api<{ enabled: boolean }>(BASE)
      .then((res) => setEnabled(res.enabled))
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : 'Không tải được trạng thái bảo trì.'),
      )
      .finally(() => setLoading(false))
  }, [])

  useEffect(reload, [reload])

  async function toggle(next: boolean) {
    setToggling(true)
    setError(null)
    try {
      const res = await api<{ enabled: boolean }>(BASE, { method: 'PUT', body: { enabled: next } })
      setEnabled(res.enabled)
      return true
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không đổi được trạng thái bảo trì.')
      return false
    } finally {
      setToggling(false)
    }
  }

  return { enabled, loading, error, toggling, reload, toggle }
}
