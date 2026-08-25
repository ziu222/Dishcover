import { CaretDown } from '@phosphor-icons/react'

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

/** Dropdown lọc dùng chung — native <select> tạo kiểu (accessible, gọn) + mũi tên Phosphor. */
export function Select({ value, onChange, options, ariaLabel }: SelectProps) {
  return (
    <div className="relative inline-flex">
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={ariaLabel}
        className="cursor-pointer appearance-none rounded-full border border-line bg-surface py-2.5 pl-4 pr-10 text-[13px] font-medium text-ink outline-none transition-colors hover:border-mist focus:border-accent focus:ring-2 focus:ring-accent/15"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <CaretDown
        weight="bold"
        className="pointer-events-none absolute right-3.5 top-1/2 size-3.5 -translate-y-1/2 text-faint"
      />
    </div>
  )
}
