# Lets GitHub Actions assume an AWS role via short-lived OIDC tokens instead
# of long-lived AWS access keys stored as repo secrets.

data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
}

data "aws_iam_policy_document" "github_actions_trust" {
  statement {
    effect  = "Allow"
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

    # AWS requires every GitHub OIDC trust policy to condition on `sub` (or
    # `job_workflow_ref`), scoped to something other than "all" - it rejects
    # UpdateAssumeRolePolicy otherwise. This wildcard exists solely to
    # satisfy that guardrail; the real restriction is the `repository` and
    # `ref` conditions below.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:*"]
    }

    # Restricts which caller can assume this role: only workflow runs
    # triggered by a push to `github_deploy_branch` in `github_repo` - not
    # pull requests, not other branches, not other repos.
    #
    # Matched on the separate `repository` and `ref` claims rather than the
    # composite `sub` claim - GitHub now includes immutable owner/repo IDs
    # alongside the names in `sub` (e.g.
    # "repo:owner@123/repo@456:ref:refs/heads/develop" instead of
    # "repo:owner/repo:ref:refs/heads/develop"), which silently breaks any
    # trust policy written against the classic sub string format. Confirmed
    # via a debug workflow step that decoded a real token: `repository` and
    # `ref` come through as plain strings, unaffected by that change.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:repository"
      values   = [var.github_repo]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:ref"
      values   = ["refs/heads/${var.github_deploy_branch}"]
    }
  }
}

resource "aws_iam_role" "github_actions_deploy" {
  name               = "${var.cluster_name}-github-actions-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_actions_trust.json
}

# Least-privilege for what the CI deploy job actually does: push images to
# these 3 ECR repos, and describe the cluster (needed by
# `aws eks update-kubeconfig`). Actual kubectl permissions inside the
# cluster come from the EKS access entry in eks.tf, not IAM.
data "aws_iam_policy_document" "github_actions_permissions" {
  statement {
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"] # this action doesn't support resource-level restriction
  }

  statement {
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
    ]
    resources = [
      aws_ecr_repository.backend.arn,
      aws_ecr_repository.ai_service.arn,
      aws_ecr_repository.frontend.arn,
    ]
  }

  statement {
    effect    = "Allow"
    actions   = ["eks:DescribeCluster"]
    resources = [module.eks.cluster_arn]
  }
}

resource "aws_iam_role_policy" "github_actions_deploy" {
  name   = "deploy-permissions"
  role   = aws_iam_role.github_actions_deploy.id
  policy = data.aws_iam_policy_document.github_actions_permissions.json
}