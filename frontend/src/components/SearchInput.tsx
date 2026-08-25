import { MagnifyingGlass, X } from '@phosphor-icons/react'

interface SearchInputProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  autoFocus?: boolean
}

/** Ô tìm kiếm dùng chung: icon trái + nút xoá phải khi có nội dung. */
export function SearchInput({
  value,
  onChange,
  placeholder = 'Tìm món ăn theo tên...',
  autoFocus,
}: SearchInputProps) {
  return (
    <div className="relative">
      <MagnifyingGlass className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-mist" />
      <input
        type="text"
        role="searchbox"
        enterKeyHint="search"
        autoComplete="off"
        autoFocus={autoFocus}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-full border border-line bg-card py-3.5 pl-12 pr-11 text-[15px] text-ink outline-none transition-colors placeholder:text-mist focus:border-accent focus:ring-2 focus:ring-accent/15"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Xoá tìm kiếm"
          className="absolute right-3 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded-full text-mist transition-colors hover:bg-line-soft hover:text-muted"
        >
          <X className="size-4" />
        </button>
      )}
    </div>
  )
}
