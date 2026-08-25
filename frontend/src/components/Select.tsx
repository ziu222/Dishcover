import { useEffect, useId, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { CaretDown, Check } from '@phosphor-icons/react'
import { cn } from '../lib/cn'

export interface SelectOption {
  value: string
  label: string
}

interface SelectProps {
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  ariaLabel?: string
}

/** Dropdown lọc — custom listbox (thay native <select> xấu). Popover bo góc, nền kem, mục chọn
 *  đánh dấu accent + tick; mở/đóng bằng spring; điều hướng bàn phím + click-ra-ngoài + Escape. */
export function Select({ value, onChange, options, ariaLabel }: SelectProps) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const rootRef = useRef<HTMLDivElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const listId = useId()

  const selected = options.find((o) => o.value === value) ?? options[0]

  // Đóng khi click ra ngoài.
  useEffect(() => {
    if (!open) return
    function onDown(e: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  // Mở tới đúng mục đang chọn + cuộn mục active vào tầm nhìn.
  useEffect(() => {
    if (!open) return
    setActive(Math.max(0, options.findIndex((o) => o.value === value)))
  }, [open, value, options])

  useEffect(() => {
    if (!open) return
    listRef.current?.querySelector<HTMLElement>(`[data-idx="${active}"]`)?.scrollIntoView({
      block: 'nearest',
    })
  }, [open, active])

  function choose(v: string) {
    onChange(v)
    setOpen(false)
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (!open && (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault()
      setOpen(true)
      return
    }
    if (!open) return
    if (e.key === 'Escape') setOpen(false)
    else if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActive((i) => Math.min(options.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((i) => Math.max(0, i - 1))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      choose(options[active].value)
    }
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={onKeyDown}
        className={cn(
          'inline-flex min-w-[190px] items-center justify-between gap-3 rounded-full border bg-surface',
          'py-2.5 pl-5 pr-4 text-[13px] font-medium text-ink outline-none transition-colors',
          'focus-visible:ring-2 focus-visible:ring-accent/40',
          open ? 'border-accent' : 'border-line hover:border-mist',
        )}
      >
        <span className="truncate">{selected?.label}</span>
        <motion.span animate={{ rotate: open ? 180 : 0 }} transition={{ duration: 0.2 }}>
          <CaretDown weight="bold" className="size-3.5 text-faint" />
        </motion.span>
      </button>

      <AnimatePresence>
        {open && (
          <motion.ul
            ref={listRef}
            id={listId}
            role="listbox"
            aria-label={ariaLabel}
            initial={{ opacity: 0, y: -8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.98 }}
            transition={{ type: 'spring', stiffness: 420, damping: 32 }}
            style={{ transformOrigin: 'top right' }}
            className="absolute right-0 z-30 mt-2 max-h-[340px] min-w-full overflow-y-auto rounded-2xl border border-line-soft bg-card p-1.5 shadow-[0_24px_50px_-20px_rgba(40,34,24,0.35)]"
          >
            {options.map((o, i) => {
              const isSel = o.value === value
              return (
                <li key={o.value} role="option" aria-selected={isSel} data-idx={i}>
                  <button
                    type="button"
                    onClick={() => choose(o.value)}
                    onMouseEnter={() => setActive(i)}
                    className={cn(
                      'flex w-full items-center justify-between gap-3 whitespace-nowrap rounded-xl px-3.5 py-2 text-left text-[13.5px] transition-colors',
                      i === active ? 'bg-line-soft/60' : 'bg-transparent',
                      isSel ? 'font-medium text-accent-strong' : 'text-muted',
                    )}
                  >
                    {o.label}
                    {isSel && <Check weight="bold" className="size-4 shrink-0 text-accent" />}
                  </button>
                </li>
              )
            })}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  )
}
