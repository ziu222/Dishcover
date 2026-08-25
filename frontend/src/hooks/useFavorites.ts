import { useState } from 'react'

// ponytail: yêu thích chỉ lưu localStorage — backend chưa có endpoint favorites.
// Khi có API thì đổi bên trong hook này, các màn dùng không phải sửa.
const FAV_KEY = 'larder.favorites'

function load(): Set<string> {
  try {
    return new Set(JSON.parse(localStorage.getItem(FAV_KEY) ?? '[]') as string[])
  } catch {
    return new Set()
  }
}

/** Danh sách công thức yêu thích, dùng chung giữa Home/Search/Detail. */
export function useFavorites() {
  const [favorites, setFavorites] = useState<Set<string>>(load)

  function toggle(id: string) {
    setFavorites((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      localStorage.setItem(FAV_KEY, JSON.stringify([...next]))
      return next
    })
  }

  return { favorites, toggle }
}
