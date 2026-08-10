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

No Docker required — the app runs directly on the server.

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

# Allow Nginx to read frontend files
chmod o+x /home/ec2-user
```

### Clone and Run

```bash
git clone https://github.com/LandmakTechnology/employee-app.git
cd employee-app/employee-app
chmod +x scripts/*.sh
```

Run one backend at a time — each script builds, tests, starts the app, and prints the URL:

```bash
bash scripts/run-python.sh   # Python
bash scripts/run-node.sh     # Node.js
bash scripts/run-java.sh     # Java
```

Clean up between practicals:

```bash
kill <PID>   # PID printed by the script
sudo rm -f /etc/nginx/conf.d/employee-app.conf
sudo systemctl reload nginx
```

### Access

| URL | Description |
|-----|-------------|
| `http://<EC2_PUBLIC_IP>` | Full stack via Nginx |
| `http://<EC2_PUBLIC_IP>:5000/api/health` | Health check |

> See `BUILD.md` for the full step-by-step EC2 guide.

---

## Option B — Run with Docker (manual containers)

Build the images first:

```bash
cd employee-app
docker build -t employee-backend:v1 ./backend
docker build -t employee-frontend:v1 ./frontend
```

Then start the stack in order:

```bash
# 1. Create a shared network
docker network create employee-network

# 2. Start PostgreSQL
docker run -d \
  --name db \
  --network employee-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  -p 5432:5432 \
  postgres:15

# 3. Wait for Postgres to be ready before starting the backend
until docker exec db pg_isready -U postgres; do
  echo "Waiting for database..."
  sleep 2
done

# 4. Start the backend
docker run -d \
  --name backend \
  --network employee-network \
  -p 5000:5000 \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  employee-backend:v1

# 5. Start the frontend
docker run -d \
  --name frontend \
  --network employee-network \
  -p 8080:80 \
  employee-frontend:v1
```

### Access

| URL | Description |
|-----|-------------|
| `http://localhost:8080` | Full web interface |
| `http://localhost:5000/api/health` | Backend health check |

If running on EC2, replace `localhost` with the public IP and ensure ports `8080` and `5000` are open in the security group:

```
http://<EC2_PUBLIC_IP>:8080
```

Or use SSH port forwarding to access without opening ports (run on your local machine):

```bash
ssh -i your-key.pem \
  -L 8080:localhost:8080 \
  -L 5000:localhost:5000 \
  ec2-user@<EC2_PUBLIC_IP>
```

Then open `http://localhost:8080` in your browser.

### Clean up

```bash
docker rm -f frontend backend db
docker network rm employee-network
```

---

## Option C — Run with Docker Compose

Docker Compose starts all three services (Postgres, backend, frontend) in the correct order automatically:

```bash
cd employee-app
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:8080` |
| Backend | `http://localhost:5000` |

Stop:

```bash
docker compose down       # stop
docker compose down -v    # stop and wipe database
```

> See `DOCKER.md` for the full Docker guide including networking, volumes, and image registries.

---

## Option D — Deploy to EKS (Production)

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
