# Goal App - Microservices Architecture

A comprehensive goal-setting and tracking application built with microservices architecture, featuring friend challenges, points system, and real-time notifications.

## 🏗️ Architecture

### Backend Services (Spring Boot)
- **API Gateway** (8080) - Request routing and load balancing
- **User Service** (8081) - User management and authentication
- **Goal Service** (8082) - Goal creation and tracking
- **Points Service** (8083) - Points calculation and leaderboards
- **Notification Service** (8084) - Real-time notifications
- **Challenge Service** (8085) - Friend challenges and competitions

### Frontend
- **React Application** (3000) - Modern web interface with TypeScript

### Infrastructure
- **MySQL Databases** - Separate database per service
- **Apache Kafka** - Event streaming and inter-service communication
- **Zookeeper** - Kafka coordination

## 🚀 Quick Start

### Local Development (Docker Compose)

1. **Prerequisites**
   ```bash
   # Install Docker Desktop
   # Ensure Docker is running
   docker --version
   docker-compose --version
   ```

2. **Clone and Start**
   ```bash
   git clone <your-repo-url>
   cd GoalApp
   
   # Start all services
   ./scripts/deploy.sh local
   
   # Or manually
   docker-compose up --build -d
   ```

3. **Access Applications**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - Individual services: 8081-8085

4. **View Logs**
   ```bash
   docker-compose logs -f [service-name]
   ```

5. **Stop Services**
   ```bash
   docker-compose down -v
   ```

### AWS Production Deployment

1. **Prerequisites**
   ```bash
   # Install required tools
   aws --version
   kubectl version --client
   eksctl version
   helm version
   ```

2. **Configure AWS**
   ```bash
   aws configure
   # Enter your AWS credentials and region
   ```

3. **Deploy to AWS**
   ```bash
   # Full deployment (infrastructure + applications)
   ENVIRONMENT=prod ./scripts/deploy.sh aws
   
   # Or step by step
   ./scripts/deploy.sh infra    # Deploy EKS, RDS, MSK
   ./scripts/deploy.sh build    # Build and push images
   ./scripts/deploy.sh monitor  # Deploy monitoring
   ```

## 📁 Project Structure

```
GoalApp/
├── goal-app-backend/           # Backend microservices
│   ├── api-gateway/           # API Gateway service
│   ├── user-service/          # User management
│   ├── goal-service/          # Goal management
│   ├── points-service/        # Points and leaderboards
│   ├── notification-service/  # Notifications
│   ├── challenge-service/     # Friend challenges
│   └── common/               # Shared DTOs and utilities
├── goal-app-frontend/         # React frontend application
├── aws/                      # AWS CloudFormation templates
│   ├── eks-cluster.yaml      # EKS cluster configuration
│   ├── rds-stack.yaml        # RDS databases
│   └── msk-stack.yaml        # MSK Kafka cluster
├── config/                   # Configuration files
├── scripts/                  # Deployment scripts
├── docker-compose.yml        # Local development setup
├── Jenkinsfile              # CI/CD pipeline
└── DEPLOYMENT_GUIDE.md      # Detailed deployment guide
```

## 🔧 Configuration

### Environment Variables

Key environment variables (see `.env` file):

```bash
# Database
MYSQL_ROOT_PASSWORD=root
MYSQL_USER=goalapp
MYSQL_PASSWORD=goalapp123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Application
SPRING_PROFILES_ACTIVE=docker
REACT_APP_API_URL=http://localhost:8080
```

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React web application |
| API Gateway | 8080 | Main entry point |
| User Service | 8081 | User management |
| Goal Service | 8082 | Goal operations |
| Points Service | 8083 | Points and leaderboards |
| Notification Service | 8084 | Notifications |
| Challenge Service | 8085 | Friend challenges |

### Database Ports (Local)

| Database | Port | Service |
|----------|------|---------|
| User DB | 3307 | User Service |
| Goal DB | 3308 | Goal Service |
| Points DB | 3309 | Points Service |
| Notification DB | 3310 | Notification Service |
| Challenge DB | 3311 | Challenge Service |

## 🛠️ Development

### Building Individual Services

```bash
# Backend services
cd goal-app-backend
mvn clean package -DskipTests

# Frontend
cd goal-app-frontend
npm install
npm run build
```

### Running Tests

```bash
# Backend tests
cd goal-app-backend
mvn test

# Frontend tests
cd goal-app-frontend
npm test
```

### Database Access

Connect to local databases:
```bash
# Example: User Service database
mysql -h localhost -P 3307 -u goalapp -p goalapp_user
# Password: goalapp123
```

## 🔍 Monitoring

### Health Checks

- Service health: `http://localhost:808X/actuator/health`
- Frontend health: `http://localhost:3000/health`

### Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service

# Follow logs in real-time
docker-compose logs -f --tail=100 api-gateway
```

### Metrics

- Prometheus metrics: `http://localhost:808X/actuator/prometheus`
- Application info: `http://localhost:808X/actuator/info`

## 🚀 CI/CD Pipeline

### Jenkins Setup

1. **Install Jenkins** with required plugins:
   - Docker Pipeline
   - Kubernetes
   - AWS Steps
   - SonarQube Scanner

2. **Configure Credentials**:
   - AWS Account ID
   - AWS Access Keys
   - Kubeconfig file
   - SonarQube token

3. **Create Pipeline Jobs**:
   - Main pipeline: `Jenkinsfile`
   - Service pipelines: `*/Jenkinsfile`

### Pipeline Stages

1. **Build & Test** - Compile and test code
2. **Security Scan** - Vulnerability scanning
3. **Build Images** - Create Docker images
4. **Push to ECR** - Upload to AWS registry
5. **Deploy** - Deploy to Kubernetes
6. **Integration Tests** - End-to-end testing

## 🔒 Security

### Local Development
- Default credentials (change for production)
- HTTP communication
- Basic security configurations

### Production
- AWS IAM roles and policies
- VPC with private subnets
- RDS and MSK encryption
- Container image scanning
- Kubernetes secrets management

## 📊 Scaling

### Horizontal Scaling
- Kubernetes HPA for auto-scaling
- Load balancing across instances
- Database read replicas

### Performance Optimization
- Connection pooling
- Kafka partitioning
- Caching strategies
- Resource optimization

## 🆘 Troubleshooting

### Common Issues

1. **Services not starting**
   ```bash
   docker-compose logs [service-name]
   docker-compose restart [service-name]
   ```

2. **Database connection issues**
   ```bash
   # Check database status
   docker-compose ps
   # Restart databases
   docker-compose restart mysql-user mysql-goal
   ```

3. **Kafka connection issues**
   ```bash
   # Check Kafka status
   docker-compose logs kafka zookeeper
   # Restart Kafka stack
   docker-compose restart zookeeper kafka
   ```

4. **Port conflicts**
   ```bash
   # Check port usage
   netstat -tulpn | grep :8080
   # Stop conflicting services
   sudo lsof -ti:8080 | xargs kill -9
   ```

### Useful Commands

```bash
# Reset everything
docker-compose down -v
docker system prune -f

# Check service status
docker-compose ps

# Execute commands in containers
docker-compose exec user-service bash

# View container resource usage
docker stats
```

**Happy coding! 🚀**
