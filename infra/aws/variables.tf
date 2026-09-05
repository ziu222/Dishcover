variable "aws_region" {
  type    = string
  default = "ap-southeast-1"
}

variable "project" {
  type    = string
  default = "dishcover"
}

variable "domain_name" {
  description = "Domain thật đã mua (Hostinger) — DNS vẫn quản lý ở Hostinger, KHÔNG chuyển sang Route 53. Dùng www.<domain> cho frontend + api.<domain> cho backend (tránh giới hạn CNAME ở apex domain, hầu hết registrar không hỗ trợ)."
  type        = string
  default     = "dishcover.online"
}

variable "image_tag" {
  description = "Tag Docker image sẽ deploy (CI/CD truyền vào = SHA commit, không dùng 'latest' cho production)"
  type        = string
  default     = "latest"
}

# --- Secrets (KHÔNG có default — bắt buộc truyền qua terraform.tfvars hoặc TF_VAR_*, không commit) ---

variable "postgres_master_password" {
  type      = string
  sensitive = true
}

variable "mongo_root_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  description = ">= 32 ký tự, dùng chung mọi service"
  type        = string
  sensitive   = true
}

variable "internal_service_secret" {
  type      = string
  sensitive = true
}

variable "gemini_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "openai_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "mail_username" {
  type    = string
  default = ""
}

variable "mail_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "turnstile_secret_key" {
  description = "Cloudflare Turnstile — để trống dùng key test mặc định trong code (KHÔNG dùng key test khi deploy thật)"
  type        = string
  sensitive   = true
  default     = ""
}

# --- Kích thước tài nguyên (ponytail: mặc định nhỏ nhất đủ chạy demo) ---

variable "app_cpu" {
  type    = number
  default = 256 # 0.25 vCPU
}

variable "app_memory" {
  type    = number
  default = 512 # MB
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}
