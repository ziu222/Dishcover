// Tra icon Phosphor theo nhóm nguyên liệu, dựa trên `normalizedName` (chuỗi ASCII đã bỏ dấu,
// viết thường). Thay cho emoji cũ (ingredientEmoji.ts, đã gỡ) — bảng ledger cần tông trầm, đơn
// sắc hơn là icon vui mắt theo từng món; chỉ cần đúng NHÓM (thịt/hải sản/rau củ...), không cần
// đoán đúng từng nguyên liệu cụ thể.
//
// Khoá là keyword khớp bằng includes(). THỨ TỰ GIỮ NGUYÊN như bảng emoji gốc (đã kiểm chứng qua
// self-check) — KHÔNG nhóm lại theo icon vì sẽ phá thứ tự chặn xung đột: "ca chua" phải đứng
// trước "ca", "thit bo" trước "bo", "gia do" trước "ga", "chanh" trước "hanh", "dua leo" trước
// "dua" (dứa).
import { Avocado, BowlFood, Bread, Carrot, Cow, Drop, Egg, Fish, Orange, type Icon } from '@phosphor-icons/react'

type Entry = readonly [keywords: readonly string[], icon: Icon]

const TABLE: readonly Entry[] = [
  // Thịt & hải sản (đặt "thit bo" trước mọi "bo")
  [['thit bo', 'bo bit tet', 'bo luc lac'], Cow],
  [['thit heo', 'thit lon', 'ba roi', 'suon'], Cow],
  [['thit ga', 'uc ga', 'dui ga', 'canh ga'], Cow],
  [['gia cam', 'vit', 'ngan'], Cow],
  [['giam bong', 'xuc xich', 'lap xuong', 'thit nguoi'], Cow],
  [['tom'], Fish],
  [['muc'], Fish],
  [['cua', 'ghe'], Fish],
  [['nghieu', 'so huyet', 'oc', 'hen'], Fish],
  [['ca hoi'], Fish],
  [['ca chua'], Carrot], // chặn "ca" bắt nhầm cà chua thành hải sản
  [['ca rot'], Carrot],
  [['ca tim'], Carrot],
  [['ca phao', 'ca muoi'], Carrot],
  [['ca'], Fish],

  // Trứng & sữa
  [['trung'], Egg],
  [['pho mai', 'pho mát', 'phô mai'], Egg],
  [['sua chua', 'yaourt'], Egg],
  [['bo trai', 'trai bo', 'qua bo', 'avocado'], Avocado], // chặn trước "bo" trơ
  [['bo', 'butter'], Egg],
  [['sua'], Egg],

  // Rau lá & rau củ
  [['gia do', 'gia'], Carrot], // chặn trước "ga"
  [['chanh'], Orange], // "chanh" chứa "hanh" → phải chặn trước hành
  [['hanh la', 'hanh tay', 'hanh'], Carrot],
  [['toi'], Carrot],
  [['gung'], Carrot],
  [['ot chuong', 'ot'], Carrot],
  [['nam'], Carrot],
  [['khoai lang'], Carrot],
  [['khoai tay', 'khoai'], Carrot],
  [['bap cai', 'cai thao', 'cai ngot', 'cai xanh', 'xa lach', 'rau muong', 'rau'], Carrot],
  [['bong cai', 'sup lo', 'broccoli'], Carrot],
  [['dua leo', 'dua chuot'], Carrot], // chặn trước "dua" (dứa)
  [['bi do', 'bi ngo', 'bi'], Carrot],
  [['ngo', 'bap'], Carrot],
  [['dau hu', 'dau phu', 'tofu'], BowlFood],
  [['dau que', 'dau cove', 'dau ha lan', 'dau xanh', 'dau do'], Carrot],
  [['ca chua bi'], Carrot],

  // Trái cây
  [['cam', 'quyt'], Orange],
  [['chuoi'], Orange],
  [['tao'], Orange],
  [['nho'], Orange],
  [['dua hau'], Orange],
  [['dua'], Orange],
  [['xoai'], Orange],
  [['dau tay', 'strawberry'], Orange],

  // Tinh bột & khô
  [['bot mi', 'bot'], Bread],
  [['banh mi'], Bread],
  [['com', 'gao', 'rice'], Bread],
  [['mi', 'bun', 'pho', 'noodle', 'nui'], Bread],

  // Gia vị & nước
  [['mu tat', 'mustard'], Drop],
  [['muoi'], Drop],
  [['duong'], Drop],
  [['mat ong', 'honey'], Drop],
  [['dau an', 'dau oliu', 'dau me'], Drop],
  [['nuoc mam', 'nuoc tuong', 'xi dau', 'sot'], Drop],
  [['tuong ot', 'tuong'], Drop],
]

const FALLBACK: Icon = BowlFood

/** Icon Phosphor đại diện cho 1 nguyên liệu theo nhóm. Không khớp keyword nào → BowlFood. */
export function ingredientIcon(normalizedName: string | null | undefined): Icon {
  const n = (normalizedName ?? '').toLowerCase().trim()
  if (!n) return FALLBACK
  for (const [keywords, icon] of TABLE) {
    if (keywords.some((k) => n.includes(k))) return icon
  }
  return FALLBACK
}

// ponytail: self-check cho các xung đột dễ sai (bo/ca/ga/chanh/dua) — cùng bài học từ bảng emoji cũ.
if (import.meta.env.DEV) {
  const cases: Array<[string, Icon]> = [
    ['thit bo', Cow],
    ['bo', Egg],
    ['ca chua', Carrot],
    ['ca rot', Carrot],
    ['ca', Fish],
    ['gia do', Carrot],
    ['thit ga', Cow],
    ['trung ga', Egg],
    ['khoai lang', Carrot],
    ['dau an', Drop],
    ['trai bo', Avocado],
    ['chanh', Orange],
    ['hanh la', Carrot],
    ['dua leo', Carrot],
    ['dua hau', Orange],
    ['xyz khong co', BowlFood],
  ]
  for (const [input, want] of cases) {
    console.assert(
      ingredientIcon(input) === want,
      `ingredientIcon("${input}") không khớp icon mong đợi`,
    )
  }
}
