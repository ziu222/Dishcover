// Bộ đánh giá Image Recognition Service (CLAUDE.md mục 11) — 30 ảnh thật (Wikimedia Commons,
// giấy phép mở), chạy qua Gateway thật với Vision API thật. Chạy: node eval/run-image-eval.mjs
//
// Cách chấm điểm (ghi rõ vì không có bộ nhãn "vàng" chi tiết cho từng ảnh multi-nguyên-liệu):
// - Nhóm "single"/"confusable" (1 nguyên liệu rõ ràng, biết chính xác tên): so khớp keyword
//   không phân biệt dấu giữa `expected` và tên/normalizedName Vision trả về — coi là tìm đúng
//   (hit) nếu khớp ít nhất 1 item.
// - Nhóm "multi" (nhiều nguyên liệu): không chấm theo từng nguyên liệu cụ thể (không có nhãn vàng
//   chi tiết), chấm theo tiêu chí "phát hiện được ≥2 nguyên liệu khác nhau" — đúng tinh thần thiết
//   kế mục 11 (ảnh nhiều nguyên liệu phải nhận diện được nhiều hơn 1 món).
// - Nhóm "none" (không có nguyên liệu): đúng nếu Vision trả về mảng rỗng (không tự bịa ra nguyên
//   liệu không tồn tại) — mọi item được trả là 1 false positive.

const GATEWAY = 'http://localhost:8080'
const EMAIL = 'test.larder@example.com'
const PASSWORD = 'Test123456'

function fold(s) {
  return s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
}

