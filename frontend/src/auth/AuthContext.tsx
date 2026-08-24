import { createContext, use, useMemo, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '../lib/api'
import type { AuthResponse, User } from '../types'

const USER_KEY = 'larder.user'

function loadUser(): User | null {
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
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

// ponytail: JWT ở localStorage chấp nhận cho đồ án; nâng httpOnly cookie nếu cần chống XSS.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => (getToken() ? loadUser() : null))

  const value = useMemo<AuthState>(() => {
    function persist(res: AuthResponse) {
      setToken(res.token)
      localStorage.setItem(USER_KEY, JSON.stringify(res.user))
      setUser(res.user)
    }

    return {
      user,
      isAuthenticated: user !== null,
      async login(email, password) {
        persist(await api<AuthResponse>('/user-service/auth/login', {
          method: 'POST',
          body: { email, password },
        }))
      },
      async register(email, password, fullName) {
        persist(await api<AuthResponse>('/user-service/auth/register', {
          method: 'POST',
          body: { email, password, fullName },
        }))
      },
      logout() {
        setToken(null)
        localStorage.removeItem(USER_KEY)
        setUser(null)
      },
    }
  }, [user])

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthState {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth phải nằm trong <AuthProvider>')
  return ctx
}
