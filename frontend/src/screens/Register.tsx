import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Warning } from '@phosphor-icons/react'
import { AuthLayout } from '../components/AuthLayout'
import { Field } from '../components/Field'
import { Button } from '../components/Button'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'

export function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pwError, setPwError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setPwError(null)
    if (password.length < 6) {
      setPwError('Mật khẩu cần ít nhất 6 ký tự.')
      return
    }
    setLoading(true)
    try {
      await register(email.trim(), password, fullName.trim())
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Đăng ký thất bại, vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Tạo tài khoản"
      subtitle="Vài giây để bắt đầu nấu từ những gì bạn đang có."
      footer={
        <>
          Đã có tài khoản?{' '}
          <Link to="/login" className="font-medium text-accent hover:text-accent-strong">
            Đăng nhập
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
        <Field
          label="Họ tên"
          type="text"
          autoComplete="name"
          placeholder="Nguyễn Minh"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
        />
        <Field
          label="Email"
          type="email"
          autoComplete="email"
          required
          placeholder="ban@vidu.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Field
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          required
          placeholder="••••••••"
          helperText="Ít nhất 6 ký tự"
          error={pwError ?? undefined}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && (
          <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <Button type="submit" size="lg" fullWidth loading={loading} className="mt-1">
          Đăng ký
        </Button>
      </form>
    </AuthLayout>
  )
}
