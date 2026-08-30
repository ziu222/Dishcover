// Bộ đánh giá Chatbot RAG (CLAUDE.md mục 6 "Đánh giá") — chạy qua Gateway thật, KHÔNG mock.
// Dùng: node eval/run-chatbot-eval.mjs
//
// Đo 3 tiêu chí: (a) tỉ lệ chỉ nhắc món có thật trong hệ thống (proxy tự động: quét tên món xuất
// hiện trong câu trả lời, đối chiếu với sourceRecipeIds — không thay thế hoàn toàn việc đọc thủ
// công vì tên món tiếng Việt có thể trùng lặp/là con của nhau), (b) có tìm ra gợi ý phù hợp khi
// câu hỏi có nguyên liệu cụ thể hay không, (c) thời gian phản hồi.
//
// Lưu ý phạm vi: CLAUDE.md mục 6 mô tả so sánh "bản A (keyword) vs bản B (hybrid + vector)" —
// giai đoạn B (vector search pgvector) CHƯA được cài đặt (mục 10.7: RAG mới chỉ có giai đoạn A,
// lọc cứng qua Matching Service). Vì vậy bộ này chỉ đánh giá 1 bản hiện có (giai đoạn A), không
// có gì để so sánh A/B — ghi rõ trong báo cáo thay vì giả vờ có 2 bản.

const GATEWAY = 'http://localhost:8080'
const EMAIL = 'test.larder@example.com'
const PASSWORD = 'Test123456'

async function login() {
  const res = await fetch(`${GATEWAY}/user-service/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  })
  if (!res.ok) throw new Error(`Login thất bại: ${res.status} ${await res.text()}`)
  const setCookie = res.headers.get('set-cookie')
  if (!setCookie) throw new Error('Không thấy Set-Cookie sau khi login')
  return setCookie.split(';')[0] // "auth_token=..."
}

async function fetchAllRecipes(cookie) {
  const res = await fetch(`${GATEWAY}/recipe-service/recipes?size=500`, {
    headers: { Cookie: cookie },
  })
  if (!res.ok) throw new Error(`Không tải được danh sách công thức: ${res.status}`)
  const page = await res.json()
  return page.content // [{id, name, slug, ...}]
}

async function chat(cookie, message, conversationId) {
  const started = Date.now()
  const res = await fetch(`${GATEWAY}/rag-service/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookie },
    body: JSON.stringify({ message, conversationId }),
  })
  const latencyMs = Date.now() - started
  if (!res.ok) {
    return { error: `${res.status} ${await res.text()}`, latencyMs }
  }
  const body = await res.json()
  return { ...body, latencyMs }
}

/** Quét câu trả lời tìm tên món có thật xuất hiện nguyên văn (không phân biệt hoa/thường). */
function findMentionedRecipes(answer, allRecipes) {
  const lower = answer.toLowerCase()
  return allRecipes.filter((r) => r.name.length >= 4 && lower.includes(r.name.toLowerCase()))
}

