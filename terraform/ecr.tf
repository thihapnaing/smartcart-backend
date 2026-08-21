# One repo per image. force_delete lets `terraform destroy` clean these up
# even if they still hold pushed images - convenient for a short-lived
# project you intend to tear down; drop it if you want deletion protection.

resource "aws_ecr_repository" "backend" {
  name                 = "smartcart-backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "ai_service" {
  name                 = "smartcart-ai-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "frontend" {
  name                 = "smartcart-frontend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}