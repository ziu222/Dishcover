/* Nội dung tĩnh của trang "Về chúng tôi" — tách khỏi JSX cho dễ sửa, giống landingData.ts.
   Quy ước chữ: giữ nguyên thuật ngữ tiếng Anh (service, database, embedding, LLM...) thay vì
   dịch cứng sang tiếng Việt; phần văn xuôi vẫn viết tiếng Việt. */

export const stats = [
  { value: '08', label: 'microservice chạy độc lập' },
  { value: '161', label: 'công thức trong database' },
  { value: '194', label: 'nguyên liệu đã chuẩn hoá' },
  { value: '93,3%', label: 'ảnh nhận đúng nguyên liệu' },
]

export const pillars = [
  {
    index: '01',
    title: 'Virtual Fridge',
    text: 'Ghi lại nguyên liệu đang có kèm hạn dùng, gộp theo lô và tự suy ngày hết hạn từ ingredient catalog. Thêm bằng tay hoặc chụp một tấm ảnh.',
  },
  {
    index: '02',
    title: 'Recipe Matching',
    text: 'Chấm điểm từng công thức bằng weighted Jaccard, cộng điểm cho nguyên liệu sắp hết hạn và mục tiêu calo, loại thẳng món chạm vào danh sách dị ứng.',
  },
  {
    index: '03',
    title: 'RAG Chatbot',
    text: 'Hỏi đáp tự nhiên nhưng chỉ nói về công thức có thật trong database — mỗi câu trả lời truy ngược được về đúng recipe đã dùng để soạn prompt.',
  },
]

export const services = [
  { name: 'API Gateway', role: 'Cửa duy nhất ra ngoài, xác thực JWT tập trung', store: '—' },
  { name: 'User Service', role: 'Account, email OTP, dietary preferences, calorie goal', store: 'PostgreSQL' },
  { name: 'Inventory Service', role: 'Virtual fridge, trừ kho sau khi nấu theo FEFO', store: 'PostgreSQL' },
  { name: 'Recipe Service', role: 'Recipe document lồng nhau: ingredients, steps, nutrition', store: 'MongoDB' },
  { name: 'Matching Service', role: 'Chuỗi scoring rule + vector search cho retrieval', store: 'PostgreSQL + pgvector' },
  { name: 'RAG Service', role: 'Hybrid retrieval 4 kênh rồi mới gọi LLM', store: '—' },
  { name: 'Image Service', role: 'Vision API đọc ảnh, luôn human-in-the-loop', store: '—' },
  { name: 'Notification Service', role: 'Cảnh báo hạn dùng qua Kafka, in-app và email', store: 'PostgreSQL' },
]

export const stack = [
  {
    layer: 'Frontend',
    items: ['React 19', 'TypeScript', 'Vite', 'Tailwind CSS v4', 'Framer Motion', 'Phosphor Icons'],
  },
  {
    layer: 'Backend',
    items: ['Java 21', 'Spring Boot 3.5', 'Spring Cloud Gateway', 'Spring AI', 'Resilience4j', 'Apache Kafka'],
  },
  {
    layer: 'Database',
    items: ['PostgreSQL 16', 'pgvector (HNSW)', 'MongoDB 7', 'Flyway'],
  },
  {
    layer: 'AI',
    items: ['GPT-4o-mini Vision', 'text-embedding-3-small', 'Gemini', 'Groq Llama 3.3'],
  },
  {
    layer: 'DevOps',
    items: ['Docker Compose', 'AWS ECS', 'Terraform', 'GitHub Actions'],
  },
]