async function main() {
  console.log('Đăng nhập...')
  const cookie = await login()

  console.log('Tải danh sách công thức thật...')
  const allRecipes = await fetchAllRecipes(cookie)
  const byId = new Map(allRecipes.map((r) => [r.id, r]))
  console.log(`  ${allRecipes.length} công thức trong hệ thống.`)

  const questions = JSON.parse(
    await (await import('node:fs/promises')).readFile(
      new URL('./chatbot-questions.json', import.meta.url),
    ),
  )

  const results = []
  for (const q of questions) {
    process.stdout.write(`[${q.id}] ${q.category} — "${q.message.slice(0, 40)}..." `)
    const conversationId = `eval-${q.id}-${Math.random().toString(36).slice(2)}`
    const res = await chat(cookie, q.message, conversationId)

    if (res.error) {
      console.log(`LỖI: ${res.error}`)
      results.push({ ...q, error: res.error, latencyMs: res.latencyMs })
      continue
    }

    const mentioned = findMentionedRecipes(res.answer, allRecipes)
    const sourceNames = new Set((res.sourceRecipeIds ?? []).map((id) => byId.get(id)?.name).filter(Boolean))
    const unsourcedMentions = mentioned.filter((r) => !sourceNames.has(r.name))
    const onlyRealDishes = res.fallback || unsourcedMentions.length === 0

    results.push({
      ...q,
      answer: res.answer,
      fallback: res.fallback,
      sourceRecipeIds: res.sourceRecipeIds ?? [],
      latencyMs: res.latencyMs,
      mentionedRecipes: mentioned.map((r) => r.name),
      unsourcedMentions: unsourcedMentions.map((r) => r.name),
      onlyRealDishes,
      foundSuggestion: (res.sourceRecipeIds ?? []).length > 0,
    })
    console.log(`${res.latencyMs}ms, fallback=${res.fallback}, nguồn=${(res.sourceRecipeIds ?? []).length}`)
  }

  const fs = await import('node:fs/promises')
  const outDir = new URL('./results/', import.meta.url)
  await fs.mkdir(outDir, { recursive: true })
  const rawPath = new URL('./chatbot-run.json', outDir)
  await fs.writeFile(rawPath, JSON.stringify(results, null, 2), 'utf-8')

  // ---- Tổng hợp ----
  const withAnswer = results.filter((r) => !r.error)
  const onlyRealCount = withAnswer.filter((r) => r.onlyRealDishes).length
  const ingredientQs = withAnswer.filter((r) =>
    ['INGREDIENT_MATCH', 'ALLERGY_AWARE'].includes(r.category),
  )
  const foundSuggestionCount = ingredientQs.filter((r) => r.foundSuggestion).length
  const latencies = withAnswer.map((r) => r.latencyMs)
  const avgLatency = Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length)
  const maxLatency = Math.max(...latencies)
  const flagged = withAnswer.filter((r) => !r.onlyRealDishes)

  const report = `# Đánh giá Chatbot RAG (giai đoạn A) — ${new Date().toISOString().slice(0, 10)}

**Lưu ý phạm vi**: CLAUDE.md mục 6 mô tả so sánh bản A (keyword) vs bản B (hybrid + vector).
Giai đoạn B (vector search pgvector) CHƯA được cài đặt (mục 10.7) — bộ này chỉ đo 1 bản hiện có
(giai đoạn A: trích xuất nguyên liệu + lọc cứng qua Matching Service), không có gì để so sánh A/B.

## Tổng số câu hỏi: ${results.length} (${withAnswer.length} có phản hồi, ${results.length - withAnswer.length} lỗi)

## (a) Tỉ lệ chỉ nhắc món có thật trong hệ thống
${onlyRealCount}/${withAnswer.length} (${((onlyRealCount / withAnswer.length) * 100).toFixed(1)}%)
đo bằng cách quét tên món trong câu trả lời, đối chiếu \`sourceRecipeIds\` — đây là proxy tự động,
KHÔNG thay thế việc đọc thủ công vì tên món tiếng Việt có thể là chuỗi con của nhau (VD "Mì
Fettuccine Alfredo" chứa "Mì Fettuccine sốt Alfredo" một phần).

${flagged.length > 0 ? `**${flagged.length} câu bị gắn cờ cần xem thủ công** (nhắc tên món không nằm trong nguồn đã dẫn):\n${flagged.map((r) => `- [${r.id}] "${r.message}" → nhắc: ${r.unsourcedMentions.join(', ')}`).join('\n')}` : '_Không có câu nào bị gắn cờ._'}

## (b) Tìm được gợi ý khi câu hỏi có nguyên liệu cụ thể
${foundSuggestionCount}/${ingredientQs.length} câu loại INGREDIENT_MATCH/ALLERGY_AWARE có ít nhất 1 công thức nguồn.

## (c) Thời gian phản hồi
- Trung bình: ${avgLatency}ms
- Cao nhất: ${maxLatency}ms
- Tất cả: ${latencies.join(', ')}ms

## Chi tiết theo câu hỏi

| ID | Nhóm | Câu hỏi | Fallback | Nguồn | Chỉ món thật? | Latency |
|---|---|---|---|---|---|---|
${withAnswer.map((r) => `| ${r.id} | ${r.category} | ${r.message.replace(/\|/g, '\\|')} | ${r.fallback} | ${r.sourceRecipeIds.length} | ${r.onlyRealDishes ? '✅' : '⚠️'} | ${r.latencyMs}ms |`).join('\n')}
${results.filter((r) => r.error).map((r) => `| ${r.id} | ${r.category} | ${r.message.replace(/\|/g, '\\|')} | — | — | LỖI: ${r.error} | ${r.latencyMs}ms |`).join('\n')}

Dữ liệu thô (câu trả lời đầy đủ từng câu): xem \`chatbot-run.json\` cùng thư mục.
`

  await fs.writeFile(new URL('./chatbot-report.md', outDir), report, 'utf-8')
  console.log('\n--- Xong ---')
  console.log(report)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
