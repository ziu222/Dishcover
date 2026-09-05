# --- ECS task execution role (kéo image từ ECR, ghi log CloudWatch, đọc Secrets Manager) ---

data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  name               = "${var.project}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["arn:aws:secretsmanager:${var.aws_region}:*:secret:${var.project}/*"]
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "${var.project}-read-secrets"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

# Task role — quyền của chính ứng dụng lúc chạy. Hiện các service không gọi API AWS nào trực tiếp
# (Cloudinary/Gemini/OpenAI đều là HTTP ra ngoài, không cần SDK/IAM) nên để rỗng, không gắn policy
# nào — ponytail: không cấp quyền "phòng khi cần sau này".
resource "aws_iam_role" "ecs_task" {
  name               = "${var.project}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

# --- GitHub Actions OIDC — cho CI/CD assume role để deploy mà KHÔNG cần lưu access key dài hạn
# trong GitHub Secrets (best practice hiện tại, thay vì AWS_ACCESS_KEY_ID/SECRET_ACCESS_KEY tĩnh). ---

data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
}

variable "github_repo" {
  description = "owner/repo — giới hạn role chỉ assume được từ đúng repo này"
  type        = string
  default     = "ziu222/Dishcover"
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      # Job trong deploy.yml dùng "environment: aws-production" — GitHub đổi định dạng OIDC
      # subject sang "repo:<owner>/<repo>:environment:aws-production" (KHÔNG phải dạng
      # "ref:refs/heads/<branch>" như job thường), phát hiện lúc chạy thật deploy đầu tiên
      # ("Not authorized to perform sts:AssumeRoleWithWebIdentity"). Đặt tên "aws-production"
      # (không phải "production") để tránh case-collision: GitHub tự khớp environment không phân
      # biệt hoa/thường khi RESOLVE (VD "production" tự trỏ vào "Production" có sẵn từ Vercel),
      # nhưng OIDC subject lại dùng ĐÚNG tên gốc có phân biệt hoa/thường — IAM StringLike cũng
      # phân biệt hoa/thường, lệch 1 ký tự hoa/thường là bị từ chối. toggle-infra.yml không đặt
      # environment nên vẫn dùng dạng ref — phải cho phép cả 2 định dạng.
      values = [
        "repo:${var.github_repo}:environment:aws-production",
        "repo:${var.github_repo}:ref:refs/heads/master",
      ]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = "${var.project}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume.json
}

# Quyền tối thiểu để build/push image + update ECS service. KHÔNG dùng AdministratorAccess cho
# role CI dù account setup ban đầu có thể cần admin (đó là việc của người chạy `terraform apply`
# thủ công lần đầu, không phải của CI).
data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid = "ECR"
    actions = [
      "ecr:GetAuthorizationToken",
      "ecr:BatchCheckLayerAvailability",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
    ]
    resources = ["*"]
  }
  statement {
    sid       = "ECSDeploy"
    actions   = ["ecs:UpdateService", "ecs:DescribeServices", "ecs:RegisterTaskDefinition", "ecs:DescribeTaskDefinition"]
    resources = ["*"]
  }
  statement {
    # Bật/tắt hạ tầng theo yêu cầu (infra/aws/toggle.sh, .github/workflows/toggle-infra.yml) —
    # scale ECS service về 0 + stop/start RDS khi không demo, tránh trả tiền 24/7.
    sid       = "ToggleInfra"
    actions   = ["rds:StopDBInstance", "rds:StartDBInstance", "rds:DescribeDBInstances"]
    resources = [aws_db_instance.postgres.arn]
  }
  statement {
    sid       = "PassRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.ecs_execution.arn, aws_iam_role.ecs_task.arn]
  }
  statement {
    sid       = "FrontendDeployS3"
    actions   = ["s3:PutObject", "s3:DeleteObject", "s3:ListBucket"]
    resources = [aws_s3_bucket.frontend.arn, "${aws_s3_bucket.frontend.arn}/*"]
  }
  statement {
    sid       = "FrontendDeployCloudFront"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "${var.project}-deploy-policy"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}

output "github_deploy_role_arn" {
  value       = aws_iam_role.github_deploy.arn
  description = "Set làm GitHub Actions secret AWS_DEPLOY_ROLE_ARN"
}
