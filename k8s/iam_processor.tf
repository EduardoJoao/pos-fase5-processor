# Busca o cluster EKS já existente por nome
data "aws_eks_cluster" "eks" {
  name = "events-cluster"  # Nome do cluster EKS existente
}

# Obtém o certificado do OIDC provider
data "tls_certificate" "eks" {
  url = data.aws_eks_cluster.eks.identity[0].oidc[0].issuer
}

# Cria o OIDC provider se ele não existir
resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
  url             = data.aws_eks_cluster.eks.identity[0].oidc[0].issuer
}

# Busca os buckets S3 existentes por nome
data "aws_s3_bucket" "user_video_uploads" {
  bucket = "fiapfase5-user-video-uploads"
}

data "aws_s3_bucket" "processed_videos" {
  bucket = "fiapfase5-processed-videos"
}

# Busca a fila SQS por nome
data "aws_sqs_queue" "video_processing_queue" {
  name = "video-processing-queue"
}

# IAM Role para o processor
resource "aws_iam_role" "processor_irsa" {
  name = "processor-irsa-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(data.aws_eks_cluster.eks.identity[0].oidc[0].issuer, "https://", "")}:sub" = "system:serviceaccount:default:processor-sa"
          }
        }
      }
    ]
  })
}

# Política de acesso ao S3 e SQS
resource "aws_iam_policy" "processor_policy" {
  name = "processor-s3-sqs-policy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Resource = [
          data.aws_s3_bucket.user_video_uploads.arn,
          "${data.aws_s3_bucket.user_video_uploads.arn}/*",
          data.aws_s3_bucket.processed_videos.arn,
          "${data.aws_s3_bucket.processed_videos.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = data.aws_sqs_queue.video_processing_queue.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "processor_attach" {
  role       = aws_iam_role.processor_irsa.name
  policy_arn = aws_iam_policy.processor_policy.arn
}