# SmartCart AWS infra

Provisions an EKS cluster, 3 ECR repos (backend, ai-service, frontend), and
an IAM role GitHub Actions can assume via OIDC to push images and deploy.
Built for a short-lived school-project deployment - see the cost/security
trade-offs called out in comments in `vpc.tf` and `eks.tf` before reusing
this for anything longer-lived.

MySQL runs in-cluster with ephemeral (`emptyDir`) storage: no EBS CSI driver
or extra IAM to manage, but data resets if the mysql pod restarts. Fine for
a demo that redeploys on every push anyway.

## One-time setup (run locally, not from CI)

Bootstrapping problem: the GitHub Actions OIDC role doesn't exist until
Terraform creates it, so this first `apply` has to run with your own AWS
credentials, not CI's.

1. Install Terraform and the AWS CLI, then `aws configure` (or set
   `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION` env vars).
2. `cp terraform.tfvars.example terraform.tfvars` and fill in `github_repo`
   with this repo's actual `owner/repo`.
3. `terraform init`
4. `terraform apply`
5. Copy the `github_actions_role_arn` output value.

## Wire up GitHub Actions

Add these as repo secrets (Settings → Secrets and variables → Actions):

- `AWS_DEPLOY_ROLE_ARN` - the `github_actions_role_arn` output from step 5
  above.
- `JWT_SECRET`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `OPENAI_API_KEY`,
  `OPENROUTER_API_KEY` - app credentials, consumed by the `deploy` job in
  `.github/workflows/backend.yml` to create the `smartcart-secrets` k8s
  Secret at deploy time. Not read by Terraform at all.

From here, pushes to `develop` build, scan, push to ECR, and deploy
automatically - see `.github/workflows/backend.yml`.

## Tearing down

`terraform destroy` - do this when you're done. The EKS control plane bills
hourly (~$0.10/hr) whether or not anything's running on it, and is the main
thing that'll rack up cost if left up past the few days you need it for.