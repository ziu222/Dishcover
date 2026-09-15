import { Warning, Wrench } from '@phosphor-icons/react'
import { AdminNav } from '../components/AdminNav'
import { Button } from '../components/Button'
import { Spinner } from '../components/Spinner'
import { useMaintenance } from '../hooks/useMaintenance'

/**
 * Bật/tắt bảo trì toàn hệ thống. Cờ sống ở Gateway (bộ nhớ, không DB — xem javadoc
 * MaintenanceState phía backend), nên tắt/mở ở đây có hiệu lực ngay cho MỌI request, không chỉ
 * frontend: khi bật, Gateway trả 503 cho mọi người dùng thường, chỉ admin (JWT role ADMIN) và
 * đăng nhập còn đi qua được.
 */
export function AdminMaintenance() {
  const { enabled, loading, error, toggling, reload, toggle } = useMaintenance()

  return (
    <div className="px-6 py-9 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <AdminNav
          title="Chế độ bảo trì"
          description="Khi bật, mọi người dùng thường nhận thông báo bảo trì; chỉ quản trị viên còn thao tác được bình thường."
        />

        {error && (
          <div className="mt-5 flex items-start gap-2 rounded-xl border border-expired/30 bg-expired-bg px-4 py-3 text-sm text-expired">
            <Warning weight="fill" className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
            <Button variant="secondary" size="md" className="ml-auto shrink-0" onClick={reload}>
              Thử lại
            </Button>
          </div>
        )}

        {loading ? (
          <Spinner label="Đang tải trạng thái bảo trì…" />
        ) : (
          enabled !== null && (
            <div className="mt-7 flex items-center justify-between gap-4 rounded-2xl border border-line-soft px-6 py-5">
              <div className="flex items-center gap-3">
                <span
                  className={
                    'grid size-11 shrink-0 place-items-center rounded-full ' +
                    (enabled ? 'bg-expired-bg text-expired' : 'bg-accent-wash text-accent-strong')
                  }
                >
                  <Wrench weight="fill" className="size-5" />
                </span>
                <div>
                  <p className="text-[15px] font-medium text-ink">
                    {enabled ? 'Đang bảo trì' : 'Hệ thống hoạt động bình thường'}
                  </p>
                  <p className="mt-0.5 text-[13px] text-faint">
                    {enabled
                      ? 'Người dùng thường đang bị chặn ở mọi API.'
                      : 'Tắt để chặn truy cập khi cần bảo trì.'}
                  </p>
                </div>
              </div>
              <Button
                variant={enabled ? 'secondary' : 'primary'}
                disabled={toggling}
                onClick={() => void toggle(!enabled)}
              >
                {toggling ? 'Đang đổi…' : enabled ? 'Tắt bảo trì' : 'Bật bảo trì'}
              </Button>
            </div>
          )
        )}
      </div>
    </div>
  )
}
