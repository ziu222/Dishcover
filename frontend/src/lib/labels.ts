import type { Difficulty } from '../types'

export const DIFFICULTY_VI: Record<Difficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Vừa',
  HARD: 'Khó',
}

/** dietary_flags của backend → nhãn tiếng Việt (seed data dùng cả 2 dạng: vegan/vegetarian và contains_*). */
const DIETARY_VI: Record<string, string> = {
  vegetarian: 'Chay',
  vegan: 'Thuần chay',
  contains_egg: 'Có trứng',
  contains_dairy: 'Có sữa',
  contains_meat: 'Có thịt',
  contains_seafood: 'Có hải sản',
  contains_gluten: 'Có gluten',
  contains_nuts: 'Có hạt',
  contains_sesame: 'Có mè',
}

/** Cờ lạ vẫn hiển thị (dạng thô đã làm sạch) — thông tin dị ứng không được im lặng nuốt mất. */
export function dietaryLabel(flag: string): string {
  return DIETARY_VI[flag] ?? flag.replace(/^contains_/, 'Có ').replace(/_/g, ' ')
}
