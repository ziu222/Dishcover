import { Component, type ReactNode } from 'react'
import { Button } from './Button'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/** Chặn crash render lan ra trắng toàn trang — hiện màn lỗi có nút tải lại thay vì màn hình trắng. */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: { componentStack: string }) {
    console.error('Lỗi render không bắt được:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="grid min-h-[100dvh] place-items-center bg-bg px-6 text-center">
          <div>
            <p className="font-display text-2xl font-light text-ink">Đã có lỗi xảy ra</p>
            <p className="mt-2 text-sm text-muted">Vui lòng tải lại trang để tiếp tục.</p>
            <Button className="mt-5" onClick={() => window.location.reload()}>
              Tải lại
            </Button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
