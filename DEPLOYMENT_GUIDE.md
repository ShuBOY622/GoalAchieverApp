# Goal App - Docker & AWS Deployment Guide

## Overview

This guide provides comprehensive instructions for containerizing and deploying the Goal App microservices architecture using Docker Compose locally and AWS EKS for production deployment with CI/CD using Jenkins.

## Architecture

The Goal App consists of:

### Backend Services (Spring Boot)
- **API Gateway** (Port 8080) - Routes requests to microservices
- **User Service** (Port 8081) - User management and authentication
- **Goal Service** (Port 8082) - Goal creation and management
- **Points Service** (Port 8083) - Points calculation and leaderboards
- **Notification Service** (Port 8084) - Notifications and alerts
- **Challenge Service** (Port 8085) - Friend challenges and competitions

### Frontend
- **React Application** (Port 3000) - User interface

### Infrastructure
- **MySQL Databases** - Separate database per service
- **Apache Kafka** - Event streaming and messaging
- **Zookeeper** - Kafka coordination

## Prerequisites

### Local Development
- Docker Desktop
- Docker Compose
- Git
- Java 17+ (for local development)
- Node.js 18+ (for local development)

### AWS Deployment
- AWS CLI configured
- kubectl installed
- eksctl installed
- Helm 3.x installed
- Jenkins server with required plugins

### Required AWS Services
- Amazon EKS (Kubernetes)
- Amazon RDS (MySQL databases)
- Amazon MSK (Managed Kafka)
- Amazon ECR (Container registry)
- Amazon VPC
- AWS IAM

## Local Development with Docker Compose

### 1. Clone and Setup

```bash
git clone <your-repo-url>
cd GoalApp
```

### 2. Build and Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Run in background
docker-compose up -d --build

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Remove volumes (clean slate)
docker-compose down -v
```

### 3. Service URLs (Local)

- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- User Service: http://localhost:8081
- Goal Service: http://localhost:8082
- Points Service: http://localhost:8083
- Notification Service: http://localhost:8084
- Challenge Service: http://localhost:8085

### 4. Database Access (Local)

Each service has its own MySQL database:
- User DB: localhost:3307
- Goal DB: localhost:3308
- Points DB: localhost:3309
- Notification DB: localhost:3310
- Challenge DB: localhost:3311

Default credentials: `goalapp` / `goalapp123`

### 5. Kafka Access (Local)

- Kafka: localhost:9092
- Zookeeper: localhost:2181

## AWS Production Deployment

### Phase 1: AWS Account Setup

#### 1. Create AWS Account
- Sign up for AWS account
- Set up billing alerts
- Create IAM user with appropriate permissions

#### 2. Configure AWS CLI
```bash
aws configure
# Enter your Access Key ID, Secret Access Key, Region (us-east-1), and output format (json)
```

#### 3. Create ECR Repositories
```bash
# The Jenkins pipeline will create these automatically, or create manually:
aws ecr create-repository --repository-name goalapp-api-gateway --region us-east-1
aws ecr create-repository --repository-name goalapp-user-service --region us-east-1
aws ecr create-repository --repository-name goalapp-goal-service --region us-east-1
aws ecr create-repository --repository-name goalapp-points-service --region us-east-1
aws ecr create-repository --repository-name goalapp-notification-service --region us-east-1
aws ecr create-repository --repository-name goalapp-challenge-service --region us-east-1
aws ecr create-repository --repository-name goalapp-frontend --region us-east-1
```

### Phase 2: Infrastructure Setup

#### 1. Deploy EKS Cluster
```bash
# Update the account ID in aws/eks-cluster.yaml
eksctl create cluster --config-file=aws/eks-cluster.yaml
```

#### 2. Deploy RDS Databases
```bash
# Get VPC and subnet information from EKS cluster
VPC_ID=$(aws eks describe-cluster --name goalapp-cluster --query 'cluster.resourcesVpcConfig.vpcId' --output text)
SUBNET_IDS=$(aws eks describe-cluster --name goalapp-cluster --query 'cluster.resourcesVpcConfig.subnetIds' --output text)

