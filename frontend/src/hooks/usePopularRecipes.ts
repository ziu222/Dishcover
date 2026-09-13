import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import type { Page, RecipeSummary } from '../types'

/**
 * Vài công thức để mồi cho màn Tìm kiếm (trạng thái chờ nhập và trạng thái không có kết quả).
 * Lấy thẳng từ kho công thức thật nên chip gợi ý luôn bấm ra kết quả — khác với danh sách
 * từ khoá viết cứng, vốn có thể trỏ vào món không tồn tại trong database.
 */
export function usePopularRecipes(limit = 6) {
  const [names, setNames] = useState<string[]>([])

  useEffect(() => {
    let cancelled = false
    api<Page<RecipeSummary>>('/recipe-service/recipes', { params: { size: limit } })
      .then((page) => {
        if (!cancelled) setNames(page.content.map((r) => r.name))
      })
      .catch(() => {
        /* gợi ý là thứ có thì tốt — hỏng thì màn vẫn dùng được, không báo lỗi. */
      })
    return () => {
      cancelled = true
    }
  }, [limit])

  return names
}
