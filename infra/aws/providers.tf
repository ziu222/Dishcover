terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # ponytail: state cục bộ (terraform.tfstate), đủ cho 1 người demo. Nếu làm việc nhóm hoặc
  # deploy lâu dài, chuyển sang backend "s3" + DynamoDB lock table.
}

provider "aws" {
  region = var.aws_region
}
