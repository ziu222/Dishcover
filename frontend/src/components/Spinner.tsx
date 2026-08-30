import { CookingPot } from '@phosphor-icons/react'
import { motion } from 'framer-motion'

interface SteamWispProps {
  delay: number
  xOffset: number
}

/** 1 làn hơi bốc lên — lệch pha nhau (delay khác nhau) để trông tự nhiên, không đồng bộ máy móc. */
function SteamWisp({ delay, xOffset }: SteamWispProps) {
  return (
    <motion.span
      aria-hidden
      className="absolute bottom-[85%] left-1/2 h-4 w-[3px] rounded-full bg-mist/60"
      initial={{ opacity: 0, y: 0, x: xOffset, scaleY: 0.6 }}
      animate={{
        opacity: [0, 0.9, 0],
        y: [0, -20],
        x: [xOffset, xOffset + 3, xOffset - 3],
        scaleY: [0.6, 1.15, 0.8],
      }}
      transition={{ duration: 1.8, repeat: Infinity, delay, ease: 'easeInOut' }}
    />
  )
}

/**
 * Loading — "nồi đang sôi": nồi thở nhẹ (breathing scale) + 3 làn hơi bốc lên so le phía trên,
 * đúng chủ đề nấu ăn của app thay vì vòng xoay chung chung. 3 lớp chuyển động (skill motion-design):
 * primary = hơi bốc lên, secondary = nồi phập phồng theo nhịp chậm hơn, ambient = quầng sáng ấm
 * phía sau mờ dần. Dùng khi ĐANG fetch data (chưa có gì để hiện).
 */
export function Spinner({ label = 'Đang tải công thức…' }: { label?: string }) {
  return (
    <div className="flex flex-col items-center gap-5 py-24" role="status" aria-live="polite">
      <div className="relative flex h-16 w-16 items-end justify-center">
        {/* ambient: quầng ấm phía sau, mờ dần rồi rõ lại chậm rãi */}
        <motion.span
          aria-hidden
          className="absolute inset-0 rounded-full bg-accent-wash"
          animate={{ opacity: [0.3, 0.6, 0.3], scale: [0.85, 1, 0.85] }}
          transition={{ duration: 2.4, repeat: Infinity, ease: 'easeInOut' }}
        />
        <SteamWisp delay={0} xOffset={-7} />
        <SteamWisp delay={0.45} xOffset={1} />
        <SteamWisp delay={0.9} xOffset={8} />
        <motion.div
          className="relative z-10"
          animate={{ scale: [1, 1.07, 1] }}
          transition={{ duration: 1.6, repeat: Infinity, ease: 'easeInOut' }}
        >
          <CookingPot weight="fill" className="size-9 text-accent" />
        </motion.div>
      </div>
      <span className="text-sm text-muted">{label}</span>
    </div>
  )
}
