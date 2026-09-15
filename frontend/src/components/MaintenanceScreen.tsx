import { motion } from 'framer-motion'
import { LarderMark } from './LarderMark'
import { Button } from './Button'

/**
 * Chặn toàn trang khi Gateway trả MAINTENANCE_MODE — thay hẳn nội dung app thay vì để từng màn
 * tự hiện lỗi rời rạc. Admin không bao giờ thấy màn này (Gateway cho JWT role ADMIN đi qua).
 */
export function MaintenanceScreen() {
  return (
    <div className="grid min-h-[100dvh] place-items-center bg-bg px-6">
      <motion.div
        className="max-w-[420px] text-center"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="mb-7 flex justify-center">
          <LarderMark size={86} draw className="text-mist [--dot:var(--color-accent)]" />
        </div>
        <p className="font-display text-2xl font-light italic text-muted sm:text-[30px] sm:leading-tight">
          Larder đang bảo trì
        </p>
        <p className="mt-2.5 text-sm leading-relaxed text-faint">
          Hệ thống tạm ngừng để cập nhật, vui lòng quay lại sau ít phút.
        </p>
        <div className="mt-7">
          <Button variant="secondary" onClick={() => window.location.reload()}>
            Thử lại
          </Button>
        </div>
      </motion.div>
    </div>
  )
}