# Deploy RDS stack
aws cloudformation deploy \
  --template-file aws/rds-stack.yaml \
  --stack-name goalapp-rds-stack \
  --parameter-overrides \
    Environment=prod \
    VpcId=$VPC_ID \
    PrivateSubnetIds=$SUBNET_IDS \
    MasterPassword=YourSecurePassword123! \
  --capabilities CAPABILITY_IAM
```

#### 3. Deploy MSK (Kafka) Cluster
```bash
# Deploy MSK stack
aws cloudformation deploy \
  --template-file aws/msk-stack.yaml \
  --stack-name goalapp-msk-stack \
  --parameter-overrides \
    Environment=prod \
    VpcId=$VPC_ID \
    PrivateSubnetIds=$SUBNET_IDS \
  --capabilities CAPABILITY_IAM
```

### Phase 3: Jenkins CI/CD Setup

#### 1. Jenkins Server Setup

**Option A: AWS EC2**
```bash
# Launch EC2 instance (t3.medium recommended)
# Install Jenkins, Docker, AWS CLI, kubectl, helm

# Install Jenkins
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker jenkins

# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

**Option B: Jenkins on EKS**
```bash
# Deploy Jenkins using Helm
helm repo add jenkins https://charts.jenkins.io
helm repo update
helm install jenkins jenkins/jenkins --namespace jenkins --create-namespace
```

#### 2. Jenkins Configuration

1. **Install Required Plugins:**
   - Docker Pipeline
   - Kubernetes
   - AWS Steps
   - SonarQube Scanner
   - Slack Notification

2. **Configure Credentials:**
   - AWS Account ID
   - AWS Access Keys
   - Kubeconfig file
   - SonarQube token
   - Slack webhook

3. **Create Pipeline Jobs:**
   - Create multibranch pipeline for each service
   - Point to respective Jenkinsfile in each service directory

#### 3. Pipeline Execution

```bash
# Trigger builds through Jenkins UI or webhook
# Pipelines will:
# 1. Build and test code
# 2. Create Docker images
# 3. Push to ECR
# 4. Deploy to EKS
# 5. Run integration tests
```

### Phase 4: Kubernetes Deployment

#### 1. Create Namespaces
```bash
kubectl create namespace goalapp-dev
kubectl create namespace goalapp-prod
```

#### 2. Create Secrets
```bash
# Database secrets
kubectl create secret generic db-secrets \
  --from-literal=username=goalapp \
  --from-literal=password=YourSecurePassword123! \
  --namespace goalapp-prod

# Kafka secrets
kubectl create secret generic kafka-secrets \
  --from-literal=bootstrap-servers=<MSK_BOOTSTRAP_SERVERS> \
  --namespace goalapp-prod
```

#### 3. Deploy Services
The Jenkins pipelines will automatically deploy services using Helm charts.

### Phase 5: Monitoring and Observability

#### 1. Install Prometheus and Grafana
```bash
# Add Helm repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace

# Install Grafana
helm install grafana grafana/grafana \
  --namespace monitoring
```

#### 2. Configure Dashboards
- Import Spring Boot dashboards
- Create custom dashboards for business metrics
- Set up alerts for critical metrics

## Environment Variables

### Docker Compose Environment Variables

Create `.env` file in root directory:
```env
# Database
MYSQL_ROOT_PASSWORD=root
MYSQL_USER=goalapp
MYSQL_PASSWORD=goalapp123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Application
SPRING_PROFILES_ACTIVE=docker
```

### Kubernetes Environment Variables

Each service deployment will include:
```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "production"
- name: SPRING_DATASOURCE_URL
  value: "jdbc:mysql://rds-endpoint:3306/database_name"
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: db-secrets
      key: username
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-secrets
      key: password
- name: SPRING_KAFKA_BOOTSTRAP_SERVERS
  valueFrom:
    secretKeyRef:
      name: kafka-secrets
      key: bootstrap-servers
```

