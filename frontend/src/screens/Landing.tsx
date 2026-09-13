import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  Basket,
  Check,
  ChefHat,
  Clock,
  CookingPot,
  Egg,
  ForkKnife,
  Leaf,
  List,
  Plus,
  Sparkle,
  X,
} from '@phosphor-icons/react'
import { useAuth } from '../auth/AuthContext'
import { ingredients, sampleRecipes, type Ingredient, type SampleRecipe } from './landingData'
import './landing.css'

const filters = ['Tất cả', 'Dưới 20 phút', 'Món chay', 'Giàu đạm'] as const
type Filter = (typeof filters)[number]

export function Landing() {
  const { isAuthenticated, checking } = useAuth()
  const signedIn = isAuthenticated && !checking
  const reduceMotion = useReducedMotion()
  const [menuOpen, setMenuOpen] = useState(false)
  const [selected, setSelected] = useState<Ingredient[]>(['Cà chua', 'Rau xanh'])
  const [filter, setFilter] = useState<Filter>('Tất cả')
  const [matchIngredients, setMatchIngredients] = useState<Ingredient[] | null>(null)
  const [activeRecipe, setActiveRecipe] = useState<SampleRecipe | null>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const recipeHeadingRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (!activeRecipe) return
    const dialog = dialogRef.current
    dialog?.showModal()
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      dialog?.close()
      document.body.style.overflow = overflow
    }
  }, [activeRecipe])

  const visibleRecipes = useMemo(() => {
    const matching = sampleRecipes.filter((recipe) => {
      const categoryMatch =
        filter === 'Tất cả' ||
        (filter === 'Dưới 20 phút' ? recipe.minutes < 20 : recipe.category === filter)
      return (
        categoryMatch &&
        (!matchIngredients || recipe.ingredients.some((item) => matchIngredients.includes(item)))
      )
    })
    if (!matchIngredients) return matching
    const score = (recipe: SampleRecipe) =>
      recipe.ingredients.filter((item) => matchIngredients.includes(item)).length /
      recipe.ingredients.length
    return matching.sort((a, b) => score(b) - score(a))
  }, [filter, matchIngredients])

  function toggleIngredient(ingredient: Ingredient) {
    setSelected((current) =>
      current.includes(ingredient)
        ? current.filter((item) => item !== ingredient)
        : [...current, ingredient],
    )
  }

  function findRecipes() {
    setMatchIngredients([...selected])
    setFilter('Tất cả')
    recipeHeadingRef.current?.focus({ preventScroll: true })
    document
      .getElementById('cong-thuc')
      ?.scrollIntoView({ behavior: reduceMotion ? 'instant' : 'smooth' })
  }

  const reveal = {
    initial: reduceMotion ? (false as const) : { opacity: 0, y: 20 },
    whileInView: { opacity: 1, y: 0 },
    viewport: { once: true, amount: 0.12 },
    transition: { duration: 0.5 },
  }

  return (
    <div className="landing">
      <a className="landing-skip" href="#noi-dung">
        Đến nội dung chính
      </a>
      <header className="landing-header">
        <div className="landing-container landing-nav">
          <Link className="landing-logo" to="/" aria-label="Larder, trang chủ">
            Larder<span>.</span>
          </Link>
          <nav className="landing-desktop-nav" aria-label="Điều hướng trang giới thiệu">
            <a href="#cach-hoat-dong">Cách hoạt động</a>
            <a href="#cong-thuc">Cảm hứng vào bếp</a>
            <a href="#ve-larder">Về Larder</a>
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
              {menuOpen ? <X size={23} /> : <List size={23} />}
            </button>
          </div>
        </div>
        {menuOpen && (
          <nav
            id="landing-mobile-nav"
            className="landing-mobile-nav"
            aria-label="Điều hướng trên điện thoại"
            onKeyDown={(event) => {
              if (event.key === 'Escape') setMenuOpen(false)
            }}
          >
            <a href="#cach-hoat-dong" onClick={() => setMenuOpen(false)}>
              Cách hoạt động
            </a>
            <a href="#cong-thuc" onClick={() => setMenuOpen(false)}>
              Cảm hứng vào bếp
            </a>
            <a href="#ve-larder" onClick={() => setMenuOpen(false)}>
              Về Larder
            </a>
          </nav>
        )}
      </header>

      <main id="noi-dung">
        <section className="landing-hero">
          <img
            className="landing-hero-image"
            src="/assets/landing/hero-warm.webp"
            alt="Bát cơm gà áp chảo với rau xanh, cà chua và dưa leo tươi"
            width="1672"
            height="941"
            fetchPriority="high"
          />
          <div className="landing-container landing-hero-inner">
            <motion.div className="landing-hero-copy" {...reveal}>
              <div className="landing-eyebrow">
                <Leaf size={18} weight="fill" /> Ít lãng phí. Nhiều món ngon.
              </div>
              <h1>
                Larder<span>.</span>
              </h1>
              <p className="landing-hero-headline">
                Bếp nhỏ của bạn.
                <br />
                Cảm hứng mỗi ngày.
              </p>
              <p className="landing-hero-description">
                Biến nguyên liệu sẵn có thành bữa ngon.
                <br className="landing-desktop-break" /> Để câu hỏi “hôm nay ăn gì?” trở nên dễ
                dàng.
              </p>
              <div className="landing-hero-actions">
                <Link
                  className="landing-button landing-button-primary"
                  to={signedIn ? '/' : '/register'}
                >
                  Bắt đầu vào bếp <ArrowUpRight size={20} />
                </Link>
                <a className="landing-text-link" href="#thu-ngay">
                  Khám phá thử <ArrowDown size={18} />
                </a>
              </div>
            </motion.div>
          </div>
        </section>

        <section className="landing-values" aria-label="Lợi ích của Larder">
          <div className="landing-container landing-values-inner">
            <span>
              <Basket /> Tận dụng đồ sẵn có
            </span>
            <span>
              <CookingPot /> Gợi ý hợp khẩu vị
            </span>
            <span>
              <Leaf /> Bớt lãng phí mỗi ngày
            </span>
          </div>
        </section>

        <section id="thu-ngay" className="landing-container landing-try-section">
          <motion.div {...reveal}>
            <div className="landing-section-label">
              <Sparkle size={18} /> MỘT CHÚT CẢM HỨNG
            </div>
            <h2>
              Tủ lạnh có gì,
              <br />
              bữa ngon có đó.
            </h2>
            <p className="landing-body-copy">Một vài nguyên liệu quen thuộc cũng đủ để bắt đầu.</p>
          </motion.div>
          <motion.div className="landing-ingredient-tool" {...reveal}>
            <div className="landing-tool-heading">
              <h3>Nguyên liệu của bạn</h3>
              <Basket size={23} />
            </div>
            <div className="landing-ingredients" role="group" aria-label="Chọn nguyên liệu có sẵn">
              {ingredients.map((ingredient) => {
                const checked = selected.includes(ingredient)
                return (
                  <button
                    key={ingredient}
                    className="landing-ingredient"
                    aria-pressed={checked}
                    onClick={() => toggleIngredient(ingredient)}
                  >
                    {ingredient === 'Trứng' ? (
                      <Egg size={18} />
                    ) : ingredient === 'Rau xanh' || ingredient === 'Cà chua' ? (
                      <Leaf size={18} />
                    ) : (
                      <ForkKnife size={18} />
                    )}
                    <span>{ingredient}</span>
                    {checked ? <Check size={16} weight="bold" /> : <Plus size={16} />}
                  </button>
                )
              })}
            </div>
            <div className="landing-tool-footer">
              <span aria-live="polite">
                {selected.length
                  ? `${selected.length} nguyên liệu đã chọn`
                  : 'Chưa chọn nguyên liệu'}
              </span>
              <button
                className="landing-button landing-button-primary"
                disabled={!selected.length}
                onClick={findRecipes}
              >
                Tìm món ngon <ArrowRight size={18} />
              </button>
            </div>
          </motion.div>
        </section>

        <section id="cong-thuc" className="landing-recipes-section">
          <div className="landing-container">
            <motion.div className="landing-section-heading" {...reveal}>
              <div>
                <h2 ref={recipeHeadingRef} tabIndex={-1}>
                  Hôm nay, thử món này nhé.
                </h2>
                <p className="landing-body-copy">
                  Những công thức mẫu giản đơn cho một bữa ăn đáng mong chờ.
                </p>
              </div>
              <a className="landing-text-link" href="#thu-ngay">
                Chọn nguyên liệu <ArrowUpRight size={18} />
              </a>
            </motion.div>
            <div className="landing-recipe-controls">
              <div className="landing-filters" role="group" aria-label="Lọc công thức">
                {filters.map((item) => (
                  <button key={item} aria-pressed={filter === item} onClick={() => setFilter(item)}>
                    {item === 'Tất cả' && <ForkKnife size={16} />}
                    {item}
                  </button>
                ))}
              </div>
              {matchIngredients && (
                <button className="landing-clear" onClick={() => setMatchIngredients(null)}>
                  Bỏ lọc nguyên liệu <X size={16} />
                </button>
              )}
            </div>
            <p className="landing-result-count" role="status">
              {matchIngredients
                ? `${visibleRecipes.length} món có nguyên liệu bạn chọn: ${matchIngredients.join(', ')}`
                : `${visibleRecipes.length} công thức cho bạn khám phá`}
            </p>
            <div className="landing-recipe-grid">
              {visibleRecipes.map((recipe) => (
                <motion.article className="landing-recipe" key={recipe.id} {...reveal}>
                  <button
                    className="landing-recipe-image-button"
                    onClick={() => setActiveRecipe(recipe)}
                    aria-label={`Xem công thức ${recipe.title}`}
                  >
                    <img
                      src={recipe.image}
                      alt={recipe.title}
                      className={recipe.id === 'chicken' ? 'landing-chicken-image' : ''}
                      loading="lazy"
                      width="600"
                      height="450"
                    />
                  </button>
                  <div className="landing-recipe-meta">
                    <span>
                      <Clock size={15} /> {recipe.minutes} phút
                    </span>
                    <span>{recipe.category}</span>
                  </div>
                  <h3>
                    <button onClick={() => setActiveRecipe(recipe)}>
                      {recipe.title}
                      <ArrowUpRight size={20} />
                    </button>
                  </h3>
                  <p>{recipe.description}</p>
                  {matchIngredients && (
                    <span className="landing-match-label">
                      <Check size={15} /> Có{' '}
                      {recipe.ingredients.filter((item) => matchIngredients.includes(item)).length}/
                      {recipe.ingredients.length} nguyên liệu chính
                    </span>
                  )}
                </motion.article>
              ))}
            </div>
            {!visibleRecipes.length && (
              <div className="landing-empty">
                <CookingPot size={36} />
                <h3>Chưa có món phù hợp trong bộ mẫu.</h3>
                <p>Thử nhóm món khác hoặc thêm nguyên liệu vào tủ lạnh nhé.</p>
                <button
                  className="landing-text-link"
                  onClick={() => {
                    setFilter('Tất cả')
                    setMatchIngredients(null)
                  }}
                >
                  Xem tất cả công thức <ArrowRight size={18} />
                </button>
              </div>
            )}
          </div>
        </section>

        <section id="cach-hoat-dong" className="landing-container landing-how-section">
          <motion.div className="landing-how-intro" {...reveal}>
            <ChefHat size={34} weight="duotone" />
            <h2>
              Từ mở tủ lạnh
              <br />
              đến dọn bữa ngon.
            </h2>
            <p className="landing-body-copy">
              Larder đồng hành cùng những bữa cơm thường ngày của bạn.
            </p>
            <Link className="landing-text-link" to={signedIn ? '/tu-lanh' : '/register'}>
              Tạo tủ lạnh của bạn <ArrowUpRight size={19} />
            </Link>
          </motion.div>
          <div className="landing-steps">
            {[
              {
                icon: Basket,
                title: 'Bắt đầu từ những gì đang có',
                text: 'Thêm nguyên liệu vào tủ lạnh ảo và theo dõi hạn dùng ở cùng một nơi.',
              },
              {
                icon: Sparkle,
                title: 'Tìm một món thật hợp ý',
                text: 'Khám phá công thức theo nguyên liệu, ưu tiên những thực phẩm cần dùng sớm.',
              },
              {
                icon: CookingPot,
                title: 'Vào bếp theo cách của bạn',
                text: 'Lưu món yêu thích và hỏi trợ lý nấu ăn khi cần thêm một chút cảm hứng.',
              },
            ].map(({ icon: Icon, title, text }) => (
              <motion.div className="landing-step" key={title} {...reveal}>
                <div className="landing-step-icon">
                  <Icon size={25} />
                </div>
                <div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </section>

        <section id="ve-larder" className="landing-about-section">
          <div className="landing-container landing-about-inner">
            <motion.div {...reveal}>
              <Leaf size={30} weight="duotone" />
              <h2>
                Mỗi nguyên liệu đều xứng đáng
                <br />
                trở thành một món ngon.
              </h2>
              <p>
                Một quả cà chua còn lại. Vài cọng rau trong tủ.
                <br />
                Một thay đổi nhỏ hôm nay, một căn bếp ít lãng phí hơn ngày mai.
              </p>
              <Link
                className="landing-button landing-button-primary"
                to={signedIn ? '/' : '/register'}
              >
                Cùng Larder bắt đầu <ArrowUpRight size={20} />
              </Link>
            </motion.div>
          </div>
        </section>
      </main>

      <footer className="landing-footer landing-container">
        <Link className="landing-logo" to="/">
          Larder<span>.</span>
        </Link>
        <p>Nấu ngon từ những gì bạn có.</p>
        <a href="#noi-dung" className="landing-text-link">
          Lên đầu trang <ArrowUpRight size={17} />
        </a>
        <small>© {new Date().getFullYear()} Larder</small>
      </footer>

      <dialog
        ref={dialogRef}
        className="landing-dialog"
        aria-labelledby="landing-recipe-title"
        onCancel={() => setActiveRecipe(null)}
        onClick={(event) => {
          if (event.target === event.currentTarget) setActiveRecipe(null)
        }}
      >
        {activeRecipe && (
          <div className="landing-dialog-content">
            <div className="landing-dialog-header">
              <span>Công thức mẫu · {activeRecipe.minutes} phút · 2 phần ăn</span>
              <button
                className="landing-icon-button"
                onClick={() => setActiveRecipe(null)}
                aria-label="Đóng công thức"
                autoFocus
              >
                <X size={23} />
              </button>
            </div>
            <h2 id="landing-recipe-title">{activeRecipe.title}</h2>
            <p>{activeRecipe.description}</p>
            <h3>Nguyên liệu</h3>
            <ul>
              {activeRecipe.amounts.map((amount) => (
                <li key={amount}>{amount}</li>
              ))}
            </ul>
            <h3>Cùng vào bếp</h3>
            <ol>
              {activeRecipe.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
            <Link
              className="landing-button landing-button-primary"
              to={signedIn ? '/tim-kiem' : '/register'}
              onClick={() => setActiveRecipe(null)}
            >
              {signedIn ? 'Khám phá thêm công thức' : 'Tạo tài khoản để khám phá thêm'}
              <ArrowUpRight size={18} />
            </Link>
          </div>
        )}
      </dialog>
    </div>
  )
}
