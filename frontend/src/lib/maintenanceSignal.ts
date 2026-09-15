// Cầu nối tối giản giữa `api.ts` (không phải component) và `App.tsx` (component): khi bất kỳ
// lời gọi API nào gặp lỗi MAINTENANCE_MODE, báo cho App hiện màn bảo trì toàn trang thay vì
// từng màn tự bắt lỗi riêng lẻ.
type Listener = () => void

let listener: Listener | null = null

export function notifyMaintenanceDetected() {
  listener?.()
}

export function setMaintenanceListener(fn: Listener | null) {
  listener = fn
}
