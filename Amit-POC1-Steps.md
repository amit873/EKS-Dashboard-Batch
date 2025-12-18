# POC 1 - End-to-End Deployment Guide

## Prerequisites
- Java 17.0.17 installed
- Maven 3.9.11 installed  
- Docker Desktop 4.55.0 (213807) running
- AWS CLI 2.17.0 Installation
- AWS configured with **Access Keys**
  - AWS Access Key ID
  - AWS Secret Access Key
  - Default region: us-east-1
- eksctl 0.220.0 installed
- kubectl client v1.31.0 installed for EKS cluster
- EKS Cluster 1.32.9 Creation including Node Group
- ECR repository created: `dashboard-repo`

## Phase 1: Build Multi-Module Application

### 📁 POC-1 **Project Structure**
Dashbaord-Batch/
├── Dashboard/                    # Dashboard Module
│   ├── src/main/java/com/dashboard/
│   ├── src/main/resources/
│   ├── pom.xml
│   └── target/                   # Dashboard's target folder
│       └── dashboard-1.0.0.jar
├── Batch1/                      # Batch1 Module  
│   ├── src/main/java/com/batch1/
│   ├── pom.xml
│   └── target/                   # Batch1's target folder
│       └── batch1-1.0.0.jar
├── Batch2/                      # Batch2 Module
│   ├── src/main/java/com/batch2/
│   ├── pom.xml
│   └── target/                   # Batch2's target folder
│       └── batch2-1.0.0.jar
├── pom.xml                      # Parent POM
├── Dockerfile                   # Single image config
└── batch-config.properties      # Config-based routing

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
### Step 1: Build All Modules
cd C:\Users\amitk\Downloads\Dashbaord-Batch
mvn clean package -DskipTests
**Note:** Each module gets its own separate `target/` folder

### Step 2: Test Local JARs (Each from its own target folder)
# Test Batch1 (from Batch1/target/)
java -jar Batch1\target\batch1-1.0.0.jar

# Test Batch2 (from Batch2/target/)
java -jar Batch2\target\batch2-1.0.0.jar

# Test Dashboard (from Dashboard/target/) - Ctrl+C to stop
java -jar Dashboard\target\dashboard-1.0.0.jar

# Check dashboard (should be accessible at http://localhost:8080)

### 📝 **Target Folder Details**
# Each module has independent build artifacts:
Dashboard\target\classes\          # Compiled Dashboard classes
Dashboard\target\dashboard-1.0.0.jar

Batch1\target\classes\             # Compiled Batch1 classes  
Batch1\target\batch1-1.0.0.jar

Batch2\target\classes\             # Compiled Batch2 classes
Batch2\target\batch2-1.0.0.jar

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 2: Docker Image Creation
### 🐳 **Docker Structure**

Dashbaord-Batch/
├── Dockerfile                   # Multi-module image definition
├── advanced-entrypoint.sh       # Smart routing script
├── batch-config.properties      # Application mapping config
└── Docker Image Contents:
    ├── /app/entrypoint.sh       # → advanced-entrypoint.sh
    ├── /app/batch-config.properties
    ├── /app/dashboard.jar       # ← Dashboard/target/dashboard-1.0.0.jar
    ├── /app/batch1.jar         # ← Batch1/target/batch1-1.0.0.jar
    └── /app/batch2.jar         # ← Batch2/target/batch2-1.0.0.jar

### Step 3: Build Docker Image
docker build -t dashboard-batch-app:latest .

### Step 4: Test Docker Containers
# Test Batch1 via Docker
# Flow: entrypoint.sh → reads config → executes batch1.jar
docker run --rm dashboard-batch-app:latest batch1

# Test Batch2 via Docker  
# Flow: entrypoint.sh → reads config → executes batch2.jar
docker run --rm dashboard-batch-app:latest batch2

# Test Dashboard via Docker (background)
# Flow: entrypoint.sh → reads config → executes dashboard.jar
docker run -d -p 8080:8080 --name test-dashboard dashboard-batch-app:latest dashboard

# Check dashboard (should be accessible at http://localhost:8080)

# Cleanup test container
docker stop test-dashboard
docker rm test-dashboard

### 🔄 **How Single Image Handles Multiple Apps**
# Inside container when you run: docker run image batch1
# 1. entrypoint.sh receives argument "batch1"
# 2. Looks up in batch-config.properties: batch.batch1.jar=batch1.jar
# 3. Executes: java -jar batch1.jar
# 4. Batch1Application.main() runs and exits

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 3: ECR Push
### Step 5: Login to ECR
# Uses AWS Access Keys configured with 'aws configure'
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 058264125602.dkr.ecr.us-east-1.amazonaws.com

### Step 6: Tag and Push Image
docker tag dashboard-batch-app:latest 058264125602.dkr.ecr.us-east-1.amazonaws.com/dashboard-repo:latest
docker push 058264125602.dkr.ecr.us-east-1.amazonaws.com/dashboard-repo:latest

