// Type khớp DTO backend. Nguồn: recipe/dto/RecipeDtos.java, user/dto/{AuthDtos,UserResponse}.java

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** RecipeSummaryResponse — dùng cho danh sách (không kèm ingredients/steps). */
export interface RecipeSummary {
  id: string
  name: string
  slug: string
  cookTimeMinutes: number
  difficulty: Difficulty
  tags: string[]
  imageUrl: string | null
}

/** RecipeIngredientResponse — một nguyên liệu trong chi tiết công thức. */
export interface RecipeIngredient {
  name: string
  normalizedName: string
  amount: number | null
  unit: string | null
  /** true = nguyên liệu chính (thiếu là không nấu được), false = phụ/gia vị. */
  essential: boolean
  weight: number
}

/** RecipeStepResponse — một bước nấu. */
export interface RecipeStep {
  order: number
  title: string
  content: string
  durationMinutes: number
}

/** RecipeDetailResponse — GET /recipes/{id}, kèm ingredients/steps. */
export interface RecipeDetail extends RecipeSummary {
  dietaryFlags: string[]
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
  videoUrl: string | null
}

/** Trang Spring Data (org.springframework.data.domain.Page) rút gọn phần frontend cần. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/** UserResponse — field `plan` giữ nhưng không dùng (Freemium đã gỡ). */
export interface User {
  id: number
  email: string
  fullName: string | null
  avatarUrl: string | null
  plan: string | null
}

/** AuthResponse — trả về từ /auth/login và /auth/register. */
export interface AuthResponse {
  token: string
  expiresInSeconds: number
  user: User
}
