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
  }

  # Local state by default - fine for a solo, short-lived school project.
  # If you ever collaborate or run terraform from CI, switch to an S3
  # backend (with a DynamoDB lock table) instead of local state.
}

provider "aws" {
  region = var.aws_region
}