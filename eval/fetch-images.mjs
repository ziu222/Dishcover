// Tải 30 ảnh test cho Image Recognition Service (CLAUDE.md mục 11) từ Wikimedia Commons —
// ảnh thật, giấy phép mở (CC/PD), nguồn ổn định không bot-gate. Chạy: node eval/fetch-images.mjs
import { writeFile, mkdir, access } from 'node:fs/promises'

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function withRetry(fn, label, attempts = 5) {
  for (let i = 0; i < attempts; i++) {
    try {
      return await fn()
    } catch (err) {
      const is429 = String(err.message).includes('429')
      if (!is429 || i === attempts - 1) throw err
      const wait = 3000 * (i + 1)
      process.stdout.write(`(429, đợi ${wait}ms) `)
      await sleep(wait)
    }
  }
}

const PLAN = [
  // A. 10 ảnh rõ 1 nguyên liệu
  { id: 'a01', group: 'single', query: 'chicken egg white background', file: 'a01-trung-ga.jpg', expected: ['trứng gà'] },
  { id: 'a02', group: 'single', query: 'tomato fruit', file: 'a02-ca-chua.jpg', expected: ['cà chua'] },
  { id: 'a03', group: 'single', query: 'carrot vegetable', file: 'a03-ca-rot.jpg', expected: ['cà rốt'] },
  { id: 'a04', group: 'single', query: 'onion bulb vegetable', file: 'a04-hanh-tay.jpg', expected: ['hành tây'] },
  { id: 'a05', group: 'single', query: 'garlic bulb', file: 'a05-toi.jpg', expected: ['tỏi'] },
  { id: 'a06', group: 'single', query: 'potato vegetable', file: 'a06-khoai-tay.jpg', expected: ['khoai tây'] },
  { id: 'a07', group: 'single', query: 'banana fruit bunch', file: 'a07-chuoi.jpg', expected: ['chuối'] },
  { id: 'a08', group: 'single', query: 'orange citrus fruit', file: 'a08-cam.jpg', expected: ['cam'] },
  { id: 'a09', group: 'single', query: 'raw beef steak meat', file: 'a09-thit-bo.jpg', expected: ['thịt bò'] },
  { id: 'a10', group: 'single', query: 'raw shrimp prawn seafood', file: 'a10-tom.jpg', expected: ['tôm'] },

  // B. 10 ảnh nhiều nguyên liệu
  { id: 'b01', group: 'multi', query: 'farmers market vegetable stall photograph', file: 'b01-rau-cu-mix.jpg', expected: ['nhiều loại rau củ'] },
  { id: 'b02', group: 'multi', query: 'wok stir fry beef broccoli carrot photo', file: 'b02-xao-thit-rau.jpg', expected: ['thịt', 'rau'] },
  { id: 'b03', group: 'multi', query: 'shakshuka eggs poached tomato sauce', file: 'b03-trung-ca-chua.jpg', expected: ['trứng', 'cà chua'] },
  { id: 'b04', group: 'multi', query: 'fish market ice display photograph', file: 'b04-hai-san.jpg', expected: ['nhiều loại hải sản'] },
  { id: 'b05', group: 'multi', query: 'fruit stand market colorful photograph', file: 'b05-gio-trai-cay.jpg', expected: ['nhiều loại trái cây'] },
  { id: 'b06', group: 'multi', query: 'chopped vegetables cutting board photograph', file: 'b06-rau-cu-ban-bep.jpg', expected: ['nhiều loại rau củ'] },
  { id: 'b07', group: 'multi', query: 'raw whole chicken rosemary garlic photograph', file: 'b07-ga-gia-vi.jpg', expected: ['thịt gà', 'gia vị'] },
  { id: 'b08', group: 'multi', query: 'noodles vegetables meat dish', file: 'b08-mi-rau-thit.jpg', expected: ['mì', 'rau', 'thịt'] },
  { id: 'b09', group: 'multi', query: 'mixed salad vegetables bowl', file: 'b09-salad.jpg', expected: ['nhiều loại rau'] },
  { id: 'b10', group: 'multi', query: 'vegetables and spices kitchen photograph', file: 'b10-nguyen-lieu-flatlay.jpg', expected: ['nhiều nguyên liệu'] },

  // C. 5 ảnh khó/dễ nhầm
  { id: 'c01', group: 'confusable', query: 'lime citrus fruit green', file: 'c01-chanh.jpg', expected: ['chanh'], note: 'dễ nhầm chanh ta/chanh vàng' },
  { id: 'c02', group: 'confusable', query: 'sweet potato raw', file: 'c02-khoai-lang.jpg', expected: ['khoai lang'], note: 'dễ nhầm khoai tây' },
  { id: 'c03', group: 'confusable', query: 'red bell pepper', file: 'c03-ot-chuong.jpg', expected: ['ớt chuông'], note: 'dễ nhầm cà chua' },
  { id: 'c04', group: 'confusable', query: 'shallot onion small', file: 'c04-hanh-tim.jpg', expected: ['hành tím'], note: 'dễ nhầm tỏi/hành tây' },
  { id: 'c05', group: 'confusable', query: 'cherry tomatoes cluster', file: 'c05-ca-chua-bi.jpg', expected: ['cà chua bi'], note: 'dễ nhầm nho đỏ' },

  // D. 5 ảnh không có nguyên liệu
  { id: 'd01', group: 'none', query: 'car vehicle road', file: 'd01-oto.jpg', expected: [] },
  { id: 'd02', group: 'none', query: 'laptop computer desk', file: 'd02-laptop.jpg', expected: [] },
  { id: 'd03', group: 'none', query: 'stack of books photograph library', file: 'd03-sach.jpg', expected: [] },
  { id: 'd04', group: 'none', query: 'wooden chair furniture', file: 'd04-ghe.jpg', expected: [] },
  { id: 'd05', group: 'none', query: 'building architecture city', file: 'd05-toa-nha.jpg', expected: [] },
]

