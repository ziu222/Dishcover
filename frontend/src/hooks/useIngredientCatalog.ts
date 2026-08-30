import { useEffect, useState } from 'react'
import { api } from '../lib/api'

// ponytail: cache ở module scope — danh sách ~200 nguyên liệu tĩnh, không đổi trong 1 phiên,
// không cần refetch mỗi lần mở modal thêm nguyên liệu.
let cache: string[] | null = null
let inflight: Promise<string[]> | null = null

function fetchCatalog(): Promise<string[]> {
  if (cache) return Promise.resolve(cache)
  if (!inflight) {
    inflight = api<string[]>('/inventory-service/inventory/catalog/ingredients')
      .then((names) => {
        cache = names
        return names
      })
      .catch(() => []) // gợi ý là tiện ích phụ — lỗi tải thì coi như rỗng, không chặn nhập tay
  }
  return inflight
}

/** Danh sách tên nguyên liệu chuẩn để gợi ý (autocomplete) khi nhập tay — không bắt buộc chọn
 *  từ danh sách này, chỉ hỗ trợ gõ nhanh + tránh sai chính tả. */
export function useIngredientCatalog(): string[] {
  const [names, setNames] = useState<string[]>(cache ?? [])

  useEffect(() => {
    if (cache) return
    let cancelled = false
    fetchCatalog().then((n) => {
      if (!cancelled) setNames(n)
    })
    return () => {
      cancelled = true
    }
  }, [])

  return names
}
