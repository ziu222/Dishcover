/* Nội dung tĩnh của trang "Về chúng tôi" — tách khỏi JSX cho dễ sửa, giống landingData.ts. */

export const stats = [
  { value: '08', label: 'dịch vụ chạy độc lập' },
  { value: '161', label: 'công thức trong kho' },
  { value: '194', label: 'nguyên liệu đã chuẩn hoá' },
  { value: '93,3%', label: 'ảnh nhận đúng nguyên liệu' },
]

export const pillars = [
  {
    index: '01',
    title: 'Tủ lạnh ảo',
    text: 'Ghi lại nguyên liệu đang có kèm hạn dùng, gộp theo lô và tự suy ngày hết hạn từ từ điển nguyên liệu. Thêm bằng tay hoặc chụp một tấm ảnh.',
  },
  {
    index: '02',
    title: 'Gợi ý theo nguyên liệu',
    text: 'Chấm điểm từng công thức bằng Jaccard có trọng số, cộng điểm cho thứ sắp hết hạn và mục tiêu calo, loại thẳng món chạm vào danh sách dị ứng.',
  },
  {
    index: '03',
    title: 'Trợ lý nấu ăn',
    text: 'Hỏi đáp tự nhiên nhưng chỉ nói về công thức có thật trong kho — mỗi câu trả lời truy ngược được về đúng công thức đã dùng để soạn.',
  },
]

export const services = [
  { name: 'API Gateway', role: 'Cửa duy nhất ra ngoài, kiểm tra JWT tập trung', store: '—' },
  { name: 'User Service', role: 'Tài khoản, OTP qua email, dị ứng và mục tiêu calo', store: 'PostgreSQL' },
  { name: 'Inventory Service', role: 'Tủ lạnh ảo, trừ kho sau khi nấu theo hạn dùng', store: 'PostgreSQL' },
  { name: 'Recipe Service', role: 'Kho công thức, nguyên liệu và các bước nấu lồng nhau', store: 'MongoDB' },
  { name: 'Matching Service', role: 'Chuỗi quy tắc chấm điểm và tìm kiếm theo vector', store: 'PostgreSQL + pgvector' },
  { name: 'RAG Chatbot', role: 'Truy hồi lai 4 kênh rồi mới gọi mô hình ngôn ngữ', store: '—' },
  { name: 'Image Service', role: 'Đọc ảnh nguyên liệu, luôn chờ người dùng xác nhận', store: '—' },
  { name: 'Notification Service', role: 'Nhắc hạn dùng qua Kafka, gửi in-app và email', store: 'PostgreSQL' },
]

export const stack = [
  {
    layer: 'Giao diện',
    items: ['React 19', 'TypeScript', 'Vite', 'Tailwind CSS v4', 'Framer Motion', 'Phosphor Icons'],
  },
  {
    layer: 'Dịch vụ',
    items: ['Java 21', 'Spring Boot 3.5', 'Spring Cloud Gateway', 'Spring AI', 'Resilience4j', 'Apache Kafka'],
  },
  {
    layer: 'Dữ liệu',
    items: ['PostgreSQL 16', 'pgvector (HNSW)', 'MongoDB 7', 'Flyway'],
  },
  {
    layer: 'Trí tuệ nhân tạo',
    items: ['GPT-4o-mini Vision', 'text-embedding-3-small', 'Gemini', 'Groq Llama 3.3'],
  },
  {
    layer: 'Vận hành',
    items: ['Docker Compose', 'AWS ECS', 'Terraform', 'GitHub Actions'],
  },
]
