#!/bin/bash

# Goal App Deployment Script
# This script helps automate the deployment process

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION=${AWS_REGION:-us-east-1}
ENVIRONMENT=${ENVIRONMENT:-dev}
CLUSTER_NAME="goalapp-cluster"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if required tools are installed
    command -v docker >/dev/null 2>&1 || { log_error "Docker is required but not installed. Aborting."; exit 1; }
    command -v aws >/dev/null 2>&1 || { log_error "AWS CLI is required but not installed. Aborting."; exit 1; }
    command -v kubectl >/dev/null 2>&1 || { log_error "kubectl is required but not installed. Aborting."; exit 1; }
    command -v helm >/dev/null 2>&1 || { log_error "Helm is required but not installed. Aborting."; exit 1; }
    command -v eksctl >/dev/null 2>&1 || { log_error "eksctl is required but not installed. Aborting."; exit 1; }
    
    # Check AWS credentials
    aws sts get-caller-identity >/dev/null 2>&1 || { log_error "AWS credentials not configured. Run 'aws configure' first."; exit 1; }
    
    log_success "All prerequisites met!"
}

create_ecr_repositories() {
    log_info "Creating ECR repositories..."
    
    local services=("goalapp-api-gateway" "goalapp-user-service" "goalapp-goal-service" 
                   "goalapp-points-service" "goalapp-notification-service" 
                   "goalapp-challenge-service" "goalapp-frontend")
    
    for service in "${services[@]}"; do
        if aws ecr describe-repositories --repository-names "$service" --region "$AWS_REGION" >/dev/null 2>&1; then
            log_warning "ECR repository $service already exists"
        else
            aws ecr create-repository --repository-name "$service" --region "$AWS_REGION" >/dev/null
            log_success "Created ECR repository: $service"
        fi
    done
}

deploy_infrastructure() {
    log_info "Deploying infrastructure..."
    
    # Deploy EKS cluster
    if eksctl get cluster --name "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
        log_warning "EKS cluster $CLUSTER_NAME already exists"
    else
        log_info "Creating EKS cluster (this may take 15-20 minutes)..."
        eksctl create cluster --config-file=aws/eks-cluster.yaml
        log_success "EKS cluster created successfully"
    fi
    
    # Update kubeconfig
    aws eks update-kubeconfig --name "$CLUSTER_NAME" --region "$AWS_REGION"
    
    # Get VPC and subnet information
    local vpc_id=$(aws eks describe-cluster --name "$CLUSTER_NAME" --query 'cluster.resourcesVpcConfig.vpcId' --output text)
    local subnet_ids=$(aws eks describe-cluster --name "$CLUSTER_NAME" --query 'cluster.resourcesVpcConfig.subnetIds' --output text | tr '\t' ',')
    
    # Deploy RDS stack
    log_info "Deploying RDS databases..."
    aws cloudformation deploy \
        --template-file aws/rds-stack.yaml \
        --stack-name "goalapp-rds-stack-$ENVIRONMENT" \
        --parameter-overrides \
            Environment="$ENVIRONMENT" \
            VpcId="$vpc_id" \
            PrivateSubnetIds="$subnet_ids" \
            MasterPassword="GoalApp123!" \
        --capabilities CAPABILITY_IAM
    log_success "RDS stack deployed successfully"
    
    # Deploy MSK stack
    log_info "Deploying MSK (Kafka) cluster..."
    aws cloudformation deploy \
        --template-file aws/msk-stack.yaml \
        --stack-name "goalapp-msk-stack-$ENVIRONMENT" \
        --parameter-overrides \
            Environment="$ENVIRONMENT" \
            VpcId="$vpc_id" \
            PrivateSubnetIds="$subnet_ids" \
        --capabilities CAPABILITY_IAM
    log_success "MSK stack deployed successfully"
}

