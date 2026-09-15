import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Button } from '../components/Button'
import { useDietaryPreferences } from '../hooks/useDietaryPreferences'
import { cn } from '../lib/cn'
import type { DietaryType } from '../types'

interface Question {
  key: string
  type: DietaryType
  question: string
  choices: string[]
  /** Đáp án nghĩa là "không có gì đặc biệt" — không lưu vào hồ sơ ăn uống. */
  skipValues: string[]
}

/**
 * Nguyên văn 3 câu hỏi từ mockup gốc (Claude Design, SCREEN 12 "Onboarding") —
 * xem docs/specs/onboarding-wizard.md mục 4.2.
 */
const QUESTIONS: Question[] = [
  {
    key: 'diet',
    type: 'DIET',
    question: 'Bạn có ăn chay không?',
    choices: ['Không', 'Ăn chay', 'Thuần chay'],
    skipValues: ['Không'],
  },
  {
    key: 'allergy',
    type: 'ALLERGY',
    question: 'Bạn dị ứng với gì?',
    choices: ['Hải sản', 'Đậu phộng', 'Sữa', 'Không có'],
    skipValues: ['Không có'],
  },
  {
    key: 'household',
    type: 'HOUSEHOLD_SIZE',
    question: 'Bạn thường nấu cho mấy người?',
    choices: ['1 người', '2 người', '3 – 4 người', 'Trên 4 người'],
    skipValues: [],
  },
]

/**
 * Wizard 3 bước hiện đúng 1 lần ngay sau khi xác thực OTP lúc đăng ký (xem VerifyOtp.onSubmit).
 * Mỗi bước lưu ngay khi bấm "Tiếp tục" — bỏ qua giữa chừng vẫn giữ lại các câu đã trả lời trước
 * đó, không gom chờ tới bước cuối mới gửi 1 lần.
 */
export function Onboarding() {
  const navigate = useNavigate()
  const { add } = useDietaryPreferences()
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const current = QUESTIONS[step]
  const isLast = step === QUESTIONS.length - 1
  const selected = answers[current.key]

  function finish() {
    navigate('/', { replace: true })
  }

  async function next() {
    setSaving(true)
    try {
      if (selected && !current.skipValues.includes(selected)) {
        await add(current.type, selected)
      }
    } catch {
      // Không chặn wizard nếu lưu lỗi (CLAUDE.md mục 11: không bao giờ màn hình lỗi trắng) —
      // user vẫn thêm lại được thủ công ở Tài khoản.
    } finally {
      setSaving(false)
      if (isLast) finish()
      else setStep((s) => s + 1)
    }
  }

  return (
    <div className="grid min-h-[100dvh] place-items-center bg-bg px-6 py-10">
      <div className="flex w-full max-w-[420px] flex-col">
        <div className="mb-6 flex items-center justify-end">
          <button
            type="button"
            onClick={finish}
            className="text-[13px] font-medium text-faint transition-colors hover:text-muted"
          >
            Bỏ qua, làm sau
          </button>
        </div>

        <div className="mb-9 flex justify-center gap-2">
          {QUESTIONS.map((q, i) => (
            <motion.span
              key={q.key}
              layout
              animate={{ width: i === step ? 28 : 6 }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className={cn('h-1.5 rounded-full transition-colors', i <= step ? 'bg-accent' : 'bg-line-soft')}
            />
          ))}
        </div>

        <AnimatePresence mode="wait">
          <motion.div
            key={current.key}
            initial={{ opacity: 0, x: 16 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -16 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
          >
            <p className="mb-8 font-display text-[32px] font-light leading-[1.15] text-ink">
              {current.question}
            </p>

            <div className="flex flex-col gap-3">
              {current.choices.map((choice) => {
                const active = selected === choice
                return (
                  <button
                    key={choice}
                    type="button"
                    onClick={() => setAnswers((a) => ({ ...a, [current.key]: choice }))}
                    className={cn(
                      'rounded-2xl border px-5 py-4 text-left text-[15px] font-medium transition-colors',
                      active
                        ? 'border-accent bg-accent-wash text-accent-strong'
                        : 'border-line text-ink hover:border-mist',
                    )}
                  >
                    {choice}
                  </button>
                )
              })}
            </div>
          </motion.div>
        </AnimatePresence>

        <div className="mt-9 flex gap-3">
          {step > 0 && (
            <Button
              variant="secondary"
              disabled={saving}
              onClick={() => setStep((s) => Math.max(0, s - 1))}
            >
              ‹
            </Button>
          )}
          <Button fullWidth disabled={!selected || saving} loading={saving} onClick={() => void next()}>
            {isLast ? 'Hoàn tất' : 'Tiếp tục'}
          </Button>
        </div>
      </div>
    </div>
  )
}
