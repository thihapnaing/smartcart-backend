# Public-subnets-only VPC: no NAT Gateway, so nodes get public IPs directly.
# That's a deliberate cost/simplicity trade-off for a short-lived school
# project - a NAT Gateway alone runs ~$0.045/hr + data processing on top of
# the EKS control plane's ~$0.10/hr, for a resource this deployment doesn't
# otherwise need. Skip this shortcut for anything long-lived or handling
# real user data.

data "aws_availability_zones" "available" {
  state = "available"
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.cluster_name}-vpc"
  cidr = var.vpc_cidr

  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets  = [cidrsubnet(var.vpc_cidr, 8, 0), cidrsubnet(var.vpc_cidr, 8, 1)]
  private_subnets = []

  enable_nat_gateway      = false
  map_public_ip_on_launch = true

  # Required tags for the EKS/ELB integration to auto-discover these subnets.
  public_subnet_tags = {
    "kubernetes.io/role/elb"                    = "1"
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
  }
}