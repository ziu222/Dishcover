# Kịch bản demo Circuit Breaker (CLAUDE.md mục 9/10.9)

Chứng minh Resilience4j THẬT chạy trên 2 service AI (RAG dùng `llm`, Image dùng `vision`) — không
chỉ đọc code. Bằng chứng gián tiếp đã có sẵn lúc eval ảnh (429 thật trigger circuit breaker), kịch
bản này tái tạo lại có chủ đích + quan sát được qua `/actuator/circuitbreakers`.

Cơ chế: `slidingWindowSize=10, failureRateThreshold=50%` — cứ 10 lời gọi gần nhất mà ≥5 lỗi thì
breaker mở (OPEN), các lời gọi sau đó bị **short-circuit tức thì** (không gọi ra ngoài nữa) trong
`waitDurationInOpenState=30s`, rồi chuyển HALF_OPEN cho thử lại.

## Chuẩn bị

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"  # JWT_SECRET dùng chung
```

## A) RAG Service — "tắt" (LLM API hỏng) → thấy fallback + breaker OPEN

```bash
# 1. Bật RAG với key LLM SAI có chủ đích (mọi lời gọi LLM đều lỗi)
JWT_SECRET=<secret-32-ký-tự> GEMINI_API_KEY=sai-co-chu-y ./mvnw -pl rag spring-boot:run

# 2. (terminal khác) Mint token + xem trạng thái ban đầu — phải là CLOSED
JWT_SECRET=<cùng-secret> TOKEN=$(node eval/circuit-breaker/mint-jwt.mjs)
curl -s localhost:8085/actuator/circuitbreakers | grep -o '"state":"[A-Z_]*"'

# 3. Bắn liên tiếp 10 câu hỏi — vượt ngưỡng 50% lỗi trong sliding window 10
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "call $i: %{http_code} %{time_total}s\n" \
    -X POST localhost:8085/chat -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"message":"còn trứng và cà chua thì nấu gì?"}'
done

# 4. Xem trạng thái — phải chuyển OPEN
curl -s localhost:8085/actuator/circuitbreakers | grep -o '"state":"[A-Z_]*"'
```

**Đọc kết quả**: vài call đầu chậm (thật sự gọi Gemini rồi timeout/lỗi), từ lúc breaker OPEN các
call sau trả về **gần như tức thì** (không còn gọi mạng ra ngoài) — so `time_total` giữa call đầu
và call cuối để thấy rõ. Response `body` mỗi call vẫn `200` với `fallback:true` (CLAUDE.md mục 6
"Fallback bắt buộc" — trả danh sách công thức thô, không màn hình lỗi).

## B) RAG Service — "bật" (khôi phục) → breaker về CLOSED

```bash
# Dừng (Ctrl+C) rồi bật lại với key thật
JWT_SECRET=<cùng-secret> GEMINI_API_KEY=<key-thật> ./mvnw -pl rag spring-boot:run

curl -s -X POST localhost:8085/chat -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"message":"còn trứng và cà chua thì nấu gì?"}'
curl -s localhost:8085/actuator/circuitbreakers | grep -o '"state":"[A-Z_]*"'
# -> fallback:false (LLM trả lời thật), state CLOSED
```

## C) Image Service — cùng bài, breaker "vision"

```bash
# 1. Bật Image với key SAI
JWT_SECRET=<cùng-secret> OPENAI_API_KEY=sai-co-chu-y ./mvnw -pl image spring-boot:run

# 2. Bắn 10 ảnh liên tiếp (dùng ảnh có sẵn trong eval/image/images/, hoặc bất kỳ ảnh jpg/png/webp)
IMG=eval/image/images/single_01.jpg   # đổi đúng tên file có thật
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "call $i: %{http_code} %{time_total}s\n" \
    -X POST localhost:8086/recognize -H "Authorization: Bearer $TOKEN" \
    -F "file=@${IMG};type=image/jpeg"
done

# 3. Trạng thái + xác nhận fallback 503 VISION_UNAVAILABLE
curl -s localhost:8086/actuator/circuitbreakers | grep -o '"state":"[A-Z_]*"'
curl -s -X POST localhost:8086/recognize -H "Authorization: Bearer $TOKEN" \
  -F "file=@${IMG};type=image/jpeg"
# -> 503 {"code":"VISION_UNAVAILABLE", ...} tức thì, không đợi timeout 15s
```

Khôi phục: dừng, bật lại `OPENAI_API_KEY=<key-thật>`, gọi lại → 200 trả kết quả thật + state CLOSED.

## Trình bày trước hội đồng

Mở 2 terminal cạnh nhau: 1 chạy vòng lặp `curl` (bước A.3/C.2), 1 poll
`watch -n1 'curl -s localhost:8085/actuator/circuitbreakers | grep state'` — thấy trạng thái nhảy
CLOSED → OPEN trực tiếp trong lúc đang bắn request, kèm `time_total` rớt xuống gần 0 ngay khi mở.
