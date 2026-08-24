import { useId, useState, type ComponentPropsWithoutRef } from 'react'
import { Eye, EyeSlash } from '@phosphor-icons/react'
import { cn } from '../lib/cn'

interface FieldProps extends Omit<ComponentPropsWithoutRef<'input'>, 'id'> {
  label: string
  error?: string
  helperText?: string
}

/** Ô nhập dùng chung: label trên · input · helper/error dưới (CLAUDE.md/skill Rule 6).
 *  type="password" tự thêm nút hiện/ẩn. */
export function Field({ label, error, helperText, type, className, ...rest }: FieldProps) {
  const id = useId()
  const [reveal, setReveal] = useState(false)
  const isPassword = type === 'password'
  const inputType = isPassword && reveal ? 'text' : type

  return (
    <div className="flex flex-col gap-2">
      <label
        htmlFor={id}
        className="text-[11px] font-semibold uppercase tracking-[0.14em] text-faint"
      >
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          type={inputType}
          aria-invalid={error ? true : undefined}
          className={cn(
            'w-full rounded-xl bg-card px-4 py-3 text-[15px] text-ink placeholder:text-mist',
            'border outline-none transition-colors',
            'focus:border-accent focus:ring-2 focus:ring-accent/15',
            error ? 'border-expired' : 'border-line',
            isPassword && 'pr-11',
            className,
          )}
          {...rest}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setReveal((v) => !v)}
            aria-label={reveal ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-mist hover:text-muted"
          >
            {reveal ? <EyeSlash className="size-5" /> : <Eye className="size-5" />}
          </button>
        )}
      </div>
      {error ? (
        <p className="text-xs text-expired">{error}</p>
      ) : helperText ? (
        <p className="text-xs text-mist">{helperText}</p>
      ) : null}
    </div>
  )
}
