/** Nối class có điều kiện — bỏ giá trị falsy. Đủ dùng, không cần clsx (ponytail). */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ')
}
