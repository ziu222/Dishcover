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

  // silent=true: làm mới NGẦM sau khi thêm/sửa/xoá — KHÔNG bật spinner nên danh sách giữ nguyên
  // mounted, để AnimatePresence thấy thay đổi từng phần và chạy animation rơi-vào/bốc-hơi.
  const fetchItems = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    setError(null)
    try {
      setItems(await api<InventoryItem[]>(BASE))
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Không tải được tủ lạnh.')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [])

  const reload = useCallback(() => fetchItems(false), [fetchItems])

  useEffect(() => {
    fetchItems(false)
  }, [fetchItems])

  const add = useCallback(
    async (input: ItemInput) => {
      await api<InventoryItem>(BASE, { method: 'POST', body: input })
      await fetchItems(true)
    },
    [fetchItems],
  )

  const update = useCallback(
    async (id: number, input: ItemInput) => {
      await api<InventoryItem>(`${BASE}/${id}`, { method: 'PATCH', body: input })
      await fetchItems(true)
    },
    [fetchItems],
  )

  const remove = useCallback(
    async (id: number) => {
      await api<void>(`${BASE}/${id}`, { method: 'DELETE' })
      await fetchItems(true)
    },
    [fetchItems],
  )

  /** Sau bước người dùng xác nhận kết quả nhận diện ảnh (human-in-the-loop) — mỗi item cùng
   *  quy tắc upsert theo lô như add(). */
  const addBatch = useCallback(
    async (items: ItemInput[]) => {
      await api<unknown>(`${BASE}/batch`, { method: 'POST', body: { items } })
      await fetchItems(true)
    },
    [fetchItems],
  )

  return { items, loading, error, reload, add, update, remove, addBatch }
}
