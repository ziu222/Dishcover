import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Warning } from '@phosphor-icons/react'
import { AuthLayout } from '../components/AuthLayout'
import { Field } from '../components/Field'
import { Button } from '../components/Button'
import { TurnstileWidget } from '../components/Turnstile'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  // Sai đủ 3 lần liên tiếp thì backend bắt kèm CAPTCHA (xem AuthController) — widget chỉ hiện
  // từ lúc đó, không làm phiền lượt đăng nhập bình thường.
  const [needsCaptcha, setNeedsCaptcha] = useState(false)
  const [captchaToken, setCaptchaToken] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email.trim(), password, captchaToken ?? undefined)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'CAPTCHA_REQUIRED') {
        setNeedsCaptcha(true)
        setCaptchaToken(null) // token cũ (nếu có) đã bị bác — bắt xác minh lại
      }
      setError(err instanceof ApiError ? err.message : 'Đăng nhập thất bại, vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Chào mừng trở lại"
      subtitle="Đăng nhập để xem gợi ý món ăn từ tủ lạnh của bạn."
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-medium text-accent hover:text-accent-strong">
            Đăng ký
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
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
          autoComplete="current-password"
          required
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {needsCaptcha && <TurnstileWidget onVerify={setCaptchaToken} />}

        {error && (
          <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <Button
          type="submit"
          size="lg"
          fullWidth
          loading={loading}
          disabled={needsCaptcha && !captchaToken}
          className="mt-1"
        >
          Đăng nhập
        </Button>
      </form>
    </AuthLayout>
  )
}
