import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AnimatePresence,
  motion,
  useMotionValueEvent,
  useScroll,
  useSpring,
} from 'framer-motion'
import { ArrowUpRight, GithubLogo, List, PaperPlaneTilt, X } from '@phosphor-icons/react'
import { useAuth } from '../auth/AuthContext'
import { LarderMark } from './LarderMark'
import { ease, fadeUp, group, spring } from '../screens/landingMotion'

/* Khung chung của các trang công khai (Landing + Về chúng tôi): thanh điều hướng dính
   trên đỉnh và chân trang. Tách ra khỏi Landing.tsx để 2 trang dùng đúng một bản, đổi
   một chỗ là cả hai đổi theo. */

const navLinks = [
  { href: '/#cach-hoat-dong', label: 'Cách hoạt động' },
  { href: '/#cong-thuc', label: 'Cảm hứng vào bếp' },
  { href: '/#ve-larder', label: 'Về Larder' },
]

export const REPO_URL = 'https://github.com/ziu222/Dishcover'
export const CONTACT_EMAIL = 'btn2812@gmail.com'

export function LandingHeader() {
  const { isAuthenticated, checking } = useAuth()
  const signedIn = isAuthenticated && !checking
  const [menuOpen, setMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  // Thanh tiến độ đọc + đổi trạng thái header khi rời khỏi đỉnh trang.
  const { scrollYProgress, scrollY } = useScroll()
  const progress = useSpring(scrollYProgress, { stiffness: 140, damping: 30, mass: 0.3 })
  useMotionValueEvent(scrollY, 'change', (value) => setScrolled(value > 24))

  return (
    <header className={`landing-header${scrolled ? ' landing-header-scrolled' : ''}`}>
      <div className="landing-container landing-nav">
        <Link className="landing-logo" to="/" aria-label="Larder, trang chủ">
          <LarderMark size={21} />
          <span className="landing-wordmark">
            Larder<span>.</span>
          </span>
        </Link>
        <nav className="landing-desktop-nav" aria-label="Điều hướng trang giới thiệu">
          {navLinks.map(({ href, label }) => (
            <a key={href} href={href}>
              {label}
            </a>
          ))}
          <Link to="/ve-chung-toi">Về chúng tôi</Link>
        </nav>
        <div className="landing-nav-actions">
          <Link className="landing-login" to={signedIn ? '/' : '/login'}>
            {signedIn ? 'Vào bếp' : 'Đăng nhập'}
            <ArrowUpRight size={17} />
          </Link>
          <button
            className="landing-icon-button landing-menu-toggle"
            aria-label={menuOpen ? 'Đóng menu' : 'Mở menu'}
            aria-expanded={menuOpen}
            aria-controls="landing-mobile-nav"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <AnimatePresence initial={false} mode="wait">
              <motion.span
                key={menuOpen ? 'close' : 'open'}
                className="landing-icon-swap"
                initial={{ opacity: 0, rotate: menuOpen ? -60 : 60, scale: 0.7 }}
                animate={{ opacity: 1, rotate: 0, scale: 1 }}
                exit={{ opacity: 0, rotate: menuOpen ? 60 : -60, scale: 0.7 }}
                transition={spring}
              >
                {menuOpen ? <X size={23} /> : <List size={23} />}
              </motion.span>
            </AnimatePresence>
          </button>
        </div>
      </div>
      <AnimatePresence initial={false}>
        {menuOpen && (
          <motion.nav
            id="landing-mobile-nav"
            className="landing-mobile-nav"
            aria-label="Điều hướng trên điện thoại"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.42, ease }}
            onKeyDown={(event) => {
              if (event.key === 'Escape') setMenuOpen(false)
            }}
          >
            <motion.div
              className="landing-mobile-nav-inner"
              variants={group(0.06, 0.08)}
              initial="hidden"
              animate="show"
            >
              {navLinks.map(({ href, label }) => (
                <motion.a key={href} href={href} variants={fadeUp} onClick={() => setMenuOpen(false)}>
                  {label}
                </motion.a>
              ))}
              <motion.div variants={fadeUp}>
                <Link to="/ve-chung-toi" onClick={() => setMenuOpen(false)}>
                  Về chúng tôi
                </Link>
              </motion.div>
            </motion.div>
          </motion.nav>
        )}
      </AnimatePresence>
      <motion.div className="landing-progress" style={{ scaleX: progress }} aria-hidden="true" />
    </header>
  )
}

export function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="landing-container landing-footer-inner">
        <div className="landing-footer-brand">
          <Link className="landing-logo" to="/">
            <LarderMark size={19} />
            <span className="landing-wordmark">
              Larder<span>.</span>
            </span>
          </Link>
          <p>
            Nấu ngon từ những gì bạn có — đồ án tốt nghiệp về gợi ý công thức theo nguyên liệu còn
            trong tủ lạnh.
          </p>
        </div>
        <nav className="landing-footer-col" aria-label="Khám phá">
          <h2>Khám phá</h2>
          {navLinks.map(({ href, label }) => (
            <a key={href} href={href}>
              {label}
            </a>
          ))}
        </nav>
        <nav className="landing-footer-col" aria-label="Dự án">
          <h2>Dự án</h2>
          <Link to="/ve-chung-toi">Về chúng tôi</Link>
          <Link to="/ve-chung-toi#cong-nghe">Tech stack</Link>
          <Link to="/ve-chung-toi#kien-truc">Kiến trúc hệ thống</Link>
        </nav>
        <nav className="landing-footer-col" aria-label="Liên hệ">
          <h2>Liên hệ</h2>
          <a href={`mailto:${CONTACT_EMAIL}`}>
            <PaperPlaneTilt size={15} /> {CONTACT_EMAIL}
          </a>
          <a href={REPO_URL} target="_blank" rel="noreferrer">
            <GithubLogo size={15} /> ziu222/Dishcover
          </a>
          <a className="landing-footer-top" href="#noi-dung">
            Lên đầu trang <ArrowUpRight size={15} />
          </a>
        </nav>
      </div>
      <div className="landing-container landing-footer-base">
        <small>© {new Date().getFullYear()} Larder · Bùi Trọng Nghĩa</small>
        <small>Khoa học máy tính · 2351010136</small>
      </div>
    </footer>
  )
}