### Step 7: Verify ECR Push
aws ecr describe-images --repository-name dashboard-repo --region us-east-1

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 4: EKS Cluster Setup (If Not Exists)
### Step 8: Create EKS Cluster (Skip if exists)
eksctl create cluster --name dashboard-cluster --region us-east-1 --nodes 2 --node-type t3.medium --managed

### Step 9: Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name dashboard-cluster

## Check nodes and namespaces
kubectl get nodes
kubectl get ns

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 5: Clean EKS Environment
## Check Current Status
# Check jobs
kubectl get jobs

# Check deployment
kubectl get deployment
kubectl get deployment dashboard-app

# Check cronjobs
kubectl get cronjobs

# Check services
kubectl get svc
kubectl get service dashboard-service

### Step 10: Delete Old Resources
# Delete old jobs
kubectl delete jobs --all

# Delete old cronjobs
kubectl delete cronjobs --all

# Delete old deployment
kubectl delete deployment dashboard-app

# Delete old services
kubectl delete service dashboard-service

### Step 11: Apply RBAC Configuration
kubectl apply -f k8s/rbac.yaml

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 6: Deploy Application
### ⚙️ **EKS Configuration Structure:**
k8s/
├── rbac.yaml           # Kubernetes permissions
│   ├── ServiceAccount   # dashboard-service-account
│   ├── ClusterRole      # job-manager-role
│   └── ClusterRoleBinding # Binds account to role
├── deployment.yaml     # Dashboard web application
│   ├── Deployment       # dashboard-app (3 replicas)
│   └── Service         # dashboard-service (LoadBalancer)
└── cronjob.yaml        # Scheduled batch jobs
    ├── CronJob         # batch1-scheduler (hourly :00)
    └── CronJob         # batch2-scheduler (hourly :30)

### 📋 **Deployment Configuration:**
# deployment.yaml structure
### 🕰️ **CronJob Configuration:**
# cronjob.yaml structure

### Step 12: Deploy Dashboard Application
kubectl apply -f k8s/deployment.yaml

### Step 13: Wait for Deployment
kubectl rollout status deployment/dashboard-app --timeout=300s

### Step 14: Deploy CronJobs
kubectl apply -f k8s/cronjob.yaml

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 7: Verification
### Step 15: Check All Resources
kubectl get all

### Step 16: Check CronJobs
kubectl get cronjobs
kubectl describe cronjob batch1-scheduler
kubectl describe cronjob batch2-scheduler

### 🔍 **EKS Resource Relationships:**
EKS Cluster
├── dashboard-app (Deployment)
│   ├── Pod 1: dashboard.jar running
│   ├── Pod 2: dashboard.jar running  
│   └── Pod 3: dashboard.jar running
├── dashboard-service (LoadBalancer)
│   └── Routes traffic to dashboard pods
├── batch1-scheduler (CronJob)
│   └── Creates Jobs hourly → Pods run batch1.jar
└── batch2-scheduler (CronJob)
    └── Creates Jobs hourly → Pods run batch2.jar

### 🔄 **Same Image, Different Execution:**
ECR Image: 058264125602.dkr.ecr.us-east-1.amazonaws.com/dashboard-repo:latest
├── Dashboard Pods: No args → entrypoint.sh → dashboard.jar (web server)
├── Batch1 Jobs: args=["batch1"] → entrypoint.sh → batch1.jar (exits)
└── Batch2 Jobs: args=["batch2"] → entrypoint.sh → batch2.jar (exits)

### Step 17: Get Dashboard URL
kubectl get services dashboard-service
**Note the EXTERNAL-IP for dashboard access**

### Step 18: Check Pod Status
kubectl get pods
kubectl logs deployment/dashboard-app

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 8: Testing
### Step 19: Test Manual Batch Execution

# Open browser to dashboard URL (EXTERNAL-IP from step 17)
# Click "Run Batch 1" and "Run Batch 2" buttons

### Step 20: Verify Manual Jobs
kubectl get jobs
kubectl logs job/dashboard-batch-batch1-<timestamp>
kubectl logs job/dashboard-batch-batch2-<timestamp>

### Step 21: Monitor Automatic CronJobs
# Wait for next hour execution
kubectl get jobs -w

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
## Phase 9: Monitoring and Logs
### Step 22: Real-time Monitoring
# Monitor jobs
kubectl get jobs -w

# Monitor pods
kubectl get pods -w

# Follow dashboard logs
kubectl logs deployment/dashboard-app -f

### Step 23: Check System Health
kubectl top pods
kubectl top nodes
kubectl get events --sort-by=.metadata.creationTimestamp

## **@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@**
### ✅ Success Criteria:
- [ ] All 3 JARs built successfully
- [ ] Docker image contains all applications
- [ ] ECR push completed
- [ ] EKS deployment running
- [ ] Dashboard accessible via LoadBalancer
- [ ] Manual batch execution works
- [ ] CronJobs scheduled for hourly execution
- [ ] Batch logs show successful completion
- [ ] System handles concurrent jobs


