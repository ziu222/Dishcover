# Frontend build tĩnh (Vite) — S3 (private, chỉ CloudFront đọc được qua OAC) + CloudFront.
# CI/CD build "npm run build" rồi "aws s3 sync frontend/dist/ s3://<bucket>" — xem
# .github/workflows/deploy.yml. Terraform chỉ dựng hạ tầng, KHÔNG tự build/upload code frontend.

resource "random_id" "frontend_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project}-frontend-${random_id.frontend_suffix.hex}"
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project}-frontend-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = ["www.${var.domain_name}"]

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "s3-frontend"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  # Bug thật phát hiện lúc live-verify: frontend (lib/api.ts) luôn gọi path tương đối
  # "/api/<x>-service/**" cùng origin (thiết kế cho Vite dev proxy local) — production KHÔNG có gì
  # route "/api/*" sang backend, S3 trả 403 cho mọi request (POST vào path không tồn tại như static
  # file). Origin domain dùng "api.<domain>" (không phải DNS name gốc của ALB) để CloudFront xác
  # thực đúng chứng chỉ ACM (issue cho api.<domain>, không match DNS name gốc *.elb.amazonaws.com).
  origin {
    domain_name = "api.${var.domain_name}"
    origin_id   = "alb-api"
    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "s3-frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }
  }

  # "/api/*" -> ALB -> Gateway. Không cache (API động), forward nguyên cookie (auth_token httpOnly)
  # + query string + mọi method — Gateway tự strip "/api" (xem gateway/application.yml route
  # "*-api-prefixed"). Vì frontend/API giờ cùng 1 origin qua CloudFront, request thật của trình
  # duyệt KHÔNG cần CORS nữa (CORS ở Gateway vẫn giữ cho việc gọi thẳng api.<domain> lúc test).
  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "alb-api"
    viewer_protocol_policy = "https-only"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    min_ttl                = 0
    default_ttl            = 0
    max_ttl                = 0
    forwarded_values {
      query_string = true
      headers      = ["Origin", "Authorization", "Content-Type"]
      cookies { forward = "all" }
    }
  }

  # SPA: mọi route không tìm thấy file tĩnh -> trả về index.html để React Router tự xử lý client-side
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.frontend.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFrontOAC"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.frontend.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.frontend.arn }
      }
    }]
  })
}

output "frontend_url" {
  value       = "https://www.${var.domain_name}"
  description = "URL thật sau khi thêm CNAME www.<domain_name> -> giá trị output cloudfront_domain_name ở Hostinger"
}

output "cloudfront_domain_name" {
  value       = aws_cloudfront_distribution.frontend.domain_name
  description = "CNAME giá trị này vào www.<domain_name> ở Hostinger (xem output next_steps để có tên miền thật đã điền sẵn)"
}

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "cloudfront_distribution_id" {
  value       = aws_cloudfront_distribution.frontend.id
  description = "Dùng để invalidate cache sau mỗi lần deploy mới: aws cloudfront create-invalidation --distribution-id <id> --paths '/*'"
}