async function login() {
  const res = await fetch(`${GATEWAY}/user-service/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  })
  if (!res.ok) throw new Error(`Login thất bại: ${res.status} ${await res.text()}`)
  return res.headers.get('set-cookie').split(';')[0]
}

async function recognize(cookie, filePath) {
  const fs = await import('node:fs/promises')
  const buf = await fs.readFile(filePath)
  const form = new FormData()
  form.append('file', new Blob([buf], { type: 'image/jpeg' }), 'test.jpg')

  const started = Date.now()
  const res = await fetch(`${GATEWAY}/image-service/recognize`, {
    method: 'POST',
    headers: { Cookie: cookie },
    body: form,
  })
  const latencyMs = Date.now() - started
  if (!res.ok) return { error: `${res.status} ${await res.text()}`, latencyMs }
  const body = await res.json()
  return { items: body.items ?? [], latencyMs }
}

async function main() {
  const fs = await import('node:fs/promises')
  const manifest = JSON.parse(
    await fs.readFile(new URL('./images/manifest.json', import.meta.url)),
  )

  console.log('Đăng nhập...')
  const cookie = await login()

  // Resume: bỏ qua ảnh đã chấm thành công ở lần chạy trước (OpenAI rate-limit 429 dễ làm circuit
  // breaker mở giữa chừng — không cần chạy lại từ đầu).
  const prevPath = new URL('./results/image-run.json', import.meta.url)
  const prev = await fs
    .readFile(prevPath)
    .then((b) => JSON.parse(b))
    .catch(() => [])
  const prevById = new Map(prev.map((r) => [r.id, r]))

  const results = []
  for (const item of manifest) {
    if (item.error) continue
    const cached = prevById.get(item.id)
    if (cached && !cached.error) {
      console.log(`[${item.id}] ${item.group} "${item.file}" -> (đã có kết quả, bỏ qua)`)
      results.push(cached)
      continue
    }
    const path = new URL(`./images/${item.file}`, import.meta.url)
    process.stdout.write(`[${item.id}] ${item.group} "${item.file}" -> `)
    const res = await recognize(cookie, path)
    if (res.error) {
      console.log(`LỖI: ${res.error}`)
      results.push({ ...item, error: res.error, latencyMs: res.latencyMs })
      // 503 nghĩa là circuit breaker đang mở (hoặc OpenAI rate-limit) — đợi lâu hơn hẳn để nó
      // có cơ hội đóng lại (waitDurationInOpenState 30s, xem application.yml) trước khi gọi tiếp.
      await new Promise((r) => setTimeout(r, 35_000))
      continue
    }

    const detected = res.items.map((i) => ({ name: i.name, confidence: i.confidence }))
    let hit = false
    if (item.group === 'single' || item.group === 'confusable') {
      const expectedFold = item.expected.map(fold)
      hit = detected.some((d) => expectedFold.some((e) => fold(d.name).includes(e) || e.includes(fold(d.name))))
    } else if (item.group === 'multi') {
      hit = detected.length >= 2
    } else if (item.group === 'none') {
      hit = detected.length === 0
    }

    console.log(`${res.latencyMs}ms, ${detected.length} item(s) [${detected.map((d) => d.name).join(', ')}] -> ${hit ? 'ĐÚNG' : 'SAI'}`)
    results.push({ ...item, detected, hit, latencyMs: res.latencyMs })
    await new Promise((r) => setTimeout(r, 2500))
  }

  await fs.writeFile(
    new URL('./results/image-run.json', import.meta.url),
    JSON.stringify(results, null, 2),
    'utf-8',
  )

  const byGroup = {}
  for (const r of results) {
    if (r.error) continue
    byGroup[r.group] ??= { total: 0, hit: 0 }
    byGroup[r.group].total++
    if (r.hit) byGroup[r.group].hit++
  }
  const totalHit = results.filter((r) => r.hit).length
  const totalScored = results.filter((r) => !r.error).length
  const latencies = results.filter((r) => !r.error).map((r) => r.latencyMs)
  const avgLatency = Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length)

  // Precision/recall tổng: chỉ tính được đầy đủ cho nhóm single+confusable (có nhãn vàng rõ ràng
  // theo từng item); nhóm multi/none dùng tiêu chí nhị phân "đúng/sai" như mô tả ở đầu file.
  const singleLike = results.filter((r) => !r.error && (r.group === 'single' || r.group === 'confusable'))
  const recall = singleLike.length ? singleLike.filter((r) => r.hit).length / singleLike.length : 0
  const noneGroup = results.filter((r) => !r.error && r.group === 'none')
  const falsePositiveRate = noneGroup.length ? noneGroup.filter((r) => !r.hit).length / noneGroup.length : 0

  const report = `# Đánh giá Image Recognition Service — ${new Date().toISOString().slice(0, 10)}

30 ảnh thật (Wikimedia Commons, giấy phép mở — nguồn từng ảnh xem \`images/manifest.json\`),
Vision API thật (OpenAI gpt-4o-mini qua Gateway → Image Service).

## Tổng quan
- Tổng số ảnh chấm được: ${totalScored}/${manifest.length}
- Đúng: ${totalHit}/${totalScored} (${((totalHit / totalScored) * 100).toFixed(1)}%)
- Latency trung bình: ${avgLatency}ms

## Theo nhóm
${Object.entries(byGroup)
  .map(([g, s]) => `- **${g}**: ${s.hit}/${s.total} (${((s.hit / s.total) * 100).toFixed(1)}%)`)
  .join('\n')}

## Recall trên nhóm có nhãn vàng rõ ràng (single + confusable, ${singleLike.length} ảnh)
${(recall * 100).toFixed(1)}% — nhận đúng nguyên liệu khi ảnh chỉ có 1 nguyên liệu rõ ràng.

## Tỉ lệ báo sai trên nhóm "không có nguyên liệu" (${noneGroup.length} ảnh)
${(falsePositiveRate * 100).toFixed(1)}% ảnh không có đồ ăn nhưng Vision vẫn bịa ra nguyên liệu.

## Chi tiết từng ảnh

| ID | Nhóm | File | Kỳ vọng | Nhận diện được | Kết quả | Latency |
|---|---|---|---|---|---|---|
${results
  .filter((r) => !r.error)
  .map(
    (r) =>
      `| ${r.id} | ${r.group} | ${r.file} | ${r.expected.join(', ') || '(không)'} | ${r.detected.map((d) => `${d.name} (${d.confidence.toFixed(2)})`).join(', ') || '(rỗng)'} | ${r.hit ? '✅' : '❌'} | ${r.latencyMs}ms |`,
  )
  .join('\n')}
${results.filter((r) => r.error).map((r) => `| ${r.id} | ${r.group} | ${r.file} | ${r.expected.join(', ')} | LỖI: ${r.error} | — | ${r.latencyMs}ms |`).join('\n')}

Dữ liệu thô: \`image-run.json\` cùng thư mục. Nguồn + giấy phép từng ảnh: \`../images/manifest.json\`.
`

  await fs.writeFile(new URL('./results/image-report.md', import.meta.url), report, 'utf-8')
  console.log('\n' + report)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
