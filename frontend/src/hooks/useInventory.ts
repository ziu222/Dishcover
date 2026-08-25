import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { InventoryItem } from '../types'

const BASE = '/inventory-service/inventory/items'

/** Payload thêm/sửa — server tự chuẩn hoá tên + suy hạn dùng nếu thiếu expiryDate. */
export interface ItemInput {
  ingredientName?: string
  quantity?: number | null
  unit?: string | null
  expiryDate?: string | null
}

/** Tủ lạnh ảo: nạp danh sách + thêm/sửa/xoá (đều cần JWT cookie). Mọi mutation xong reload lại. */
export function useInventory() {
  const [items, setItems] = useState<InventoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setItems(await api<InventoryItem[]>(BASE))
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Không tải được tủ lạnh.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    reload()
  }, [reload])

  const add = useCallback(
    async (input: ItemInput) => {
      await api<InventoryItem>(BASE, { method: 'POST', body: input })
      await reload()
    },
    [reload],
  )

  const update = useCallback(
    async (id: number, input: ItemInput) => {
      await api<InventoryItem>(`${BASE}/${id}`, { method: 'PATCH', body: input })
      await reload()
    },
    [reload],
  )

  const remove = useCallback(
    async (id: number) => {
      await api<void>(`${BASE}/${id}`, { method: 'DELETE' })
      await reload()
    },
    [reload],
  )

  return { items, loading, error, reload, add, update, remove }
}