setup_kubernetes() {
    log_info "Setting up Kubernetes resources..."
    
    # Create namespaces
    kubectl create namespace "goalapp-$ENVIRONMENT" --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    # Create secrets
    kubectl create secret generic db-secrets \
        --from-literal=username=goalapp \
        --from-literal=password=GoalApp123! \
        --namespace "goalapp-$ENVIRONMENT" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    # Get MSK bootstrap servers
    local msk_arn=$(aws cloudformation describe-stacks \
        --stack-name "goalapp-msk-stack-$ENVIRONMENT" \
        --query 'Stacks[0].Outputs[?OutputKey==`MSKClusterArn`].OutputValue' \
        --output text)
    
    local bootstrap_servers=$(aws kafka describe-cluster \
        --cluster-arn "$msk_arn" \
        --query 'ClusterInfo.BrokerNodeGroupInfo.BrokerAZDistribution' \
        --output text 2>/dev/null || echo "kafka:9092")
    
    kubectl create secret generic kafka-secrets \
        --from-literal=bootstrap-servers="$bootstrap_servers" \
        --namespace "goalapp-$ENVIRONMENT" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    log_success "Kubernetes resources created successfully"
}

build_and_push_images() {
    log_info "Building and pushing Docker images..."
    
    local account_id=$(aws sts get-caller-identity --query Account --output text)
    local registry="$account_id.dkr.ecr.$AWS_REGION.amazonaws.com"
    
    # Login to ECR
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$registry"
    
    # Build and push backend services
    local services=("api-gateway" "user-service" "goal-service" "points-service" "notification-service" "challenge-service")
    
    for service in "${services[@]}"; do
        log_info "Building $service..."
        docker build -t "goalapp-$service:latest" "goal-app-backend/$service/"
        docker tag "goalapp-$service:latest" "$registry/goalapp-$service:latest"
        docker push "$registry/goalapp-$service:latest"
        log_success "Pushed goalapp-$service:latest"
    done
    
    # Build and push frontend
    log_info "Building frontend..."
    docker build -t "goalapp-frontend:latest" "goal-app-frontend/"
    docker tag "goalapp-frontend:latest" "$registry/goalapp-frontend:latest"
    docker push "$registry/goalapp-frontend:latest"
    log_success "Pushed goalapp-frontend:latest"
}

deploy_monitoring() {
    log_info "Deploying monitoring stack..."
    
    # Add Helm repositories
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo update
    
    # Install Prometheus
    helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
        --namespace monitoring \
        --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
        --set prometheus.prometheusSpec.podMonitorSelectorNilUsesHelmValues=false
    
    log_success "Monitoring stack deployed successfully"
}

local_deployment() {
    log_info "Starting local deployment with Docker Compose..."
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker Desktop."
        exit 1
    fi
    
    # Build and start services
    docker-compose down -v 2>/dev/null || true
    docker-compose up --build -d
    
    log_success "Local deployment started successfully!"
    log_info "Services are available at:"
    echo "  - Frontend: http://localhost:3000"
    echo "  - API Gateway: http://localhost:8080"
    echo "  - User Service: http://localhost:8081"
    echo "  - Goal Service: http://localhost:8082"
    echo "  - Points Service: http://localhost:8083"
    echo "  - Notification Service: http://localhost:8084"
    echo "  - Challenge Service: http://localhost:8085"
}

aws_deployment() {
    log_info "Starting AWS deployment..."
    
    check_prerequisites
    create_ecr_repositories
    deploy_infrastructure
    setup_kubernetes
    build_and_push_images
    deploy_monitoring
    
    log_success "AWS deployment completed successfully!"
    log_info "Next steps:"
    echo "  1. Set up Jenkins CI/CD pipelines"
    echo "  2. Configure monitoring dashboards"
    echo "  3. Set up backup and disaster recovery"
    echo "  4. Configure DNS and SSL certificates"
}

show_help() {
    echo "Goal App Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  local     Deploy locally using Docker Compose"
    echo "  aws       Deploy to AWS using EKS"
    echo "  build     Build and push Docker images to ECR"
    echo "  infra     Deploy only infrastructure (EKS, RDS, MSK)"
    echo "  monitor   Deploy monitoring stack"
    echo "  help      Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  AWS_REGION    AWS region (default: us-east-1)"
    echo "  ENVIRONMENT   Environment name (default: dev)"
    echo ""
    echo "Examples:"
    echo "  $0 local"
    echo "  ENVIRONMENT=prod $0 aws"
    echo "  AWS_REGION=us-west-2 $0 infra"
}

# Main script logic
case "${1:-help}" in
    local)
        local_deployment
        ;;
    aws)
        aws_deployment
        ;;
    build)
        check_prerequisites
        create_ecr_repositories
        build_and_push_images
        ;;
    infra)
        check_prerequisites
        deploy_infrastructure
        ;;
    monitor)
        check_prerequisites
        deploy_monitoring
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac