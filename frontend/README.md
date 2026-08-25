# Larder — Frontend

Giao diện web cho **Leftover Recipe Matcher** (Larder): gợi ý công thức theo nguyên liệu còn
trong tủ lạnh. SPA gọi backend microservices qua API Gateway.

## Tech stack

- **Vite 7** + **React 19** + **TypeScript** (strict)
- **Tailwind CSS v4** (plugin Vite, tokens qua `@theme` trong `src/index.css`)
- **Framer Motion** (chuyển động), **@phosphor-icons/react** (icon)
- **React Router v7** (điều hướng, route bảo vệ theo JWT)

Font: **Newsreader** (serif, tiêu đề editorial) + **Be Vietnam Pro** (sans, giao diện — native tiếng Việt).
Bảng màu: nền kem, accent đất nung `#A85D42` — trích từ mockup "Larder Foundation" (Claude Design).

## Chạy dev

```bash
npm install
npm run dev        # http://localhost:5173
```

Frontend gọi API qua path tương đối `/api/...`; Vite dev proxy (`vite.config.ts`) chuyển sang
**API Gateway `http://localhost:8080`** và cắt tiền tố `/api`. Vì vậy cần backend chạy để dùng thật:

- `docker-setup/` (Postgres + Mongo)
- User Service (8081), Recipe Service (8083), Gateway (8080)

Màn đăng nhập/đăng ký gọi `/user-service/auth/*`; màn Home gọi `/recipe-service/recipes` (public).

> Dev proxy chỉ dành cho môi trường dev. Production cần cấu hình CORS ở Gateway hoặc phục vụ
> frontend chung origin.

## Build

```bash
npm run build      # tsc -b && vite build -> dist/
npm run preview
```

## Cấu trúc

```
src/
  main.tsx            # entry: Router + AuthProvider
  App.tsx             # khai báo route + RequireAuth
  index.css           # Tailwind + design tokens (@theme)
  types.ts            # type khớp DTO backend
  lib/
    api.ts            # fetch wrapper (/api, Bearer token, ApiError)
    cn.ts             # nối className
  auth/AuthContext.tsx# login/register/logout, token+user ở localStorage
  hooks/useRecipes.ts # nạp công thức từ Recipe Service
  components/         # tái sử dụng: Button, Field, Chip, RecipeCard,
                      #   AuthLayout, AppShell
  screens/            # Login, Register, Home
```

## Tiến độ (lượt 1)

Đã xong: hệ thống thiết kế (tokens) + 6 component tái sử dụng + 3 màn
**Đăng nhập / Đăng ký / Home (desktop)**, wired API thật qua Gateway.

Chưa làm (lượt sau): Recipe Detail, Cooking Mode, Search, Tủ lạnh ảo, Thêm nguyên liệu,
Gợi ý theo nguyên liệu, Chatbot AI, Notifications, Tài khoản; bản mobile của Home;
favorites đồng bộ server (hiện chỉ localStorage); CORS Gateway cho production.

Không có PRO/paywall — mọi tính năng chỉ cần đăng nhập (Freemium đã gỡ khỏi phạm vi đề tài).
