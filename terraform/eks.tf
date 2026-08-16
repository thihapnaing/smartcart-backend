module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.cluster_name
  cluster_version = var.cluster_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.public_subnets

  # Nodes sit in public subnets (see vpc.tf) so the API server needs to stay
  # reachable from the public internet too - fine for a short-lived demo,
  # tighten this (private access + a bastion/VPN) for anything longer-lived.
  cluster_endpoint_public_access = true

  # Grants the identity running `terraform apply` (you) cluster-admin via EKS
  # access entries automatically - separate from the GitHub Actions role
  # below, which gets its own narrower access entry.
  enable_cluster_creator_admin_permissions = true

  eks_managed_node_groups = {
    default = {
      instance_types = var.node_instance_types
      min_size       = var.node_min_size
      max_size       = var.node_max_size
      desired_size   = var.node_desired_size
    }
  }

  # Let the GitHub Actions deploy role (defined in oidc.tf) run kubectl
  # against this cluster, via EKS access entries (the modern replacement for
  # hand-editing the aws-auth ConfigMap).
  access_entries = {
    github_actions = {
      principal_arn = aws_iam_role.github_actions_deploy.arn

      policy_associations = {
        admin = {
          policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = {
            type = "cluster"
          }
        }
      }
    }
  }
}