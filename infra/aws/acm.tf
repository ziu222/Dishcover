# DNS quản lý ở Hostinger (KHÔNG dùng Route 53) — validate ACM qua DNS cần bạn tự thêm CNAME thủ
# công vào bảng DNS Hostinger (xem output "dns_records_to_add" sau lần "terraform apply" đầu tiên,
# apply sẽ DỪNG LẠI chờ ở aws_acm_certificate_validation cho tới khi bạn thêm đúng record).

resource "aws_acm_certificate" "api" {
  domain_name       = "api.${var.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for o in aws_acm_certificate.api.domain_validation_options : o.resource_record_name]
}

# us-east-1 bắt buộc cho CloudFront (provider alias khai ở providers.tf)
resource "aws_acm_certificate" "frontend" {
  provider          = aws.us_east_1
  domain_name       = "www.${var.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "frontend" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.frontend.arn
  validation_record_fqdns = [for o in aws_acm_certificate.frontend.domain_validation_options : o.resource_record_name]
}

output "acm_validation_records" {
  description = "Thêm CNAME này vào Hostinger TRƯỚC (apply sẽ treo ở bước validate cho tới khi thấy record) — mỗi cert 1 dòng, tên/giá trị AWS tự sinh."
  value = concat(
    [for o in aws_acm_certificate.api.domain_validation_options : {
      name  = o.resource_record_name
      type  = o.resource_record_type
      value = o.resource_record_value
    }],
    [for o in aws_acm_certificate.frontend.domain_validation_options : {
      name  = o.resource_record_name
      type  = o.resource_record_type
      value = o.resource_record_value
    }]
  )
}
