// fetch wrapper mỏng cho Gateway. Frontend luôn gọi path tương đối bắt đầu bằng `/api`
// (dev proxy chuyển sang http://localhost:8080 và cắt `/api` — xem vite.config.ts).
//
// Auth: token nằm trong cookie httpOnly `auth_token` (đặt bởi User Service qua Set-Cookie).
// JS không đọc/set được cookie này — trình duyệt tự đính kèm mọi request cùng-origin nhờ
// `credentials: 'include'`, không cần frontend tự quản lý Authorization header nữa.

/** Lỗi API — mang mã lỗi backend ({code, message, traceId}) để UI hiển thị đúng. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
  /** query params — undefined/null/'' bị bỏ qua */
  params?: Record<string, string | number | undefined | null>
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const url = new URL(`/api${path}`, window.location.origin)
  if (opts.params) {
    for (const [k, v] of Object.entries(opts.params)) {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v))
    }
  }

  const headers: Record<string, string> = {}
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json'

  let res: Response
  try {
    res = await fetch(url.pathname + url.search, {
      method: opts.method ?? 'GET',
      headers,
      credentials: 'include',
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    })
  } catch {
    // mất mạng / Gateway sập — không để màn hình trắng (CLAUDE.md mục 11)
    throw new ApiError(0, 'NETWORK', 'Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại.')
  }

  if (res.status === 204) return undefined as T

  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    const code = data?.code ?? `HTTP_${res.status}`
    const message = data?.message ?? 'Đã có lỗi xảy ra, vui lòng thử lại.'
    throw new ApiError(res.status, code, message)
  }
  return data as T
}
