import { Navigate, Route, Routes } from 'react-router-dom'
import { Login } from './screens/Login'

// Route thật được nối dần khi từng màn hoàn thiện (đăng ký, trang chủ...).
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
