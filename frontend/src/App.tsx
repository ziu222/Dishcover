import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { Login } from './screens/Login'
import { Register } from './screens/Register'
import { Home } from './screens/Home'
import { RecipeDetail } from './screens/RecipeDetail'
import { AppShell } from './components/AppShell'
import { useAuth } from './auth/AuthContext'

/** Chặn route cần đăng nhập — chưa có token thì đẩy về /login. */
function RequireAuth() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

export function App() {
  return (
    <Routes>
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Home />} />
          <Route path="/cong-thuc/:id" element={<RecipeDetail />} />
        </Route>
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
