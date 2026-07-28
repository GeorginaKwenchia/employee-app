# Employee Directory App

A web application to manage employees — stores data in PostgreSQL and profile photos in S3. Runs in Kubernetes (EKS) or directly on EC2.

## Architecture

```
Browser → LoadBalancer → Frontend (Nginx) → Backend (Flask) → RDS PostgreSQL
                                                            → S3 (photos)
```

## Backends

The same REST API is implemented in three languages. All expose identical endpoints on port `5000`.

| Backend | Language | Directory |
|---------|----------|-----------|
| Python | Flask | `backend/` |
| Node.js | Express | `backend-node/` |
| Java | Spring Boot | `backend-java/` |

## Structure

```
employee-app/
├── backend/           # Python (Flask)
├── backend-node/      # Node.js (Express)
├── backend-java/      # Java (Spring Boot + Maven)
├── frontend/          # Nginx + HTML/JS
├── BUILD.md           # Build tutorial (Python, Node.js, Java, EC2)
└── docker-compose.yml # Full local stack
```

---

## Option A — Run on EC2 (Amazon Linux 2023)

### Recommended Instance

| Setting | Value |
|---------|-------|
| Instance type | `t3.medium` (2 vCPU, 4 GB RAM) |
| AMI | Amazon Linux 2023 |
| Storage | 20 GB gp3 |
| Region | `us-east-1` |

### Security Group

| Port | Purpose |
|------|---------|
| 22 | SSH |
| 80 | Nginx (full stack) |
| 5000 | Backend API |
| 8080 | Frontend (standalone) |

### Connect

```bash
ssh -i <YOUR_KEY.pem> ec2-user@<EC2_PUBLIC_IP>
```

### Install Dependencies

```bash
sudo dnf update -y
sudo dnf install -y git java-17-amazon-corretto-headless maven python3 python3-pip nginx

# Node.js 18
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install -y nodejs

# PostgreSQL 15
sudo dnf install -y postgresql15-server postgresql15
sudo postgresql-setup --initdb
sudo systemctl enable --now postgresql
sudo -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE employees OWNER postgres;"
sudo sed -i 's/ident/md5/g' /var/lib/pgsql/data/pg_hba.conf
sudo systemctl restart postgresql
```

### Clone and Run

```bash
git clone https://github.com/LandmakTechnology/employee-app.git
cd employee-app/employee-app
```

**Python:**
```bash
cd backend && pip3 install -r requirements.txt
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees python3 app.py
```

**Node.js:**
```bash
cd backend-node && npm install
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees npm start
```

**Java:**
```bash
cd backend-java && mvn package -DskipTests
DATABASE_URL=jdbc:postgresql://localhost:5432/employees DB_USER=postgres DB_PASS=postgres java -jar target/employee-backend-1.0.0.jar
```

### Access

| URL | Description |
|-----|-------------|
| `http://<EC2_PUBLIC_IP>:5000/api/health` | Health check |
| `http://<EC2_PUBLIC_IP>:5000/api/employees` | Employee list |
| `http://<EC2_PUBLIC_IP>:8080` | Frontend (Python HTTP server) |
| `http://<EC2_PUBLIC_IP>` | Full stack via Nginx (port 80) |

> See `BUILD.md` for the full step-by-step EC2 guide including Nginx reverse proxy setup.

---

## Option B — Run Locally with Docker Compose

```bash
cd employee-app
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:8080` |
| Backend | `http://localhost:5000` |

---

## Option C — Deploy to EKS (Production)

### 1. Deploy Infrastructure
```bash
cd ../terraform
terraform init
terraform apply -var-file=env/dev/terraform.tfvars
```

### 2. Build & Push Docker Images
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 075120018043.dkr.ecr.us-east-1.amazonaws.com

cd backend/
docker build -t employee-backend .
docker tag employee-backend:latest 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend:latest
docker push 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend:latest

cd ../frontend/
docker build -t employee-frontend .
docker tag employee-frontend:latest 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-frontend:latest
docker push 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-frontend:latest
```

### 3. Deploy with Helm
```bash
helm install employee-app helm/employee-app \
  --namespace employee-app \
  --create-namespace \
  --set database.host=<RDS_ENDPOINT> \
  --set database.password=<DB_PASSWORD> \
  --set serviceAccount.roleArn=<S3_ACCESS_ROLE_ARN>
```

### 4. Access
```bash
kubectl get svc -n employee-app  # Get LoadBalancer URL
```

### Upgrade
```bash
helm upgrade employee-app helm/employee-app \
  --namespace employee-app \
  --set database.host=<RDS_ENDPOINT> \
  --set database.password=<DB_PASSWORD> \
  --set serviceAccount.roleArn=<S3_ACCESS_ROLE_ARN>
```

### Uninstall
```bash
helm uninstall employee-app -n employee-app
```
