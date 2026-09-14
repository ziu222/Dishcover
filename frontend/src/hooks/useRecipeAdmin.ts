import { useState } from 'react'
import { api, ApiError } from '../lib/api'
import type { RecipeDetail } from '../types'

/** Payload gửi lên Recipe Service — khớp CreateRecipeRequest/UpdateRecipeRequest ở backend. */
export interface RecipeInput {
  name: string
  cookTimeMinutes: number
  difficulty: string
  tags: string[]
  servings: number | null
  imageUrl: string | null
  videoUrl: string | null
  ingredients: Array<{ name: string; amount: number | null; unit: string | null; essential: boolean }>
  steps: Array<{ order: number; title: string; content: string; durationMinutes: number | null }>
}

const BASE = '/recipe-service/recipes'

/**
 * Tạo/sửa công thức cho khu quản trị.
 *
 * Cố ý KHÔNG tính `normalizedName`, `weight`, `slug` hay `nutrition` ở đây — Recipe Service đã
 * tự suy hết từ dữ liệu thô (IngredientCatalog.resolve, essential→weight 1.0/0.3, slug tự sinh,
 * RecipeNutritionCalculator). Tính lại ở frontend sẽ tạo nguồn sự thật thứ hai, lệch lúc nào
 * không biết.
 */
export function useRecipeAdmin() {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run<T>(fn: () => Promise<T>): Promise<T | null> {
    setSaving(true)
    setError(null)
    try {
      return await fn()
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.status === 403
            ? 'Tài khoản này không còn quyền quản trị. Đăng nhập lại rồi thử tiếp.'
            : err.message
          : 'Không lưu được công thức.',
      )
      return null
    } finally {
      setSaving(false)
    }
  }

  return {
    saving,
    error,
    clearError: () => setError(null),
    create: (input: RecipeInput) => run(() => api<RecipeDetail>(BASE, { method: 'POST', body: input })),
    update: (id: string, input: RecipeInput) =>
      run(() => api<RecipeDetail>(`${BASE}/${id}`, { method: 'PATCH', body: input })),
    /** Nạp chi tiết để đổ vào form lúc sửa — danh sách chỉ có summary, thiếu nguyên liệu/các bước. */
    load: (id: string) => run(() => api<RecipeDetail>(`${BASE}/${id}`)),
  }
}

/**
 * Upload ảnh cho công thức đã tồn tại.
 *
 * Không dùng `api()` được: helper đó chỉ gửi JSON, còn đây là multipart — cùng lý do
 * useImageRecognition phải tự fetch. Không tự đặt Content-Type để trình duyệt tự sinh boundary.
 */
export async function uploadRecipeImage(recipeId: string, file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`/api${BASE}/${recipeId}/image`, {
    method: 'POST',
    body: form,
    credentials: 'include',
  })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.code ?? 'UPLOAD_FAILED', body?.message ?? 'Không tải được ảnh lên.')
  }
  const saved = (await res.json()) as RecipeDetail
  return saved.imageUrl ?? ''
}
