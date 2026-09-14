# Anh cong thuc do admin upload. Dung CHUNG CloudFront distribution voi frontend (them 1 origin +
# 1 cache behavior) thay vi mo bucket public: ca ha tang nay khong co bucket public nao, va de anh
# cung origin voi frontend thi khong dinh CORS, khong phai cau hinh them domain.
#
# URL anh: https://www.<domain>/recipe-images/<key>

resource "random_id" "recipe_images_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket" "recipe_images" {
  bucket = "${var.project}-recipe-images-${random_id.recipe_images_suffix.hex}"
}

resource "aws_s3_bucket_public_access_block" "recipe_images" {
  bucket                  = aws_s3_bucket.recipe_images.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "recipe_images" {
  name                              = "${var.project}-recipe-images-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_s3_bucket_policy" "recipe_images" {
  bucket = aws_s3_bucket.recipe_images.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFrontOAC"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.recipe_images.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.frontend.arn }
      }
    }]
  })
}

# Recipe Service ghi anh bang IAM task role — KHONG can access key nao (dung tinh than "secret cang
# it cang tot" o CLAUDE.md muc 9). Chi cho PutObject, khong cho Delete: xoa anh cu khong can thiet
# vi moi lan upload sinh key moi (co timestamp), va khong cho Delete thi mot bug cung khong the
# xoa nham anh cua cong thuc khac.
resource "aws_iam_role_policy" "ecs_task_recipe_images" {
  name = "${var.project}-recipe-images-write"
  role = aws_iam_role.ecs_task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject"]
      Resource = "${aws_s3_bucket.recipe_images.arn}/*"
    }]
  })
}

output "recipe_images_bucket" {
  value       = aws_s3_bucket.recipe_images.bucket
  description = "Dat vao bien moi truong RECIPE_IMAGES_BUCKET cua Recipe Service"
}
