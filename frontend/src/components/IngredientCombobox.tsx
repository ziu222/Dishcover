import { useId, useRef, useState, type ComponentPropsWithoutRef, type KeyboardEvent } from 'react'
import { motion } from 'framer-motion'
import { cn } from '../lib/cn'

interface IngredientComboboxProps
  extends Omit<ComponentPropsWithoutRef<'input'>, 'id' | 'onChange' | 'value'> {
  label: string
  value: string
  onChange: (value: string) => void
  suggestions: string[]
}

const MAX_SUGGESTIONS = 8

/** Bỏ dấu tiếng Việt để so khớp không phân biệt dấu — gõ "ca" vẫn ra "Cà chua"/"Cá hồi". */
function fold(s: string): string {
  return s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

/** Ô nhập tên nguyên liệu có gợi ý từ Ingredient Catalog (autocomplete) — vẫn cho gõ tự do,
 *  gợi ý chỉ hỗ trợ gõ nhanh/tránh sai chính tả, không bắt buộc chọn từ danh sách. */
export function IngredientCombobox({
  label,
  value,
  onChange,
  suggestions,
  className,
  ...rest
}: IngredientComboboxProps) {
  const id = useId()
  const listId = useId()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const rootRef = useRef<HTMLDivElement>(null)

  const query = fold(value.trim())
  const matches =
    query.length === 0
      ? []
      : suggestions
          .filter((s) => fold(s).includes(query))
          // Ưu tiên tên BẮT ĐẦU bằng từ khoá lên trước — gõ "ca" ra "Cà chua"/"Cá hồi" ngay,
          // thay vì các từ chỉ TÌNH CỜ chứa "ca" ở giữa (VD "Bông cải").
          .sort((a, b) => Number(!fold(a).startsWith(query)) - Number(!fold(b).startsWith(query)))
          .slice(0, MAX_SUGGESTIONS)
  const showList = open && matches.length > 0

  function choose(name: string) {
    onChange(name)
    setOpen(false)
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (!showList) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActive((i) => Math.min(matches.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((i) => Math.max(0, i - 1))
    } else if (e.key === 'Enter' && matches[active]) {
      e.preventDefault()
      choose(matches[active])
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div ref={rootRef} className="relative flex flex-col gap-2">
      <label htmlFor={id} className="text-[11px] font-semibold uppercase tracking-[0.14em] text-faint">
        {label}
      </label>
      <input
        id={id}
        role="combobox"
        aria-expanded={showList}
        aria-controls={listId}
        aria-autocomplete="list"
        autoComplete="off"
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
          setActive(0)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 120)}
        onKeyDown={onKeyDown}
        className={cn(
          'w-full rounded-xl bg-card px-4 py-3 text-[15px] text-ink placeholder:text-mist',
          'border border-line outline-none transition-colors',
          'focus:border-accent focus:ring-2 focus:ring-accent/15',
          className,
        )}
        {...rest}
      />

      {showList && (
        <motion.ul
          id={listId}
          role="listbox"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.14 }}
          className="absolute left-0 right-0 top-full z-30 mt-1.5 max-h-56 overflow-y-auto rounded-xl border border-line-soft bg-card p-1.5 shadow-[0_20px_40px_-16px_rgba(40,34,24,0.3)]"
        >
          {matches.map((name, i) => (
            <li key={name} role="option" aria-selected={i === active}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()} // giữ focus input để onBlur không đóng trước khi onClick chạy
                onClick={() => choose(name)}
                onMouseEnter={() => setActive(i)}
                className={cn(
                  'w-full rounded-lg px-3 py-2 text-left text-[14px] transition-colors',
                  i === active ? 'bg-line-soft/60 text-ink' : 'text-muted',
                )}
              >
                {name}
              </button>
            </li>
          ))}
        </motion.ul>
      )}
    </div>
  )
}
