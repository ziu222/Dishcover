// Tra emoji cho nguyên liệu theo `normalizedName` (chuỗi ASCII đã bỏ dấu, viết thường,
// VD "ca chua", "thit bo", "gia do"). Backend KHÔNG trả emoji/category nên map ở frontend.
//
// ponytail: emoji Unicode thay vì thư viện icon — 0 dependency, offline tuyệt đối (buổi bảo vệ
// có thể không có mạng), Windows/macOS render màu sẵn. Muốn nâng lên Iconify/OpenMoji sau này
// chỉ đổi tầng render tile, map này giữ nguyên khoá.
//
// Khoá là keyword khớp bằng includes(). THỨ TỰ QUAN TRỌNG: cụ thể đứng trước tổng quát để tránh
// xung đột — "thit bo" (🥩) phải chặn trước "bo" (🥑/bơ), "ca chua"/"ca rot" trước "ca" (🐟),
// "gia do" (🌱) trước "ga" (🍗), "khoai lang" trước "khoai".

type Entry = readonly [keywords: readonly string[], emoji: string]

const TABLE: readonly Entry[] = [
  // Thịt & hải sản (đặt "thit bo" trước mọi "bo")
  [['thit bo', 'bo bit tet', 'bo luc lac'], '🥩'],
  [['thit heo', 'thit lon', 'ba roi', 'suon'], '🥓'],
  [['thit ga', 'uc ga', 'dui ga', 'canh ga'], '🍗'],
  [['gia cam', 'vit', 'ngan'], '🦆'],
  [['giam bong', 'xuc xich', 'lap xuong', 'thit nguoi'], '🌭'],
  [['tom'], '🦐'],
  [['muc'], '🦑'],
  [['cua', 'ghe'], '🦀'],
  [['nghieu', 'so huyet', 'oc', 'hen'], '🐚'],
  [['ca hoi'], '🍣'],
  [['ca chua'], '🍅'], // chặn "ca" bắt nhầm cà chua thành cá
  [['ca rot'], '🥕'],
  [['ca tim'], '🍆'],
  [['ca phao', 'ca muoi'], '🫙'],
  [['ca'], '🐟'],

  // Trứng & sữa
  [['trung'], '🥚'],
  [['pho mai', 'pho mát', 'phô mai'], '🧀'],
  [['sua chua', 'yaourt'], '🥛'],
  [['bo', 'butter'], '🧈'], // "bo" tổng quát = bơ (đã chặn thịt bò ở trên)
  [['sua'], '🥛'],

  // Rau lá & rau củ
  [['gia do', 'gia'], '🌱'], // chặn trước "ga"
  [['hanh la', 'hanh tay', 'hanh'], '🧅'],
  [['toi'], '🧄'],
  [['gung'], '🫚'],
  [['ot chuong', 'ot'], '🌶️'],
  [['nam'], '🍄'],
  [['khoai lang'], '🍠'],
  [['khoai tay', 'khoai'], '🥔'],
  [['bap cai', 'cai thao', 'cai ngot', 'cai xanh', 'xa lach', 'rau muong', 'rau'], '🥬'],
  [['bong cai', 'sup lo', 'broccoli'], '🥦'],
  [['dua leo', 'dua chuot'], '🥒'],
  [['bi do', 'bi ngo', 'bi'], '🎃'],
  [['ngo', 'bap'], '🌽'],
  [['dau hu', 'dau phu', 'tofu'], '⬜'],
  [['dau que', 'dau cove', 'dau ha lan', 'dau'], '🫛'],
  [['ca chua bi'], '🍅'],

  // Trái cây
  [['chanh'], '🍋'],
  [['cam', 'quyt'], '🍊'],
  [['chuoi'], '🍌'],
  [['tao'], '🍎'],
  [['nho'], '🍇'],
  [['dua hau'], '🍉'],
  [['dua'], '🍍'],
  [['xoai'], '🥭'],
  [['bo trai', 'trai bo', 'qua bo', 'avocado'], '🥑'],
  [['dau tay', 'strawberry'], '🍓'],

  // Tinh bột & khô
  [['bot mi', 'bot'], '🌾'],
  [['banh mi'], '🥖'],
  [['com', 'gao', 'rice'], '🍚'],
  [['mi', 'bun', 'pho', 'noodle', 'nui'], '🍜'],

  // Gia vị & nước
  [['mu tat', 'mustard'], '🟡'],
  [['muoi'], '🧂'],
  [['duong'], '🍬'],
  [['mat ong', 'honey'], '🍯'],
  [['dau an', 'dau oliu', 'dau me'], '🫒'],
  [['nuoc mam', 'nuoc tuong', 'xi dau', 'sot'], '🫗'],
  [['tuong ot', 'tuong'], '🥫'],
]

const FALLBACK = '🥘'

/** Emoji đại diện cho một nguyên liệu. Không khớp keyword nào → 🥘. */
export function ingredientEmoji(normalizedName: string | null | undefined): string {
  const n = (normalizedName ?? '').toLowerCase().trim()
  if (!n) return FALLBACK
  for (const [keywords, emoji] of TABLE) {
    if (keywords.some((k) => n.includes(k))) return emoji
  }
  return FALLBACK
}

// ponytail: self-check cho các xung đột dễ sai (bo/ca/ga). Chạy trong dev, cảnh báo console nếu vỡ.
if (import.meta.env.DEV) {
  const cases: Array<[string, string]> = [
    ['thit bo', '🥩'],
    ['bo', '🧈'],
    ['ca chua', '🍅'],
    ['ca rot', '🥕'],
    ['ca', '🐟'],
    ['gia do', '🌱'],
    ['thit ga', '🍗'],
    ['trung ga', '🥚'],
    ['khoai lang', '🍠'],
    ['khoai tay', '🥔'],
    ['dau an', '🫒'],
    ['trai bo', '🥑'],
    ['xyz khong co', '🥘'],
  ]
  for (const [input, want] of cases) {
    console.assert(
      ingredientEmoji(input) === want,
      `ingredientEmoji("${input}") = ${ingredientEmoji(input)}, muốn ${want}`,
    )
  }
}
