import { Navigate, Route, Routes } from 'react-router-dom'
import { Login } from './screens/Login'
import { Register } from './screens/Register'

// Route thật được nối dần khi từng màn hoàn thiện (trang chủ...).
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
