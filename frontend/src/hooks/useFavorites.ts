import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'

// ponytail: yêu thích chỉ lưu localStorage — backend chưa có endpoint favorites.
// Khi có API thì đổi bên trong hook này, các màn dùng không phải sửa.
function keyFor(userId: number | undefined): string {
  return `larder.favorites.${userId ?? 'anon'}`
}

function load(key: string): Set<string> {
  try {
    return new Set(JSON.parse(localStorage.getItem(key) ?? '[]') as string[])
  } catch {
    return new Set()
  }
}

/** Danh sách công thức yêu thích, dùng chung giữa Home/Search/Detail.
 *  Khoá theo userId — tránh rò/trộn dữ liệu giữa 2 tài khoản dùng chung 1 trình duyệt. */
export function useFavorites() {
  const { user } = useAuth()
  const key = keyFor(user?.id)
  const [favorites, setFavorites] = useState<Set<string>>(() => load(key))

  // user đổi (đăng xuất rồi đăng nhập tài khoản khác) -> nạp lại đúng khoá mới.
  useEffect(() => {
    setFavorites(load(key))
  }, [key])

  function toggle(id: string) {
    setFavorites((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      localStorage.setItem(key, JSON.stringify([...next]))
      return next
    })
  }

  return { favorites, toggle }
}
