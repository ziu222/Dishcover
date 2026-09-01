import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { ChatCircleDots, PaperPlaneTilt, Sparkle, Warning } from '@phosphor-icons/react'
import { useChat, type ChatMessage } from '../hooks/useChat'
import { useRecipes } from '../hooks/useRecipes'
import { Button } from '../components/Button'
import { cn } from '../lib/cn'
import type { RecipeSummary } from '../types'

const SUGGESTIONS = [
  'Tôi còn trứng và cà chua, nấu được món gì?',
  'Món nào nấu nhanh dưới 20 phút?',
  'Gợi ý món chay dễ làm',
]

function TypingDots() {
  return (
    <div className="flex items-center gap-1 px-1 py-1.5">
      {[0, 1, 2].map((i) => (
        <motion.span
          key={i}
          className="size-1.5 rounded-full bg-mist"
          animate={{ y: [0, -4, 0] }}
          transition={{ duration: 0.9, repeat: Infinity, delay: i * 0.15, ease: 'easeInOut' }}
        />
      ))}
    </div>
  )
}

function SourceChips({ ids, recipes }: { ids: string[]; recipes: RecipeSummary[] }) {
  if (ids.length === 0) return null
  return (
    <div className="mt-2.5 flex flex-wrap gap-1.5">
      {ids.map((id) => {
        const recipe = recipes.find((r) => r.id === id)
        return (
          <Link
            key={id}
            to={`/cong-thuc/${id}`}
            className="inline-flex items-center gap-1 rounded-full border border-line bg-surface px-2.5 py-1 text-[11.5px] font-medium text-muted transition-colors hover:border-accent hover:text-accent-strong"
          >
            {recipe?.name ?? 'Xem công thức'}
          </Link>
        )
      })}
    </div>
  )
}

function DietaryWarningCallout({ warnings }: { warnings: string[] }) {
  if (warnings.length === 0) return null
  return (
    <div className="mb-2.5 flex items-start gap-2 rounded-xl border border-amber/25 bg-amber-bg px-3 py-2.5">
      <Warning weight="fill" className="mt-0.5 size-4 shrink-0 text-amber" />
      <p className="text-[13px] font-medium leading-snug text-amber">
        Món này chứa {warnings.join(', ')} — có thể vi phạm đặc điểm ăn uống bạn đã khai báo.
      </p>
    </div>
  )
}

function Bubble({ message, recipes }: { message: ChatMessage; recipes: RecipeSummary[] }) {
  const isUser = message.role === 'user'
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', stiffness: 200, damping: 24 }}
      className={cn('flex', isUser ? 'justify-end' : 'justify-start')}
    >
      <div
        className={cn(
          'max-w-[80%] rounded-2xl px-4 py-3 text-[15px] leading-relaxed sm:max-w-[65%]',
          isUser
            ? 'rounded-br-sm bg-ink text-surface'
            : message.error
              ? 'rounded-bl-sm border border-expired/30 bg-expired-bg text-expired'
              : 'rounded-bl-sm border border-line-soft bg-white text-ink',
        )}
      >
        {message.error && (
          <span className="mb-1 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-[0.1em]">
            <Warning weight="fill" className="size-3.5" />
            Lỗi
          </span>
        )}
        {!isUser && !message.error && message.dietaryWarnings && (
          <DietaryWarningCallout warnings={message.dietaryWarnings} />
        )}
        <p className="whitespace-pre-wrap">{message.content}</p>
        {!isUser && !message.error && message.sourceRecipeIds && (
          <SourceChips ids={message.sourceRecipeIds} recipes={recipes} />
        )}
        {message.fallback && (
          <p className="mt-2 text-[11.5px] text-faint">
            Trợ lý AI tạm không phản hồi — đây là danh sách công thức khớp trực tiếp.
          </p>
        )}
      </div>
    </motion.div>
  )
}

export function Chatbot() {
  const { messages, sending, send } = useChat()
  // Nạp 1 lần ở đây rồi truyền xuống — trước đó mỗi SourceChips tự useRecipes(), N tin nhắn
  // có nguồn = N lần gọi lại GET /recipes (500 công thức) không cần thiết.
  const { recipes } = useRecipes()
  const [draft, setDraft] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, sending])

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!draft.trim() || sending) return
    void send(draft)
    setDraft('')
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit(e)
    }
  }

  return (
    // h-full (không tự tính dvh trừ header) -- <main> ở AppShell đã trừ đúng header +
    // bottom tab bar mobile qua padding-bottom, tự tính lại ở đây từng gây ô nhập bị khuất
    // sau tab bar trên mobile.
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto px-6 py-8 lg:px-10">
        {messages.length === 0 ? (
          <div className="mx-auto flex h-full max-w-md flex-col items-center justify-center text-center">
            <span className="mb-4 grid size-14 place-items-center rounded-full bg-accent-wash">
              <ChatCircleDots weight="fill" className="size-7 text-accent" />
            </span>
            <h1 className="font-display text-3xl font-extralight tracking-tight text-ink">
              Hỏi trợ lý nấu ăn
            </h1>
            <p className="mt-2 text-sm text-muted">
              Trả lời dựa trên công thức thật trong hệ thống — không bịa món.
            </p>
            <div className="mt-6 flex flex-col gap-2 self-stretch">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => void send(s)}
                  className="flex items-center gap-2 rounded-xl border border-line-soft bg-white px-4 py-3 text-left text-sm text-muted transition-colors hover:border-accent/40 hover:text-ink"
                >
                  <Sparkle className="size-4 shrink-0 text-accent" />
                  {s}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="mx-auto flex max-w-2xl flex-col gap-4">
            <AnimatePresence initial={false}>
              {messages.map((m) => (
                <Bubble key={m.id} message={m} recipes={recipes} />
              ))}
            </AnimatePresence>
            {sending && (
              <div className="flex justify-start">
                <div className="rounded-2xl rounded-bl-sm border border-line-soft bg-white px-2">
                  <TypingDots />
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>
        )}
      </div>

      <form
        onSubmit={submit}
        className="border-t border-line-soft bg-card px-6 py-4 lg:px-10"
      >
        <div className="mx-auto flex max-w-2xl items-end gap-3">
          <textarea
            rows={1}
            aria-label="Nhập câu hỏi cho trợ lý"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Bạn còn nguyên liệu gì?"
            className="max-h-32 flex-1 resize-none rounded-xl border border-line bg-white px-4 py-3 text-[15px] text-ink outline-none placeholder:text-faint focus-visible:border-accent"
          />
          <Button type="submit" size="md" disabled={!draft.trim() || sending} aria-label="Gửi">
            <PaperPlaneTilt weight="fill" className="size-4" />
          </Button>
        </div>
      </form>
    </div>
  )
}
