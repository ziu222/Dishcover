import { useEffect, useRef } from 'react'

// Site key KHÔNG phải bí mật (Cloudflare khuyến khích nhúng thẳng vào JS phía client) — default
// là site key TEST chính thức, luôn hiện widget "pass", đủ chạy dev không cần tài khoản Cloudflare.
const SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY ?? '1x00000000000000000000AA'
const SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js'

type TurnstileWidgetId = string

interface TurnstileGlobal {
  render: (
    container: HTMLElement,
    options: { sitekey: string; callback: (token: string) => void; 'error-callback'?: () => void },
  ) => TurnstileWidgetId
  remove: (widgetId: TurnstileWidgetId) => void
}

declare global {
  interface Window {
    turnstile?: TurnstileGlobal
  }
}

let scriptLoadPromise: Promise<void> | null = null

/** Tải script Turnstile đúng 1 lần dù component render lại nhiều lần / mount nhiều nơi. */
function loadScript(): Promise<void> {
  if (window.turnstile) return Promise.resolve()
  if (!scriptLoadPromise) {
    scriptLoadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = SCRIPT_SRC
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('Không tải được Turnstile'))
      document.head.appendChild(script)
    })
  }
  return scriptLoadPromise
}

interface TurnstileWidgetProps {
  onVerify: (token: string) => void
}

/** Widget CAPTCHA Cloudflare Turnstile — chỉ hiện khi backend báo CAPTCHA_REQUIRED (Login.tsx). */
export function TurnstileWidget({ onVerify }: TurnstileWidgetProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let widgetId: TurnstileWidgetId | null = null
    let cancelled = false

    loadScript().then(() => {
      if (cancelled || !containerRef.current || !window.turnstile) return
      widgetId = window.turnstile.render(containerRef.current, {
        sitekey: SITE_KEY,
        callback: onVerify,
      })
    })

    return () => {
      cancelled = true
      if (widgetId && window.turnstile) window.turnstile.remove(widgetId)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- onVerify đổi mỗi render (closure), chỉ cần mount 1 lần
  }, [])

  return <div ref={containerRef} />
}
