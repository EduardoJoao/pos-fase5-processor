# Serviço de Processamento de Vídeos - FIAP Fase 5

Este serviço é responsável pelo processamento assíncrono de vídeos, convertendo arquivos MP4 em frames individuais e compactando-os em arquivos ZIP para armazenamento.

## Responsabilidades

- Escuta contínua da fila SQS para mensagens de processamento de vídeo
- Download de vídeos MP4 do bucket S3 de uploads
- Extração de frames dos vídeos usando FFmpeg
- Compactação dos frames em arquivos ZIP
- Upload dos arquivos ZIP processados para bucket S3 de saída
- Atualização do status de processamento via core-api
- Envio de notificações por email em caso de erro
- Gerenciamento de estados de processamento (PENDING → PROCESSING → SUCCESS/ERROR)

## Fluxo de Processamento

### 1. Recepção de Mensagem
- O serviço escuta continuamente a fila SQS `video-processing-queue`
- Recebe mensagens com metadados do vídeo (videoId, clientId, s3Key, etc.)

### 2. Preparação
- Atualiza status do vídeo para `PROCESSING` via core-api
- Faz download do arquivo MP4 do bucket S3 `user-video-uploads`

### 3. Processamento
- Extrai frames do vídeo usando FFmpeg
- Processa os frames em memória para otimizar performance
- Compacta todos os frames em um arquivo ZIP

### 4. Finalização
- **Sucesso**: Faz upload do ZIP para bucket S3 `processed-videos` e atualiza status para `SUCCESS`
- **Erro**: Envia email de notificação ao usuário e atualiza status para `ERROR`
- Remove mensagem da fila SQS após processamento

## Tecnologias Utilizadas

- **Java 17** - Linguagem de programação
- **Spring Boot 3.2.4** - Framework principal
- **FFmpeg** - Processamento e extração de frames de vídeo
- **AWS SDK** - Integração com serviços AWS
- **SQS** - Fila de mensagens para processamento assíncrono
- **S3** - Armazenamento de arquivos (entrada e saída)
- **Docker** - Containerização da aplicação
- **Kubernetes** - Orquestração e deployment

## Configuração

### Variáveis de Ambiente Principais

```properties
# Configurações do Servidor
SPRING_PORT=8080

# Configurações S3
S3_BUCKET_NAME=fiapfase5-user-video-uploads
S3_DOWNLOAD_BUCKET=fiapfase5-processed-videos

# Configurações SQS
SQS_PROCESS=https://sqs.us-east-1.amazonaws.com/account/video-processing-queue
sqs.threads=4

# Configurações do core-api
CORE_SERVICE_URL=http://core-api:8081

# Configurações de Email (Gmail SMTP)
EMAIL_FROM=seu-email@gmail.com
EMAIL_PASS=sua-senha-de-app
spring.mail.host=smtp.gmail.com
spring.mail.port=587
app.email.enabled=true
```

### Configurações de FFmpeg

O serviço utiliza configurações otimizadas para processamento de vídeo:

- **Qualidade JPEG**: 0.6 (balanceamento entre qualidade e tamanho)
- **Largura máxima**: 640px (redimensionamento automático)
- **Intervalo de frames**: 1 frame por segundo
- **Limite de frames**: 50 frames por vídeo (proteção contra overflow de memória)
- **Timeout FFmpeg**: 30 segundos por vídeo

## Endpoints de Integração

O serviço se integra com a **core-api** para atualização de status:

| Método | Endpoint | Descrição | Payload |
|--------|----------|-----------|---------|
| PUT | `/videos/{id}/status` | Atualizar status para PROCESSING | `{"status": "PROCESSING"}` |
| PUT | `/videos/{id}/status` | Atualizar status para SUCCESS | `{"status": "SUCCESS", "processedFile": "video.zip", "fileSize": "2.5 MB"}` |
| PUT | `/videos/{id}/status` | Atualizar status para ERROR | `{"status": "ERROR", "errorMessage": "Descrição do erro"}` |

## Estrutura de Mensagem SQS

```json
{
  "s3Key": "uploads/user123/video-uuid.mp4",
  "videoId": "d7e7f8a1-91c4-4b99-8e3f-123456789abc", 
  "clientId": "user123",
  "filename": "meu-video.mp4",
  "contentType": "video/mp4",
  "timestamp": 1719388800000
}
```

## Modelos de Dados

### VideoProcessRequest
```json
{
  "s3Key": "string",
  "videoId": "string", 
  "clientId": "string",
  "filename": "string",
  "contentType": "string",
  "timestamp": "number"
}
```

### Status de Processamento
- **PENDING** - Vídeo carregado, aguardando processamento
- **PROCESSING** - Vídeo sendo processado pelo serviço
- **SUCCESS** - Processamento concluído com sucesso
- **ERROR** - Erro durante o processamento

## Arquitetura

### Componentes do Sistema
- **video-api** - Orquestrador que recebe uploads e envia para SQS
- **core-api** - Interface para banco de dados PostgreSQL  
- **processor** - Este serviço, responsável pelo processamento
- **SQS** - Fila de mensagens para processamento assíncrono
- **S3** - Armazenamento (buckets de entrada e saída)
- **Email Service** - Notificações em caso de erro

### Deployment
O serviço roda como um Pod no cluster Kubernetes EKS, com:
- Service Account com permissões IAM para S3 e SQS
- ConfigMaps para configurações de ambiente
- Recursos otimizados para processamento de vídeo
- Health checks e auto-scaling configurados

## Executando o Projeto
```bash
# Build
mvn clean package

# Cobertura de teste
mvn verify

```

## Otimizações de Performance

### Processamento em Memória
- **Extração de frames**: Processa diretamente em memória sem arquivos temporários
- **Compactação ZIP**: Criação do arquivo ZIP em memória usando streams
- **Upload direto**: Upload dos bytes do ZIP diretamente para S3

### Configurações de Qualidade vs Tamanho
- **Compressão JPEG**: Qualidade 0.6 para reduzir tamanho mantendo qualidade visual
- **Redimensionamento**: Limitação automática a 640px de largura
- **Compressão ZIP**: Nível máximo de compressão (BEST_COMPRESSION)

### Proteções de Recursos
- **Limite de frames**: Máximo 50 frames por vídeo para evitar sobrecarga
- **Timeout FFmpeg**: 30 segundos para evitar travamentos
- **Pool de threads**: Processamento paralelo configurável via `sqs.threads`

## Cobertura de Testes

O projeto possui testes unitários para todos os componentes principais:

- **VideoProcessingWorkflowUseCaseTest** - Testa o fluxo completo de processamento
- **FrameExtractorServiceTest** - Testa extração de frames com FFmpeg
- **ZipServiceTest** - Testa compactação em memória
- **S3StorageServiceTest** - Testa operações de upload/download
- **EmailNotificationServiceTest** - Testa envio de notificações
- **SqsListenerServiceTest** - Testa escuta da fila SQS

Para verificar a cobertura:
```bash
mvn verify
# Relatório disponível em: target/site/jacoco/index.html
```
## Evidência de cobertura de teste
![alt text](image.png)