variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
  default     = "smartcart-eks"
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS control plane. Leave null to let AWS pick its current default."
  type        = string
  default     = null
}

variable "vpc_cidr" {
  description = "CIDR block for the cluster VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "node_instance_types" {
  description = "EC2 instance types for the EKS managed node group."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 3
}

variable "github_repo" {
  description = "GitHub repo allowed to assume the deploy role, as \"owner/repo\" (e.g. \"thihapnaing/smartcart-backend\"). Restricts OIDC trust to this repo only."
  type        = string
}

variable "github_deploy_branch" {
  description = "Branch allowed to assume the deploy role via OIDC. Only pushes to this branch (not PRs, not other branches) can deploy."
  type        = string
  default     = "main"
}
