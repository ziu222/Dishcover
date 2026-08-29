import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { Login } from './screens/Login'
import { Register } from './screens/Register'
import { Home } from './screens/Home'
import { Search } from './screens/Search'
import { Fridge } from './screens/Fridge'
import { Matching } from './screens/Matching'
import { Chatbot } from './screens/Chatbot'
import { Account } from './screens/Account'
import { RecipeDetail } from './screens/RecipeDetail'
import { AppShell } from './components/AppShell'
import { useAuth } from './auth/AuthContext'

/**
 * Chặn route cần đăng nhập. Token nằm trong cookie httpOnly nên không biết được ngay từ
 * localStorage — phải chờ AuthProvider gọi xong GET /users/me (checking) rồi mới quyết định,
 * tránh vừa lóe nội dung bảo vệ vừa văng về /login khi phiên đã hết hạn.
 */
function RequireAuth() {
  const { isAuthenticated, checking } = useAuth()
  if (checking) {
    return <div className="grid min-h-[100dvh] place-items-center text-sm text-muted">Đang tải…</div>
  }
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

export function App() {
  return (
    <Routes>
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Home />} />
          <Route path="/tim-kiem" element={<Search />} />
          <Route path="/tu-lanh" element={<Fridge />} />
          <Route path="/goi-y" element={<Matching />} />
          <Route path="/chatbot" element={<Chatbot />} />
          <Route path="/tai-khoan" element={<Account />} />
          <Route path="/cong-thuc/:id" element={<RecipeDetail />} />
        </Route>
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
