import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Warning } from '@phosphor-icons/react'
import { AuthLayout } from '../components/AuthLayout'
import { Field } from '../components/Field'
import { Button } from '../components/Button'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'

const RESEND_COOLDOWN_SECONDS = 60

export function VerifyOtp() {
  const { verifyOtp, resendOtp } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const email = searchParams.get('email') ?? ''
  const [otp, setOtp] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [cooldown, setCooldown] = useState(0)

  function startCooldown() {
    setCooldown(RESEND_COOLDOWN_SECONDS)
    const id = setInterval(() => {
      setCooldown((s) => {
        if (s <= 1) {
          clearInterval(id)
          return 0
        }
        return s - 1
      })
    }, 1000)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await verifyOtp(email, otp)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Xác thực thất bại, vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }

  async function onResend() {
    setError(null)
    setInfo(null)
    try {
      await resendOtp(email)
      setInfo('Đã gửi lại mã xác thực.')
      startCooldown()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không gửi lại được, thử lại sau.')
    }
  }

  return (
    <AuthLayout
      title="Xác thực email"
      subtitle={`Nhập mã 6 số vừa gửi tới ${email}.`}
      footer={
        <>
          Sai email?{' '}
          <Link to="/register" className="font-medium text-accent hover:text-accent-strong">
            Đăng ký lại
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
        <Field
          label="Mã xác thực"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          required
          placeholder="123456"
          maxLength={6}
          value={otp}
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
        />

        {info && <p className="text-sm text-accent">{info}</p>}

        {error && (
          <div className="flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <Button type="submit" size="lg" fullWidth loading={loading} disabled={otp.length !== 6} className="mt-1">
          Xác thực
        </Button>

        <button
          type="button"
          onClick={onResend}
          disabled={cooldown > 0}
          className="text-sm text-accent hover:text-accent-strong disabled:cursor-not-allowed disabled:text-mist"
        >
          {cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã'}
        </button>
      </form>
    </AuthLayout>
  )
}
