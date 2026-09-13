import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { MotionConfig, motion } from 'framer-motion'
import { ArrowLeft, ArrowUpRight, GithubLogo, PaperPlaneTilt } from '@phosphor-icons/react'
import { CONTACT_EMAIL, LandingFooter, LandingHeader, REPO_URL } from '../components/LandingChrome'
import { fadeUp, group, inView, maskLine, popIn, revealBlock } from './landingMotion'
import { pillars, services, stack, stats } from './aboutData'
import './landing.css'
import './about.css'

/** Mỗi hàng tự hiện khi cuộn tới nó, không đợi cả khối vào viewport. */
const rowInView = {
  variants: fadeUp,
  initial: 'hidden',
  whileInView: 'show',
  viewport: { once: true, amount: 0.6 },
} as const

export function About() {
  // React Router không tự cuộn tới #hash khi điều hướng từ trang khác (link ở chân trang).
  const { hash } = useLocation()
  useEffect(() => {
    if (!hash) return
    document.querySelector(hash)?.scrollIntoView()
  }, [hash])

  const reveal = { ...inView, variants: revealBlock() }

  return (
    <MotionConfig reducedMotion="user">
      <div className="landing about">
        <div className="landing-grain" aria-hidden="true" />
        <a className="landing-skip" href="#noi-dung">
          Đến nội dung chính
        </a>
        <LandingHeader />

        <main id="noi-dung">
          <section className="landing-container about-hero">
            <motion.div
              className="about-hero-copy"
              variants={revealBlock(0.09, 0.05)}
              initial="hidden"
              animate="show"
            >
              <motion.div className="landing-section-label" variants={fadeUp}>
                ĐỒ ÁN TỐT NGHIỆP · 2026
              </motion.div>
              <h1>
                <span className="landing-mask">
                  <motion.span variants={maskLine}>Một quả cà chua còn lại</motion.span>
                </span>
                <span className="landing-mask">
                  <motion.span variants={maskLine}>
                    không đáng bị bỏ<span className="landing-dot">.</span>
                  </motion.span>
                </span>
              </h1>
              <motion.p className="about-lead" variants={fadeUp}>
                Larder đi ngược với cách dùng công thức thông thường: thay vì chọn món rồi đi mua
                nguyên liệu, hệ thống nhìn vào những gì còn trong tủ lạnh rồi mới nói hôm nay nấu
                được gì — ưu tiên thứ sắp hết hạn trước.
              </motion.p>
              <motion.dl className="about-meta" variants={fadeUp}>
                <div>
                  <dt>Thực hiện</dt>
                  <dd>Bùi Trọng Nghĩa</dd>
                </div>
                <div>
                  <dt>Mã sinh viên</dt>
                  <dd>2351010136</dd>
                </div>
                <div>
                  <dt>Ngành</dt>
                  <dd>Khoa học máy tính</dd>
                </div>
              </motion.dl>
            </motion.div>
          </section>

          <section className="about-stats-section" aria-label="Vài con số của hệ thống">
            <motion.div className="landing-container about-stats" {...inView} variants={group(0.08)}>
              {stats.map(({ value, label }) => (
                <motion.div className="about-stat" key={label} variants={popIn}>
                  <strong>{value}</strong>
                  <span>{label}</span>
                </motion.div>
              ))}
            </motion.div>
          </section>

          <section className="landing-container about-split">
            <motion.div className="about-split-copy" {...reveal}>
              <h2>
                <span className="landing-mask">
                  <motion.span variants={maskLine}>Bài toán ngược</motion.span>
                </span>
              </h2>
              <p className="landing-body-copy">
                Phần lớn thức ăn bỏ đi không phải vì hỏng bất ngờ, mà vì không ai nhớ nó còn nằm
                trong tủ. Larder giữ giúp bạn phần ghi nhớ đó, rồi biến nó thành gợi ý nấu ăn.
              </p>
            </motion.div>
            <motion.div className="about-compare" {...inView} variants={group(0.1)}>
              <motion.div className="about-compare-row" variants={fadeUp}>
                <span>Cách thường</span>
                <p>Chọn món, tra công thức, đi chợ mua đủ nguyên liệu — phần thừa nằm lại tủ.</p>
              </motion.div>
              <motion.div className="about-compare-row about-compare-ours" variants={fadeUp}>
                <span>Larder</span>
                <p>
                  Mở tủ lạnh, hệ thống chấm điểm từng công thức theo nguyên liệu đang có, nấu
                  trước thứ sắp hết hạn.
                </p>
              </motion.div>
            </motion.div>
          </section>

          <section className="landing-container about-pillars">
            <motion.div {...reveal}>
              <div className="landing-section-label">BA TRỤ CỘT</div>
              <h2>
                <span className="landing-mask">
                  <motion.span variants={maskLine}>Hệ thống làm ba việc.</motion.span>
                </span>
              </h2>
            </motion.div>
            <div className="about-pillar-list">
              {pillars.map(({ index, title, text }) => (
                <motion.article className="about-pillar" key={index} {...rowInView}>
                  <span className="about-index">{index}</span>
                  <div>
                    <h3>{title}</h3>
                    <p>{text}</p>
                  </div>
                </motion.article>
              ))}
            </div>
          </section>

          <section id="kien-truc" className="about-arch-section">
            <div className="landing-container">
              <motion.div className="about-section-head" {...reveal}>
                <div>
                  <div className="landing-section-label">KIẾN TRÚC</div>
                  <h2>
                    <span className="landing-mask">
                      <motion.span variants={maskLine}>Tám dịch vụ, hai loại dữ liệu.</motion.span>
                    </span>
                  </h2>
                </div>
                <p className="landing-body-copy">
                  Mỗi dịch vụ giữ dữ liệu của riêng mình và chỉ nói chuyện với nhau qua REST; chỉ
                  Gateway lộ cổng ra ngoài. Riêng việc nhắc hạn dùng đi đường Kafka bất đồng bộ.
                </p>
              </motion.div>
              <div className="about-table">
                <div className="about-table-head">
                  <span>Dịch vụ</span>
                  <span>Vai trò</span>
                  <span>Kho dữ liệu</span>
                </div>
                {services.map(({ name, role, store }) => (
                  <motion.div className="about-table-row" key={name} {...rowInView}>
                    <span>{name}</span>
                    <span>{role}</span>
                    <span className="about-store">{store}</span>
                  </motion.div>
                ))}
              </div>
            </div>
          </section>

          <section id="cong-nghe" className="landing-container about-stack">
            <motion.div className="about-section-head" {...reveal}>
              <div>
                <div className="landing-section-label">CÔNG NGHỆ</div>
                <h2>
                  <span className="landing-mask">
                    <motion.span variants={maskLine}>Những thứ dựng nên Larder.</motion.span>
                  </span>
                </h2>
              </div>
              <p className="landing-body-copy">
                Không thứ nào ở đây có mặt chỉ để cho đẹp hồ sơ: mỗi cái đứng đúng chỗ nó giải
                quyết một vấn đề cụ thể của hệ thống.
              </p>
            </motion.div>
            <div className="about-stack-list">
              {stack.map(({ layer, items }) => (
                <motion.div className="about-stack-row" key={layer} {...rowInView}>
                  <h3>{layer}</h3>
                  <ul>
                    {items.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                </motion.div>
              ))}
            </div>
          </section>

          <section className="about-contact-section">
            <motion.div className="landing-container about-contact" {...reveal}>
              <h2>
                <span className="landing-mask">
                  <motion.span variants={maskLine}>Muốn hỏi thêm về dự án?</motion.span>
                </span>
              </h2>
              <p>
                Mã nguồn, tài liệu kiến trúc và toàn bộ lịch sử quyết định đều nằm trong kho Git
                của dự án.
              </p>
              <div className="about-contact-actions">
                <a
                  className="landing-button landing-button-primary"
                  href={REPO_URL}
                  target="_blank"
                  rel="noreferrer"
                >
                  Xem mã nguồn
                  <span className="landing-button-icon">
                    <GithubLogo size={17} />
                  </span>
                </a>
                <a className="landing-text-link" href={`mailto:${CONTACT_EMAIL}`}>
                  <PaperPlaneTilt size={18} /> {CONTACT_EMAIL}
                </a>
                <Link className="landing-text-link" to="/register">
                  Tạo tài khoản và thử ngay <ArrowUpRight size={18} />
                </Link>
              </div>
              <Link className="about-back landing-text-link" to="/">
                <ArrowLeft size={17} /> Về trang chủ
              </Link>
            </motion.div>
          </section>
        </main>

        <LandingFooter />
      </div>
    </MotionConfig>
  )
}
