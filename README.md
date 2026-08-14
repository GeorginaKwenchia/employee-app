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
├── Jenkinsfile               # Jenkins CI/CD pipeline
└── .circleci/config.yml      # CircleCI CI/CD pipeline
```

## Teaching Modules

| Module | Backend | Database | Files |
|--------|---------|----------|-------|
| Build (EC2) | Python / Node.js / Java | PostgreSQL on EC2 | `BUILD.md`, `scripts/` |
| Docker | Node.js | Postgres container | `DOCKER.md`, `docker-compose.yml` |
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

## Option D — Deploy to EKS (Production)

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

Three pipelines — all run the same stages: **test → build & push to ECR → update `kubernetes/helm/values.yaml`**

| Pipeline | File |
|----------|------|
| GitHub Actions | `.github/workflows/deploy.yml` |
| Jenkins | `Jenkinsfile` |
| CircleCI | `.circleci/config.yml` |
