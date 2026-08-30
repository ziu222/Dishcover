import { createContext, use, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import type { User } from '../types'

const USER_KEY = 'larder.user'

function loadCachedUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  /** true trong lúc xác thực phiên lúc mở app (GET /users/me) — chưa biết đăng nhập hay chưa. */
  checking: boolean
  login: (email: string, password: string, captchaToken?: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => Promise<void>
  updateProfile: (fields: { fullName?: string; avatarUrl?: string }) => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

/**
 * Token nằm trong cookie httpOnly `auth_token` — JS không đọc được, nên không còn cách nào biết
 * "đã đăng nhập chưa" chỉ từ localStorage như trước. `user` cache ở đây chỉ để hiện UI ngay
 * (tên/avatar) trong lúc chờ GET /users/me xác nhận phiên còn hiệu lực; 401 thì coi như chưa
 * đăng nhập và xoá cache.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadCachedUser)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    let cancelled = false
    api<User>('/user-service/users/me')
      .then((u) => {
        if (cancelled) return
        setUser(u)
        localStorage.setItem(USER_KEY, JSON.stringify(u))
      })
      .catch(() => {
        if (cancelled) return
        setUser(null)
        localStorage.removeItem(USER_KEY)
      })
      .finally(() => {
        if (!cancelled) setChecking(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo<AuthState>(() => {
    function persist(u: User) {
      localStorage.setItem(USER_KEY, JSON.stringify(u))
      setUser(u)
    }

    return {
      user,
      isAuthenticated: user !== null,
      checking,
      async login(email, password, captchaToken) {
        persist(
          await api<User>('/user-service/auth/login', {
            method: 'POST',
            body: { email, password, captchaToken },
          }),
        )
      },
      async register(email, password, fullName) {
        persist(
          await api<User>('/user-service/auth/register', {
            method: 'POST',
            body: { email, password, fullName },
          }),
        )
      },
      async updateProfile(fields) {
        persist(
          await api<User>('/user-service/users/me', {
            method: 'PATCH',
            body: fields,
          }),
        )
      },
      async logout() {
        try {
          await api('/user-service/auth/logout', { method: 'POST' })
        } catch {
          // Lỗi mạng/API khi logout không nên kẹt người dùng ở trạng thái "vẫn đăng nhập" —
          // vẫn dọn state cục bộ bên dưới.
        } finally {
          localStorage.removeItem(USER_KEY)
          setUser(null)
        }
      },
    }
  }, [user, checking])

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthState {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth phải nằm trong <AuthProvider>')
  return ctx
}
