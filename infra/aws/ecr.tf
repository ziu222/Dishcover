locals {
  # 1 nguồn sự thật cho danh sách 8 service thật (KHÔNG gồm "config" — skeleton chưa dùng tới,
  # xem CLAUDE.md mục 12). Port khớp application.yml mỗi module.
  # Terraform hợp nhất kiểu object của map literal về GIAO của thuộc tính chung nếu không khớp key
  # (không tự điền null như "optional attribute" của variable có type tường minh) — khai đủ 4 field
  # cho mọi service, tránh lỗi "object does not have an attribute" lúc dùng each.value.cpu ở nơi khác.
  services = {
    gateway      = { port = 8080, public = true, cpu = null, memory = null }
    user         = { port = 8081, public = false, cpu = null, memory = null }
    inventory    = { port = 8082, public = false, cpu = null, memory = null }
    recipe       = { port = 8083, public = false, cpu = null, memory = null }
    matching     = { port = 8084, public = false, cpu = null, memory = null }
    rag          = { port = 8085, public = false, cpu = 512, memory = 1024 } # gọi LLM, cần thêm chút đầu
    image        = { port = 8086, public = false, cpu = 512, memory = 1024 } # Vision API, ảnh nặng hơn JSON thường
    notification = { port = 8087, public = false, cpu = null, memory = null }
  }
}

resource "aws_ecr_repository" "services" {
  for_each             = local.services
  name                 = "${var.project}/${each.key}"
  image_tag_mutability = "IMMUTABLE" # không cho ghi đè tag đã push — tránh nhầm image cũ/mới

  image_scanning_configuration {
    scan_on_push = true # MUST DO: container scanning trong CI/CD (Trivy ở GitHub Actions bổ sung thêm, không thay thế)
  }

  tags = { Project = var.project }
}

output "ecr_repository_urls" {
  value = { for k, r in aws_ecr_repository.services : k => r.repository_url }
}