// Loại kết quả không phải ảnh chụp thật: trang scan sách/tài liệu cũ (Internet Archive, đuôi
// ".pdf" trong tên file dù mime báo image/jpeg vì là trang render từ PDF) và icon/emoji SVG.
const NOT_A_REAL_PHOTO = /\(IA[_ ]|\.pdf|\.svg/i

async function searchCommons(query) {
  const url = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(
    query,
  )}&gsrnamespace=6&gsrlimit=8&prop=imageinfo&iiprop=url|size|mime&iiurlwidth=800&format=json`
  const res = await fetch(url, { headers: { 'User-Agent': 'Dishcover-eval/1.0 (student project)' } })
  if (!res.ok) throw new Error(`Commons API lỗi: ${res.status}`)
  const data = await res.json()
  const pages = data.query?.pages
  if (!pages) return null
  const candidates = Object.values(pages)
    .filter((p) => p.imageinfo?.[0]?.mime === 'image/jpeg')
    .filter((p) => !NOT_A_REAL_PHOTO.test(p.title))
  const page = candidates[0]
  const info = page?.imageinfo?.[0]
  if (!info) return null
  return { thumbUrl: info.thumburl, pageUrl: info.descriptionurl, title: page.title }
}

async function main() {
  await mkdir(new URL('./images/', import.meta.url), { recursive: true })
  const manifest = []

  for (const item of PLAN) {
    const outPath = new URL(`./images/${item.file}`, import.meta.url)
    process.stdout.write(`[${item.id}] "${item.query}" -> `)

    const already = await access(outPath).then(() => true).catch(() => false)
    if (already) {
      console.log('đã có, bỏ qua')
      manifest.push({ ...item, skipped: true })
      await sleep(200)
      continue
    }

    try {
      const found = await withRetry(() => searchCommons(item.query), item.id)
      if (!found) {
        console.log('KHÔNG TÌM THẤY')
        manifest.push({ ...item, sourceUrl: null, sourcePage: null, error: 'not found' })
        continue
      }
      await sleep(1500)
      const imgRes = await withRetry(async () => {
        const r = await fetch(found.thumbUrl)
        if (!r.ok) throw new Error(`${r.status}`)
        return r
      }, item.id)
      const buf = Buffer.from(await imgRes.arrayBuffer())
      await writeFile(outPath, buf)
      console.log(`OK (${found.title}, ${(buf.length / 1024).toFixed(0)}KB)`)
      manifest.push({ ...item, sourceUrl: found.thumbUrl, sourcePage: found.pageUrl, sourceTitle: found.title, sizeBytes: buf.length })
    } catch (err) {
      console.log(`LỖI: ${err.message}`)
      manifest.push({ ...item, error: err.message })
    }
    // lịch sự với API — không spam liên tục
    await sleep(2000)
  }

  await writeFile(
    new URL('./images/manifest.json', import.meta.url),
    JSON.stringify(manifest, null, 2),
    'utf-8',
  )
  console.log(`\nXong. ${manifest.filter((m) => !m.error).length}/${PLAN.length} ảnh tải thành công.`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
