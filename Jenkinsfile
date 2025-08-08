pipeline {
    agent any
    
    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = credentials('aws-account-id')
        KUBECONFIG = credentials('kubeconfig')
    }
    
    parameters {
        choice(
            name: 'DEPLOYMENT_TYPE',
            choices: ['all', 'backend-only', 'frontend-only', 'infrastructure-only'],
            description: 'Select what to deploy'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip running tests'
        )
        booleanParam(
            name: 'FORCE_REBUILD',
            defaultValue: false,
            description: 'Force rebuild all images'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                }
            }
        }
        
        stage('Infrastructure Setup') {
            when {
                anyOf {
                    expression { params.DEPLOYMENT_TYPE == 'all' }
                    expression { params.DEPLOYMENT_TYPE == 'infrastructure-only' }
                }
            }
            parallel {
                stage('Create ECR Repositories') {
                    steps {
                        script {
                            def services = [
                                'goalapp-api-gateway',
                                'goalapp-user-service',
                                'goalapp-goal-service',
                                'goalapp-points-service',
                                'goalapp-notification-service',
                                'goalapp-challenge-service',
                                'goalapp-frontend'
                            ]
                            
                            services.each { service ->
                                sh """
                                    aws ecr describe-repositories --repository-names ${service} --region ${AWS_REGION} || \
                                    aws ecr create-repository --repository-name ${service} --region ${AWS_REGION}
                                """
                            }
                        }
                    }
                }
                
                stage('Setup EKS Cluster') {
                    steps {
                        sh '''
                            # Check if cluster exists
                            if ! aws eks describe-cluster --name goalapp-cluster --region ${AWS_REGION} > /dev/null 2>&1; then
                                echo "Creating EKS cluster..."
                                eksctl create cluster --config-file=./aws/eks-cluster.yaml
                            else
                                echo "EKS cluster already exists"
                            fi
                        '''
                    }
                }
                
                stage('Setup RDS Instances') {
                    steps {
                        sh '''
                            # Apply RDS CloudFormation template
                            aws cloudformation deploy \
                                --template-file ./aws/rds-stack.yaml \
                                --stack-name goalapp-rds-stack \
                                --parameter-overrides \
                                    Environment=${BRANCH_NAME == 'main' ? 'prod' : 'dev'} \
                                --capabilities CAPABILITY_IAM \
                                --region ${AWS_REGION}
                        '''
                    }
                }
                
                stage('Setup MSK (Kafka)') {
                    steps {
                        sh '''
                            # Apply MSK CloudFormation template
                            aws cloudformation deploy \
                                --template-file ./aws/msk-stack.yaml \
                                --stack-name goalapp-msk-stack \
                                --parameter-overrides \
                                    Environment=${BRANCH_NAME == 'main' ? 'prod' : 'dev'} \
                                --capabilities CAPABILITY_IAM \
                                --region ${AWS_REGION}
                        '''
                    }
                }
            }
        }
        
        stage('Build Backend Services') {
            when {
                anyOf {
                    expression { params.DEPLOYMENT_TYPE == 'all' }
                    expression { params.DEPLOYMENT_TYPE == 'backend-only' }
                }
            }
            parallel {
                stage('API Gateway') {
                    steps {
                        build job: 'goalapp-api-gateway', 
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
                stage('User Service') {
                    steps {
                        build job: 'goalapp-user-service',
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
                stage('Goal Service') {
                    steps {
                        build job: 'goalapp-goal-service',
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
                stage('Points Service') {
                    steps {
                        build job: 'goalapp-points-service',
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
                stage('Notification Service') {
                    steps {
                        build job: 'goalapp-notification-service',
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
                stage('Challenge Service') {
                    steps {
                        build job: 'goalapp-challenge-service',
                              parameters: [
                                  string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                                  booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                              ]
                    }
                }
            }
        }
        
        stage('Build Frontend') {
            when {
                anyOf {
                    expression { params.DEPLOYMENT_TYPE == 'all' }
                    expression { params.DEPLOYMENT_TYPE == 'frontend-only' }
                }
            }
            steps {
                build job: 'goalapp-frontend',
                      parameters: [
                          string(name: 'BRANCH_NAME', value: env.BRANCH_NAME),
                          booleanParam(name: 'SKIP_TESTS', value: params.SKIP_TESTS)
                      ]
            }
        }
        
        stage('Integration Tests') {
            when {
                not { params.SKIP_TESTS }
            }
            steps {
                script {
                    def namespace = env.BRANCH_NAME == 'main' ? 'goalapp-prod' : 'goalapp-dev'
                    sh """
                        # Wait for all services to be ready
                        kubectl wait --for=condition=ready pod -l app=api-gateway -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=user-service -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=goal-service -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=points-service -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=notification-service -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=challenge-service -n ${namespace} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=frontend -n ${namespace} --timeout=300s
                        
                        # Run integration tests
                        ./scripts/run-integration-tests.sh ${namespace}
                    """
                }
            }
        }
        
        stage('Performance Tests') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    # Run performance tests using k6
                    docker run --rm -v $(pwd)/tests/performance:/scripts \
                        grafana/k6 run /scripts/load-test.js
                '''
            }
        }
        
        stage('Security Scan') {
            steps {
                parallel {
                    stage('OWASP Dependency Check') {
                        steps {
                            sh '''
                                docker run --rm -v $(pwd):/src \
                                    owasp/dependency-check:latest \
                                    --scan /src --format JSON --out /src/dependency-check-report.json
                            '''
                        }
                    }
                    stage('Kubernetes Security Scan') {
                        steps {
                            sh '''
                                # Scan Kubernetes manifests with kube-score
                                kube-score score ./k8s/*.yaml > kube-score-report.txt
                            '''
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: '*.json,*.txt',
                reportName: 'Security and Performance Reports'
            ])
        }
        success {
            slackSend(
                channel: '#deployments',
                color: 'good',
                message: "✅ Goal App deployment successful - Build: ${BUILD_NUMBER}, Branch: ${BRANCH_NAME}, Type: ${params.DEPLOYMENT_TYPE}"
            )
        }
        failure {
            slackSend(
                channel: '#deployments',
                color: 'danger',
                message: "❌ Goal App deployment failed - Build: ${BUILD_NUMBER}, Branch: ${BRANCH_NAME}, Type: ${params.DEPLOYMENT_TYPE}"
            )
        }
    }
}