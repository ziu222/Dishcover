import { lazy, Suspense } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { Login } from './screens/Login'
import { Register } from './screens/Register'
import { VerifyOtp } from './screens/VerifyOtp'
import { useAuth } from './auth/AuthContext'
import { Landing } from './screens/Landing'

const Home = lazy(() => import('./screens/Home').then((module) => ({ default: module.Home })))
const Search = lazy(() => import('./screens/Search').then((module) => ({ default: module.Search })))
const Fridge = lazy(() => import('./screens/Fridge').then((module) => ({ default: module.Fridge })))
const Matching = lazy(() =>
  import('./screens/Matching').then((module) => ({ default: module.Matching })),
)
const Chatbot = lazy(() =>
  import('./screens/Chatbot').then((module) => ({ default: module.Chatbot })),
)
const Account = lazy(() =>
  import('./screens/Account').then((module) => ({ default: module.Account })),
)
const RecipeDetail = lazy(() =>
  import('./screens/RecipeDetail').then((module) => ({ default: module.RecipeDetail })),
)
const About = lazy(() => import('./screens/About').then((module) => ({ default: module.About })))
const AdminRecipes = lazy(() =>
  import('./screens/AdminRecipes').then((module) => ({ default: module.AdminRecipes })),
)
const AdminUsers = lazy(() =>
  import('./screens/AdminUsers').then((module) => ({ default: module.AdminUsers })),
)
const AppShell = lazy(() =>
  import('./components/AppShell').then((module) => ({ default: module.AppShell })),
)

/**
 * Chặn route cần đăng nhập. Token nằm trong cookie httpOnly nên không biết được ngay từ
 * localStorage — phải chờ AuthProvider gọi xong GET /users/me (checking) rồi mới quyết định,
 * tránh vừa lóe nội dung bảo vệ vừa văng về /login khi phiên đã hết hạn.
 */
function RequireAuth() {
  const { isAuthenticated, checking } = useAuth()
  if (checking) {
    return (
      <div className="grid min-h-[100dvh] place-items-center text-sm text-muted">Đang tải…</div>
    )
  }
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

/**
 * Chặn route chỉ dành cho admin. Chỉ gate HIỂN THỊ — quyền thật nằm ở Recipe Service
 * (`hasRole("ADMIN")` trên POST/PATCH/DELETE /recipes), nên user tự sửa state trong trình
 * duyệt cũng chỉ thấy được cái vỏ màn, mọi thao tác ghi vẫn bị backend trả 403.
 * Không phải admin thì đưa về trang chủ chứ không phải /login — họ đã đăng nhập rồi.
 */
function RequireAdmin() {
  const { user, isAuthenticated, checking } = useAuth()
  if (checking) {
    return (
      <div className="grid min-h-[100dvh] place-items-center text-sm text-muted">Đang tải…</div>
    )
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return user?.role === 'ADMIN' ? <Outlet /> : <Navigate to="/" replace />
}

export function App() {
  return (
    <Suspense
      fallback={
        <div role="status" className="grid min-h-[100dvh] place-items-center text-sm text-muted">
          Đang tải…
        </div>
      }
    >
      <Routes>
        <Route path="/" element={<EntryPage />}>
          <Route element={<AppShell />}>
            <Route index element={<Home />} />
          </Route>
        </Route>
        <Route path="/landing" element={<Landing />} />
        <Route path="/ve-chung-toi" element={<About />} />
        <Route element={<RequireAdmin />}>
          <Route element={<AppShell />}>
            <Route path="/admin/cong-thuc" element={<AdminRecipes />} />
            <Route path="/admin/nguoi-dung" element={<AdminUsers />} />
          </Route>
        </Route>
        <Route element={<RequireAuth />}>
          <Route element={<AppShell />}>
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
        <Route path="/xac-thuc-otp" element={<VerifyOtp />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}

function EntryPage() {
  const { isAuthenticated, checking } = useAuth()
  // Public content can render while the server verifies the session.
  return !checking && isAuthenticated ? <Outlet /> : <Landing />
}
