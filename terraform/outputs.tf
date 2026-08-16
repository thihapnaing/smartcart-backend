output "cluster_name" {
  description = "EKS cluster name - used by `aws eks update-kubeconfig --name`."
  value       = module.eks.cluster_name
}

output "aws_region" {
  description = "Region the cluster lives in."
  value       = var.aws_region
}

output "github_actions_role_arn" {
  description = "Paste this into the AWS_DEPLOY_ROLE_ARN GitHub Actions secret."
  value       = aws_iam_role.github_actions_deploy.arn
}

output "ecr_backend_url" {
  description = "Push backend images here."
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_ai_service_url" {
  description = "Push ai-service images here."
  value       = aws_ecr_repository.ai_service.repository_url
}

output "ecr_frontend_url" {
  description = "Push frontend images here."
  value       = aws_ecr_repository.frontend.repository_url
}