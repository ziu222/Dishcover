import { useCallback, useState } from 'react'
import { ApiError } from '../lib/api'
import type { RecognizedIngredient } from '../types'

interface RecognizeResponse {
  items: RecognizedIngredient[]
}

/** Nhận diện nguyên liệu từ ảnh — POST multipart, KHÔNG dùng chung api.ts vì đó luôn set
 *  Content-Type: application/json (multipart cần browser tự sinh boundary). */
async function recognizeImage(file: File): Promise<RecognizedIngredient[]> {
  const form = new FormData()
  form.append('file', file)

  let res: Response
  try {
    res = await fetch('/api/image-service/recognize', {
      method: 'POST',
      credentials: 'include',
      body: form,
    })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại.')
  }

  const text = await res.text()
  let data: { code?: string; message?: string; items?: RecognizedIngredient[] } | null = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!res.ok) {
    const code = data?.code ?? `HTTP_${res.status}`
    const message = data?.message ?? 'Nhận diện ảnh tạm thời không khả dụng, bạn có thể nhập tay.'
    throw new ApiError(res.status, code, message)
  }
  return (data as RecognizeResponse | null)?.items ?? []
}

/** Trạng thái + hành động cho luồng "chụp ảnh → xác nhận" (human-in-the-loop, không tự ghi DB). */
export function useImageRecognition() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [items, setItems] = useState<RecognizedIngredient[] | null>(null)

  const recognize = useCallback(async (file: File) => {
    setLoading(true)
    setError(null)
    try {
      setItems(await recognizeImage(file))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Nhận diện ảnh thất bại, thử lại.')
      setItems(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const reset = useCallback(() => {
    setItems(null)
    setError(null)
    setLoading(false)
  }, [])

  return { loading, error, items, recognize, reset }
}
