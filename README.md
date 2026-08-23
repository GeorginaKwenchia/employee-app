# Employee Directory App

A web application to manage employees — built on AWS EKS with full observability, automated CI/CD, and security at every layer.

## Repo Structure

```
employee-app/
├── backend/                  # Python (Flask) — used for Kubernetes / EKS module
├── backend-node/             # Node.js (Express) — used for Docker module
├── backend-java/             # Java (Spring Boot + Maven) — used for Build module
├── frontend/                 # Nginx + HTML/JS (shared by all backends)
├── scripts/                  # EC2 run scripts (run-python.sh, run-node.sh, run-java.sh)
├── kubernetes/
│   ├── manifests/            # Raw YAML — teach K8s objects one at a time
│   ├── helm/                 # Helm chart — used for Helm module
│   ├── argocd/               # ArgoCD apps — used for GitOps module
│   └── monitoring/           # Prometheus + Grafana manifests
├── terraform/                # AWS infrastructure (VPC, EKS, RDS, ECR, Secrets Manager)
├── docker-compose.yml        # Docker module — Node.js + Postgres container
├── docker-compose.python.yml # Kubernetes module prep — Python + Postgres container
├── BUILD.md                  # Build tutorial (Python, Node.js, Java, EC2)
├── DOCKER.md                 # Docker tutorial
├── JENKINS.md                # Jenkins tutorial (install, job types, Blue Ocean, pipeline)
├── Jenkinsfile               # Jenkins pipeline — test → build → push DockerHub → deploy EC2
└── .circleci/config.yml      # CircleCI CI/CD pipeline
```

## Teaching Modules

| Module | Backend | Database | Files |
|--------|---------|----------|-------|
| Build (EC2) | Python / Node.js / Java | PostgreSQL on EC2 | `BUILD.md`, `scripts/` |
| Docker | Node.js | Postgres container | `DOCKER.md`, `docker-compose.yml` |
| Jenkins CI/CD | Python | Postgres container | `JENKINS.md`, `Jenkinsfile` |
| GitHub Actions CI/CD | Python | Postgres container | `CICD.md`, `.github/workflows/` |
| Kubernetes | Python | RDS (Terraform) | `kubernetes/manifests/` |
| Helm | Python | RDS (Terraform) | `kubernetes/helm/` |
| ArgoCD | Python | RDS (Terraform) | `kubernetes/argocd/` |
| Monitoring | Python | RDS (Terraform) | `kubernetes/monitoring/` |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/employees` | List all employees |
| `POST` | `/api/employees` | Create employee (with optional photo) |
| `PUT` | `/api/employees/<id>` | Update employee |
| `DELETE` | `/api/employees/<id>` | Delete employee |
| `GET` | `/api/stats` | Total count, departments, latest hire |
| `GET` | `/api/health` | Liveness/readiness probe |
| `GET` | `/metrics` | Prometheus metrics |

---

## Option A — Run on EC2 (Amazon Linux 2023)

No Docker required — runs directly on the server.

### Recommended Instance

| Setting | Value |
|---------|-------|
| Instance type | `t3.medium` (2 vCPU, 4 GB RAM) |
| AMI | Amazon Linux 2023 |
| Storage | 20 GB gp3 |

### Security Group

| Port | Purpose |
|------|---------|
| 22 | SSH |
| 80 | Nginx |
| 5000 | Backend API |
| 8080 | Frontend (standalone) |

### Prerequisites

```bash
sudo dnf update -y
sudo dnf install -y git java-17-amazon-corretto-headless maven python3 python3-pip nginx

# Node.js 22
curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo dnf install -y nodejs

# PostgreSQL 15
sudo dnf install -y postgresql15-server postgresql15
sudo postgresql-setup --initdb
sudo systemctl enable --now postgresql
sudo sed -i 's/ident/md5/g' /var/lib/pgsql/data/pg_hba.conf
sudo systemctl restart postgresql
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE employees OWNER postgres;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE employees TO postgres;"

