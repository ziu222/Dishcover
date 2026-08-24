// Điểm vào routing. Các route thật (đăng nhập/đăng ký/trang chủ) được nối dần khi từng
// màn hoàn thiện — giữ scaffold build xanh trước.
export function App() {
  return (
    <div className="grid min-h-[100dvh] place-items-center px-4">
      <div className="text-center">
        <div className="font-display text-6xl font-extralight tracking-tight text-ink">
          Larder<span className="text-accent">.</span>
        </div>
        <p className="mt-4 font-display text-lg font-light italic text-muted">
          Nấu từ những gì bạn đang có.
        </p>
      </div>
    </div>
  )
}