## Security Considerations

### 1. Network Security
- VPC with private subnets for databases and Kafka
- Security groups with minimal required access
- EKS cluster endpoint access control

### 2. Data Security
- RDS encryption at rest
- MSK encryption in transit and at rest
- Kubernetes secrets for sensitive data
- IAM roles with least privilege

### 3. Application Security
- Container image scanning with Trivy
- SonarQube code quality checks
- OWASP dependency checking
- Regular security updates

## Scaling and Performance

### 1. Horizontal Pod Autoscaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 2. Database Scaling
- RDS read replicas for read-heavy workloads
- Connection pooling optimization
- Database indexing and query optimization

### 3. Kafka Scaling
- MSK cluster scaling based on throughput
- Topic partitioning strategy
- Consumer group optimization

## Troubleshooting

### Common Issues

#### 1. Service Discovery Issues
```bash
# Check service endpoints
kubectl get endpoints -n goalapp-prod

# Check DNS resolution
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup user-service.goalapp-prod.svc.cluster.local
```

#### 2. Database Connection Issues
```bash
# Check RDS security groups
aws rds describe-db-instances --db-instance-identifier prod-goalapp-user-db

# Test connection from pod
kubectl exec -it <pod-name> -- mysql -h <rds-endpoint> -u goalapp -p
```

#### 3. Kafka Connection Issues
```bash
# Check MSK cluster status
aws kafka describe-cluster --cluster-arn <cluster-arn>

# Test Kafka connectivity
kubectl exec -it <pod-name> -- kafka-console-producer.sh --bootstrap-server <bootstrap-servers> --topic test-topic
```

### Monitoring and Logs

#### 1. Application Logs
```bash
# View service logs
kubectl logs -f deployment/user-service -n goalapp-prod

# View logs from all pods
kubectl logs -f -l app=user-service -n goalapp-prod
```

#### 2. Infrastructure Monitoring
- CloudWatch for AWS resources
- Prometheus metrics for Kubernetes
- Grafana dashboards for visualization

## Cost Optimization

### 1. Resource Right-sizing
- Monitor CPU and memory usage
- Adjust resource requests and limits
- Use spot instances for non-critical workloads

### 2. Storage Optimization
- Use gp3 volumes for better price/performance
- Implement lifecycle policies for logs
- Optimize Docker image sizes

### 3. Network Costs
- Use VPC endpoints for AWS services
- Optimize data transfer between AZs
- Implement caching strategies

## Backup and Disaster Recovery

### 1. Database Backups
- Automated RDS backups
- Cross-region backup replication
- Point-in-time recovery testing

### 2. Application Backups
- Kubernetes resource backups with Velero
- Container image backups in ECR
- Configuration backups in Git

### 3. Disaster Recovery Plan
- Multi-AZ deployment
- Cross-region replication
- Recovery time and point objectives

## Next Steps

1. **Set up monitoring and alerting**
2. **Implement automated testing**
3. **Configure backup strategies**
4. **Optimize performance and costs**
5. **Implement security best practices**
6. **Plan for disaster recovery**

## Support and Maintenance

### Regular Tasks
- Security updates
- Performance monitoring
- Cost optimization
- Backup verification
- Disaster recovery testing

### Useful Commands

```bash
# Docker Compose
docker-compose up -d --build
docker-compose logs -f
docker-compose down -v

# Kubernetes
kubectl get pods -n goalapp-prod
kubectl describe pod <pod-name> -n goalapp-prod
kubectl logs -f <pod-name> -n goalapp-prod

# AWS
aws eks update-kubeconfig --name goalapp-cluster
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

This deployment guide provides a complete roadmap for containerizing and deploying your Goal App microservices architecture. Follow the phases sequentially, and refer to the troubleshooting section for common issues.