chmod o+x /home/ec2-user
```

### Clone and Run

```bash
git clone https://github.com/LandmakTechnology/employee-app.git
cd employee-app
chmod +x scripts/*.sh

bash scripts/run-python.sh   # Python
bash scripts/run-node.sh     # Node.js
bash scripts/run-java.sh     # Java
```

> See `BUILD.md` for the full step-by-step EC2 guide.

---

## Option B — Run with Docker (manual containers)

| Module | Backend image | Build from |
|--------|--------------|------------|
| Docker | `employee-backend-node:v1` | `./backend-node` |
| Kubernetes prep | `employee-backend:v1` | `./backend` |

```bash
# Build images
docker build -t employee-backend-node:v1 ./backend-node
docker build -t employee-backend:v1 ./backend
docker build -t employee-frontend:v1 ./frontend

# Start stack
docker network create employee-network

docker run -d --name db --restart unless-stopped \
  --network employee-network \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=employees \
  -p 5432:5432 postgres:15

until docker exec db pg_isready -U postgres; do sleep 2; done

docker run -d --name backend --restart unless-stopped \
  --network employee-network -p 5000:5000 \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  employee-backend-node:v1

docker run -d --name frontend --restart unless-stopped \
  --network employee-network -p 8080:80 \
  employee-frontend:v1
```

Access: `http://localhost:8080` or `http://<EC2_PUBLIC_IP>:8080`

```bash
docker rm -f frontend backend db
docker network rm employee-network
```

> See `DOCKER.md` for the full Docker guide.

---

## Option C — Run with Docker Compose

| File | Backend | Used when |
|------|---------|-----------|
| `docker-compose.yml` | Node.js | Docker module |
| `docker-compose.python.yml` | Python | Kubernetes module prep |

```bash
# Docker module
docker compose up --build

# Kubernetes module prep
docker compose -f docker-compose.python.yml up --build
```

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:8080` |
| Backend | `http://localhost:5000` |

```bash
docker compose down       # stop
docker compose down -v    # stop and wipe database
```

---

## Option D — CI/CD with Jenkins (DockerHub + EC2 Deploy)

Jenkins runs on its own EC2 instance. Every push to `main` triggers the pipeline: test → build Docker images → push to DockerHub → SSH deploy to the app EC2.

### Jenkins EC2 Requirements

| Setting | Value |
|---------|-------|
| AMI | Amazon Linux 2023 |
| Instance type | `t3.medium` |
| Security group | 22 (SSH), 8080 (Jenkins UI) |

### Pipeline Stages

| Stage | What it does |
|-------|--------------|
| Test | `pip install` + `pytest` against SQLite |
| Build & Push | `docker build` backend + frontend, push to `chafah/employee-backend` and `chafah/employee-frontend` on DockerHub |
| Deploy | SSH into app EC2, `docker pull`, remove old containers, `docker run` backend on `:5000` and frontend on `:80` |

### Jenkins Job Types

Three job types are covered in `JENKINS.md`:

| Job Type | How it's configured | Best for |
|----------|--------------------|---------|
| Freestyle | Jenkins UI only | Simple tasks, demos, intro to Jenkins |
| Pipeline | `Jenkinsfile` in repo | Single-branch CI/CD |
| Multibranch Pipeline | `Jenkinsfile` per branch | Full team workflow, PR builds |

### Credentials Required in Jenkins

| ID | Kind | Value |
|----|------|-------|
| `dockerhub-credentials` | Username/password | DockerHub username + access token |
| `ec2-ssh-key` | SSH private key | `.pem` key for the app EC2 |
| `ec2-host` | Secret text | App EC2 public IP |
| `database-url` | Secret text | `postgresql://postgres:postgres@db:5432/employees` |

### DockerHub Repositories

| Image | DockerHub repo |
|-------|----------------|
| Backend | `chafah/employee-backend` |
| Frontend | `chafah/employee-frontend` |

### Blue Ocean

Blue Ocean gives Jenkins a modern visual pipeline UI. After installing the Blue Ocean plugin, access it at:

```
http://<JENKINS_EC2_PUBLIC_IP>:8080/blue
```

> See `JENKINS.md` for the full step-by-step Jenkins guide.

---

## Option E — CI/CD with GitHub Actions (EC2 Deploy)

This is the CI/CD module. Three workflow files handle automated testing and deployment.

### Branching Strategy

```
feature/* / fix/* / chore/*  ──▶  test only (no deploy)
develop                       ──▶  test → build → push to ECR → deploy to DEV EC2
main                          ──▶  test → build → push to ECR → deploy to PROD EC2 (approval required)
```

### Workflow Files

| File | Trigger | What it does |
|------|---------|-------------|
| `.github/workflows/test.yml` | Push to any branch | Runs pytest only |
| `.github/workflows/deploy-dev.yml` | `test.yml` passes on `develop` | Build → ECR → deploy to dev EC2 |
| `.github/workflows/deploy-prod.yml` | `test.yml` passes on `main` | Build → ECR → deploy to prod EC2 |

### GitHub Secrets and Variables Required

**Repository secrets:**

| Name | Value |
|------|-------|
| `AWS_ACCESS_KEY_ID` | IAM user access key |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |

**Repository variables:**

| Name | Value |
|------|-------|
| `AWS_REGION` | `us-east-1` |
| `ECR_REGISTRY` | `075120018043.dkr.ecr.us-east-1.amazonaws.com` |

**`development` environment secrets:**

| Name | Value |
|------|-------|
| `EC2_HOST` | Dev EC2 public IP |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | Private key (PEM contents) |

**`production` environment secrets:**

| Name | Value |
|------|-------|
| `EC2_HOST` | Prod EC2 public IP |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | Private key (PEM contents) |

### ECR Repositories

Repositories already created in `us-east-1`:

| Repository | URI |
|------------|-----|
| `employee-backend` | `075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend` |
| `employee-frontend` | `075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-frontend` |

### EC2 Prerequisites (run once per server)

The pipeline deploys via SSH. The EC2 instance needs Docker, AWS CLI, and an IAM role with ECR pull permissions.

**1. Attach IAM role to EC2:**
```
IAM → Roles → Create role → EC2 → AmazonEC2ContainerRegistryReadOnly
EC2 → Instance → Actions → Security → Modify IAM role → attach role
```

**2. Install Docker and AWS CLI on the EC2 instance:**
```bash
sudo dnf install -y docker awscli
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
newgrp docker
```

**3. Add your SSH public key to the EC2 instance:**
```bash
# Generate a key pair for GitHub Actions
ssh-keygen -t rsa -b 4096 -f github-actions-key -N ""

# On the EC2 instance
cat github-actions-key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Then add the private key contents to the GitHub environment secret `EC2_SSH_KEY`.

### What the deploy script does on each run

```
1. Authenticate Docker with ECR (using EC2 IAM role)
2. Create employee-network if it does not exist
3. Start postgres:15 container if db is not already running
4. Wait for postgres to be ready
5. Pull new backend and frontend images from ECR
6. Remove old backend and frontend containers
7. Start new containers on employee-network
8. Remove old images to free disk space
```

### Image Tagging

| Branch | Tag format | Example |
|--------|-----------|--------|
| `develop` | `be-dev-YYYYMMDD-HHMMSS` | `be-dev-20260818-120000` |
| `main` | `be-prod-YYYYMMDD-HHMMSS` | `be-prod-20260818-130000` |

### Trigger a manual deploy

```
GitHub → Actions → Deploy to Production → Run workflow → Run workflow
```

### Set up production approval gate

```
GitHub → Settings → Environments → production → Required reviewers → add reviewer
```

The deploy job will pause and wait for approval before running on production.

> See `CICD.md` for the full CI/CD lecture notes.

---

## Option F — Deploy to EKS (Production)

### 1. Deploy Infrastructure

```bash
cd terraform
terraform init
terraform apply -var-file=env/dev/terraform.tfvars
```

### 2. Connect to EKS

```bash
aws eks update-kubeconfig --name landmark-cluster-dev --region us-east-1 --profile terraform
```

### 3. Install Cluster Add-ons

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace --wait

helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=landmark-cluster-dev \
  --set serviceAccount.create=true \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=$(terraform output -raw lb_controller_role_arn)
```

### 4. Deploy the App

```bash
helm install employee-app kubernetes/helm/ \
  --namespace employee-app --create-namespace
```

### 5. Verify

```bash
kubectl get pods -n employee-app
kubectl get ingress -n employee-app
```

### Access URLs

| Service | Command |
|---------|---------|
| App (ALB) | `kubectl get ingress -n employee-app` → ADDRESS column |
| Grafana (NLB) | `kubectl get svc grafana -n monitoring` → EXTERNAL-IP |

Grafana login: `admin` / `admin123`

---

## CI/CD

| Pipeline | Registry | Deploy target | Files |
|----------|----------|---------------|-------|
| GitHub Actions | ECR | EC2 (dev + prod) | `.github/workflows/` |
| Jenkins | DockerHub | EC2 | `Jenkinsfile`, `JENKINS.md` |
| CircleCI | ECR | EC2 | `.circleci/config.yml` |
