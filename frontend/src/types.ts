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

/** NutritionResponse — calo/macro mỗi khẩu phần, null nếu Recipe Service cũ chưa có field này. */
export interface Nutrition {
  caloriesPerServing: number
  proteinPerServing: number
  carbPerServing: number
  fatPerServing: number
  /** true = có nguyên liệu/đơn vị không quy đổi được sang gram — số liệu chỉ là ước tính một phần. */
  incomplete: boolean
}

/** RecipeDetailResponse — GET /recipes/{id}, kèm ingredients/steps. */
export interface RecipeDetail extends RecipeSummary {
  dietaryFlags: string[]
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
  servings: number | null
  nutrition: Nutrition | null
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

export type InventoryStatus = 'FRESH' | 'EXPIRING_SOON' | 'EXPIRED' | 'USED'

/** InventoryItemResponse — 1 dòng nguyên liệu trong tủ lạnh ảo. status do server derive theo hạn dùng. */
export interface InventoryItem {
  id: number
  ingredientName: string
  normalizedName: string
  quantity: number | null
  unit: string | null
  expiryDate: string | null // 'YYYY-MM-DD'
  source: string
  status: InventoryStatus
}

/** RecognizedIngredientDto — GET /image-service/recognize, 1 nguyên liệu Vision API nhận diện được. */
export interface RecognizedIngredient {
  name: string
  normalizedName: string
  confidence: number
  quantityGuess: string | null
  suggestedExpiryDate: string | null // 'YYYY-MM-DD'
}

export type DietaryType = 'ALLERGY' | 'DIET'

/** DietaryPreferenceResponse — 1 mục hồ sơ ăn uống (dị ứng/chế độ ăn) của user. */
export interface DietaryPreference {
  id: number
  type: DietaryType
  value: string
}

/** RecipeMatchResponse — GET /matching/suggestions, 1 công thức gợi ý theo tủ lạnh hiện tại. */
export interface RecipeMatch {
  recipeId: string
  name: string
  slug: string
  score: number
  matchedIngredients: string[]
  missingIngredients: string[]
  imageUrl: string | null
}

/** CalorieGoalResponse — mục tiêu calo/macro/ngày. null nếu user chưa đặt (GET trả body rỗng). */
export interface CalorieGoal {
  calorieTarget: number
  proteinTarget: number
  carbTarget: number
  fatTarget: number
}

export type AvailabilityStatus = 'SUFFICIENT' | 'PARTIAL' | 'MISSING' | 'UNKNOWN'

/** IngredientAvailabilityResponse — so 1 nguyên liệu công thức với tủ lạnh người dùng. */
export interface IngredientAvailability {
  name: string
  normalizedName: string
  neededAmount: number | null
  neededUnit: string | null
  availableGrams: number
  status: AvailabilityStatus
  /** Số lượng còn thiếu, cùng đơn vị với neededUnit — null nếu SUFFICIENT/UNKNOWN. */
  shortfallAmount: number | null
}

/** RecipeAvailabilityResponse — GET /matching/recipes/{id}/availability. */
export interface RecipeAvailability {
  recipeId: string
  name: string
  ingredients: IngredientAvailability[]
}

/** CookDeductResultLine — kết quả trừ kho cho 1 nguyên liệu sau POST cook-deduct. */
export interface CookDeductResult {
  normalizedName: string
  requestedGrams: number
  deductedGrams: number
}

export type NotificationType = 'INGREDIENT_EXPIRING_SOON' | 'INGREDIENT_EXPIRED'

/** NotificationResponse — 1 thông báo hết hạn nguyên liệu. */
export interface AppNotification {
  id: number
  type: NotificationType
  title: string
  message: string
  actionUrl: string | null
  isRead: boolean
  createdAt: string
}

/** NotificationListResponse — GET /notification-service/notifications. */
export interface NotificationListResponse {
  items: AppNotification[]
  unreadCount: number
}
