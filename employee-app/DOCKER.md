# Docker — From Build to Containers

---

## Where We Are in the SDLC

```
┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   ┌──────────────┐   ┌────────┐   ┌─────────┐
│  PLAN  │──▶│  CODE  │──▶│ BUILD  │──▶│  TEST  │──▶│   RELEASE    │──▶│ DEPLOY │──▶│ MONITOR │
│        │   │        │   │        │   │        │   │   (Docker)   │   │  (K8s) │   │Grafana  │
└────────┘   └────────┘   └────────┘   └────────┘   └──────────────┘   └────────┘   └─────────┘
                                                              ↑
                                                        You are here
```

In the **Build** phase we:
- Identified the programming language from the dependency file
- Installed dependencies (`pip install`, `npm install`, `mvn package`)
- Ran the application directly on our machine
- Ran automated tests

The problem we are left with:

> The app runs on your machine. But it only runs on your machine.

If someone else clones the repo, they need to:
- Install the exact same version of Python, Node.js, or Java
- Install all the same dependencies
- Set the same environment variables
- Have the same OS configuration

This is fragile, slow, and does not scale. This is the problem Docker solves.

---

## The Problem Docker Solves

In the build phase, you ran:

```bash
pip install -r requirements.txt
DATABASE_URL=postgresql://... python app.py
```

This works because your machine has Python 3.13 installed. But:

- A server running Amazon Linux 2023 might have Python 3.9
- A colleague's laptop might have Python 3.10
- The CI/CD pipeline might have Python 3.12

Different versions behave differently. Dependencies compiled for one OS may not work on another. The classic problem in software:

> **"It works on my machine"**

Docker eliminates this by packaging the application together with its entire environment — the runtime, the dependencies, the configuration — into a single portable unit called a **container image**.

---

## Virtualisation vs Containerisation

Before Docker, the solution to "it works on my machine" was **virtualisation**. To understand why Docker is better, you need to understand both.

### Traditional Virtualisation (Virtual Machines)

A **Virtual Machine (VM)** emulates an entire computer — CPU, memory, storage, and a full operating system — inside your physical machine.

```
┌─────────────────────────────────────────────────┐
│                Physical Server                   │
│                                                 │
│  ┌──────────────────────────────────────────┐   │
│  │           Hypervisor (VMware / KVM)       │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────┐  │
│  │     VM 1    │  │     VM 2    │  │   VM 3  │  │
│  │  Guest OS   │  │  Guest OS   │  │Guest OS │  │
│  │  (Linux)    │  │  (Windows)  │  │(Linux)  │  │
│  │    App A    │  │    App B    │  │  App C  │  │
│  └─────────────┘  └─────────────┘  └─────────┘  │
└─────────────────────────────────────────────────┘
```

Each VM includes a full operating system — kernel, system libraries, everything. This makes VMs:
- **Heavy** — each VM can be gigabytes in size
- **Slow to start** — booting a full OS takes minutes
- **Resource intensive** — each VM needs its own CPU and memory allocation

### Containerisation

A **container** does not virtualise hardware or run a full OS. Instead, it shares the host OS kernel and isolates only the application and its dependencies.

```
┌─────────────────────────────────────────────────┐
│                Physical Server                   │
│                                                 │
│  ┌──────────────────────────────────────────┐   │
│  │              Host OS (Linux)              │   │
│  │                                          │   │
│  │  ┌──────────────────────────────────┐    │   │
│  │  │        Docker Engine             │    │   │
│  │  └──────────────────────────────────┘    │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────┐  │
│  │ Container 1 │  │ Container 2 │  │  Cont 3 │  │
│  │  App + Deps │  │  App + Deps │  │App+Deps │  │
│  │  (no OS)    │  │  (no OS)    │  │ (no OS) │  │
│  └─────────────┘  └─────────────┘  └─────────┘  │
└─────────────────────────────────────────────────┘
```

Containers are:
- **Lightweight** — megabytes, not gigabytes
- **Fast to start** — milliseconds, not minutes
- **Efficient** — share the host OS kernel, no duplication

### VM vs Container — Side by Side

| | Virtual Machine | Container |
|---|---|---|
| Includes | Full OS + App | App + dependencies only |
| Size | Gigabytes | Megabytes |
| Startup time | Minutes | Seconds |
| Isolation | Full hardware isolation | Process-level isolation |
| Portability | Limited | Runs anywhere Docker runs |
| Use case | Run different OS types | Package and ship applications |

### Do VMs and Containers replace each other?

No — they are complementary. In production (which you will see in the Kubernetes phase), containers run **inside** VMs:

```
AWS EC2 Instance (VM)
  └── Docker Engine
        ├── Container: backend
        ├── Container: frontend
        └── Container: database
```

The VM gives you an isolated server. Docker gives you isolated, portable applications running on that server.

---

## What is Docker?

**Docker** is a platform for building, shipping, and running containers.

It has three core components:

| Component | What it is | Analogy |
|-----------|-----------|---------|
| **Dockerfile** | A text file with instructions to build an image | A recipe |
| **Image** | A read-only snapshot built from a Dockerfile | A packaged meal kit |
| **Container** | A running instance of an image | The meal being eaten |

```
Dockerfile  ──▶  docker build  ──▶  Image  ──▶  docker run  ──▶  Container
(instructions)                    (snapshot)                     (running process)
```

You can run many containers from the same image, just like you can cook the same recipe many times.

### Docker Hub

**Docker Hub** is a public registry where Docker images are stored and shared. When you run:

```bash
docker pull postgres:15
```

Docker downloads the `postgres` image tagged `15` from Docker Hub. Anyone in the world can pull and run it.

You can also push your own images to Docker Hub or a private registry like **AWS ECR**.

---

## Docker Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Client (CLI)                   │
│              docker build / run / push / pull            │
└─────────────────────────┬───────────────────────────────┘
                          │  REST API
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    Docker Daemon (dockerd)               │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    Images    │  │  Containers  │  │   Networks   │  │
│  │              │  │              │  │   Volumes    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                Docker Registry (Docker Hub / ECR)        │
└─────────────────────────────────────────────────────────┘
```

- The **Docker Client** is the `docker` command you type in the terminal
- The **Docker Daemon** is the background service that does the actual work
- The **Registry** is where images are stored and shared

---

## Installing Docker on Amazon Linux 2023

Amazon Linux 2023 is the OS used on AWS EC2 instances. This is how you install Docker on a fresh server.

```bash
# Update the package manager
sudo dnf update -y

# Install Docker
sudo dnf install -y docker

# Start the Docker service
sudo systemctl start docker

# Enable Docker to start automatically on reboot
sudo systemctl enable docker

# Add your user to the docker group so you don't need sudo every time
sudo usermod -aG docker $USER

# Apply the group change without logging out
newgrp docker

# Verify Docker is running
docker --version
docker ps
```

Verify the installation:

```bash
docker run hello-world
```

You should see: `Hello from Docker!`

---

## Core Docker Commands

### Images

```bash
# Download an image from Docker Hub
docker pull nginx:alpine

# List all images on your machine
docker images

# Remove an image
docker rmi nginx:alpine

# Search for images on Docker Hub
docker search postgres
```

### Containers

```bash
# Run a container (downloads image if not present)
docker run nginx:alpine

# Run in detached mode (background)
docker run -d nginx:alpine

# Run with a name
docker run -d --name my-nginx nginx:alpine

# Run with port mapping  host:container
docker run -d -p 8080:80 --name my-nginx nginx:alpine

# List running containers
docker ps

# List all containers including stopped
docker ps -a

# Stop a container
docker stop my-nginx

# Start a stopped container
docker start my-nginx

# Remove a container
docker rm my-nginx

# Remove a running container forcefully
docker rm -f my-nginx
```

### Logs and Exec

```bash
# View container logs
docker logs my-nginx

# Follow logs in real time
docker logs -f my-nginx

# Open a shell inside a running container
docker exec -it my-nginx sh

# Run a one-off command inside a container
docker exec my-nginx ls /usr/share/nginx/html
```

### Cleanup

```bash
# Remove all stopped containers
docker container prune

# Remove all unused images
docker image prune

# Remove everything unused (containers, images, networks, volumes)
docker system prune -a
```

---

## The Dockerfile

A **Dockerfile** is a text file that contains step-by-step instructions for building a Docker image. Each instruction creates a new layer in the image.

### Dockerfile Instructions

| Instruction | Purpose |
|-------------|---------|
| `FROM` | Base image to start from |
| `WORKDIR` | Set the working directory inside the container |
| `COPY` | Copy files from your machine into the image |
| `RUN` | Execute a command during the build (install dependencies, etc.) |
| `ENV` | Set environment variables |
| `EXPOSE` | Document which port the container listens on |
| `CMD` | The default command to run when the container starts |
| `ENTRYPOINT` | Like CMD but not overridable |

### How layers work

Every instruction in a Dockerfile creates a layer. Docker caches layers — if a layer has not changed, Docker reuses the cached version instead of rebuilding it. This makes builds fast.

```
FROM python:3.11-slim          ← Layer 1 (base OS + Python)
WORKDIR /app                   ← Layer 2 (set directory)
COPY requirements.txt .        ← Layer 3 (copy dependency file)
RUN pip install -r ...         ← Layer 4 (install dependencies) ← CACHED if requirements.txt unchanged
COPY app.py .                  ← Layer 5 (copy source code)     ← rebuilds when code changes
CMD ["gunicorn", ...]          ← Layer 6 (set start command)
```

This is why `COPY requirements.txt` comes before `COPY app.py` — dependencies change less often than code, so the expensive `pip install` layer stays cached.

---

## Practical — Dockerfile for the Employee Directory App

### Backend Dockerfile

```
employee-app/backend/Dockerfile
```

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5000

CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "app:app"]
```

Line by line:

| Line | What it does |
|------|-------------|
| `FROM python:3.11-slim` | Start from an official Python 3.11 image (slim = smaller size) |
| `WORKDIR /app` | All subsequent commands run from `/app` inside the container |
| `COPY requirements.txt .` | Copy the dependency file first (for layer caching) |
| `RUN pip install ...` | Install all Python dependencies inside the image |
| `COPY app.py .` | Copy the application code |
| `EXPOSE 5000` | Document that the app listens on port 5000 |
| `CMD [...]` | Start the app with Gunicorn (production web server) |

### Frontend Dockerfile

```
employee-app/frontend/Dockerfile
```

```dockerfile
FROM nginx:alpine

COPY index.html /usr/share/nginx/html/index.html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

Line by line:

| Line | What it does |
|------|-------------|
| `FROM nginx:alpine` | Start from official Nginx image (alpine = very small) |
| `COPY index.html ...` | Copy the HTML file into the Nginx web root |
| `COPY nginx.conf ...` | Replace the default Nginx config with ours |
| `EXPOSE 80` | Document that Nginx listens on port 80 |

### Build the images

```bash
# Build the backend image
cd employee-app/backend
docker build -t employee-backend:v1 .

# Build the frontend image
cd ../frontend
docker build -t employee-frontend:v1 .

# Verify both images exist
docker images | grep employee
```

The `.` at the end means "use the current directory as the build context" — Docker sends all files in that directory to the daemon.

### Run the images as containers

```bash
# Run the backend (needs a running postgres)
docker run -d \
  --name backend \
  -p 5000:5000 \
  -e DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees \
  employee-backend:v1

# Run the frontend
docker run -d \
  --name frontend \
  -p 8080:80 \
  employee-frontend:v1
```

### Run the full stack manually

The backend needs a database to connect to. Start all three containers on the same network so they can reach each other by name.

**Step 1 — Create a network:**

```bash
docker network create employee-network
```

**Step 2 — Start PostgreSQL:**

```bash
docker run -d \
  --name db \
  --network employee-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  -p 5432:5432 \
  postgres:15
```

Wait for Postgres to be ready before starting the backend — it takes a few seconds to initialise:

```bash
# Poll until Postgres accepts connections
until docker exec db pg_isready -U postgres; do
  echo "Waiting for database..."
  sleep 2
done
echo "Database is ready."
```

Or check manually:

```bash
docker exec db pg_isready -U postgres
# /var/run/postgresql:5432 - accepting connections
```

Do not proceed to the next step until you see `accepting connections`.

**Step 3 — Start the backend:**

```bash
docker run -d \
  --name backend \
  --network employee-network \
  -p 5000:5000 \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  employee-backend:v1
```

Verify the backend is up:

```bash
curl http://localhost:5000/api/health
# {"status": "healthy", "db": "connected"}
```

**Step 4 — Start the frontend:**

```bash
docker run -d \
  --name frontend \
  --network employee-network \
  -p 8080:80 \
  employee-frontend:v1
```

**Step 5 — Open in your browser:**

The frontend is mapped to port `8080` on your machine and the backend to port `5000`. Open:

```
http://localhost:8080
```

You will see the Employee Directory UI. Add employees using the form — the frontend calls the backend API which stores records in PostgreSQL.

If you are running this on an EC2 instance instead of your local machine, replace `localhost` with the EC2 public IP:

```
http://<EC2_PUBLIC_IP>:8080
```

Make sure port `8080` is open in the EC2 security group.

**Step 6 — Port forward (alternative access without opening ports):**

If you cannot open ports on the host or want to access a container running on a remote machine through your local browser, use SSH port forwarding:

```bash
# Run this on your LOCAL machine (not the server)
ssh -i your-key.pem \
  -L 8080:localhost:8080 \
  -L 5000:localhost:5000 \
  ec2-user@<EC2_PUBLIC_IP>
```

This tunnels:
- `localhost:8080` on your laptop → port `8080` on the EC2 instance (frontend)
- `localhost:5000` on your laptop → port `5000` on the EC2 instance (backend)

Then open `http://localhost:8080` in your browser — traffic travels through the SSH tunnel to the containers on the remote server. No need to open any ports in the security group beyond port `22`.

**Verify all three containers are running:**

```bash
docker ps
```

You should see `db`, `backend`, and `frontend` all with status `Up`.

**Watch the backend logs as you use the app:**

```bash
docker logs -f backend
```

Every request you make in the browser appears here in real time.

**Connect to the database directly:**

```bash
docker exec -it db psql -U postgres -d employees
```

```sql
-- List all employees
SELECT * FROM employees;

-- Exit
\q
```

**Clean up:**

```bash
docker rm -f frontend backend db
docker network rm employee-network
```

---

## Docker Networking

By default, containers are isolated — they cannot talk to each other or to the host. Docker networking controls how containers communicate.

### Network types

| Type | Description | Use case |
|------|-------------|---------|
| `bridge` | Default. Containers on the same bridge can communicate by container name | Local development |
| `host` | Container shares the host's network stack | Performance-sensitive apps |
| `none` | No networking | Fully isolated containers |
| `overlay` | Multi-host networking | Docker Swarm / Kubernetes |

### The default bridge network

When you run a container without specifying a network, it joins the default `bridge` network. Containers on the default bridge **cannot** reach each other by name — only by IP address.

### User-defined bridge networks

When you create your own bridge network, containers on it can reach each other **by container name**. This is how the Employee Directory app works — the backend connects to the database using the hostname `db`.

```bash
# Create a custom network
docker network create employee-network

# List networks
docker network ls

# Inspect a network
docker network inspect employee-network

# Run containers on the same network
docker run -d \
  --name db \
  --network employee-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  postgres:15

docker run -d \
  --name backend \
  --network employee-network \
  -p 5000:5000 \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  employee-backend:v1

docker run -d \
  --name frontend \
  --network employee-network \
  -p 8080:80 \
  employee-frontend:v1
```

Now `backend` can reach `db` by name because they are on the same user-defined network. Open `http://localhost:8080` — the full app is running.

```bash
# Remove a network (all containers must be disconnected first)
docker network rm employee-network
```

---

## Docker Volumes

Containers are **ephemeral** — when a container is removed, all data inside it is lost. If you remove the `db` container, all your employee records are gone.

**Volumes** solve this by storing data outside the container, on the host machine. The data persists even when the container is removed.

### Volume types

| Type | Description | Use case |
|------|-------------|---------|
| **Named volume** | Docker manages the storage location | Databases, persistent app data |
| **Bind mount** | You specify the exact path on the host | Development — live code reload |
| **tmpfs** | Stored in memory only | Sensitive data that should not persist |

### Named volumes

```bash
# Create a named volume
docker volume create postgres-data

# List volumes
docker volume ls

# Inspect a volume (shows where data is stored on the host)
docker volume inspect postgres-data

# Run postgres with a volume so data persists
docker run -d \
  --name db \
  --network employee-network \
  -v postgres-data:/var/lib/postgresql/data \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  postgres:15

# Remove a volume
docker volume rm postgres-data

# Remove all unused volumes
docker volume prune
```

Now if you stop and remove the `db` container and start a new one with the same volume, all your data is still there.

### Bind mounts — for development

A bind mount maps a directory on your host machine directly into the container. Changes you make to files on your host are immediately reflected inside the container — no rebuild needed.

```bash
# Mount local backend code into the container
docker run -d \
  --name backend-dev \
  --network employee-network \
  -p 5000:5000 \
  -v $(pwd)/backend:/app \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  employee-backend:v1
```

Now you can edit `app.py` on your machine and the container sees the changes immediately.

---

## Environment Variables in Docker

Applications need configuration — database URLs, API keys, ports. You should never hardcode these into your image. Pass them in at runtime using environment variables.

```bash
# Pass a single env var
docker run -e DATABASE_URL=postgresql://... employee-backend:v1

# Pass multiple env vars
docker run \
  -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
  -e AWS_REGION=us-east-1 \
  -e ENVIRONMENT=dev \
  employee-backend:v1

# Pass env vars from a file
docker run --env-file .env employee-backend:v1
```

Example `.env` file:

```
DATABASE_URL=postgresql://postgres:postgres@db:5432/employees
AWS_REGION=us-east-1
ENVIRONMENT=dev
```

---

## Docker Image Tagging and Registries

### Tagging

A tag identifies a specific version of an image. The format is:

```
registry/repository:tag
```

Examples:

```
postgres:15                                          ← Docker Hub official image
nginx:alpine                                         ← Docker Hub official image, alpine variant
075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend:v1   ← AWS ECR private image
```

```bash
# Tag an image
docker tag employee-backend:v1 employee-backend:latest

# Tag for pushing to ECR
docker tag employee-backend:v1 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend:v1
```

### Pushing to a registry

```bash
# Login to Docker Hub
docker login

# Push to Docker Hub
docker push yourusername/employee-backend:v1

# Login to AWS ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 075120018043.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 075120018043.dkr.ecr.us-east-1.amazonaws.com/employee-backend:v1
```

---

## Docker Compose

Running multiple containers with individual `docker run` commands is tedious. You have to remember the right flags, the right order, the right network names.

**Docker Compose** lets you define your entire multi-container application in a single YAML file and manage it with one command.

### docker-compose.yml structure

```yaml
version: "3.8"          # Compose file format version

services:               # Each service is a container
  service-name:
    image: ...          # Use an existing image
    build: ./path       # Or build from a Dockerfile
    ports:
      - "host:container"
    environment:
      KEY: value
    volumes:
      - name:/path
    depends_on:
      - other-service
    networks:
      - network-name

volumes:                # Named volumes
  volume-name:

networks:               # Custom networks
  network-name:
```

### The Employee Directory docker-compose.yml

```yaml
version: "3.8"

services:
  db:
    image: postgres:15
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: employees
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    environment:
      DATABASE_URL: postgresql://postgres:postgres@db:5432/employees
      ENVIRONMENT: dev
    ports:
      - "5000:5000"
    depends_on:
      db:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "8080:80"
    depends_on:
      - backend

volumes:
  postgres-data:
```

What this defines:
- `db` — PostgreSQL container with a named volume so data persists
- `backend` — built from `./backend/Dockerfile`, connects to `db` by name
- `frontend` — built from `./frontend/Dockerfile`, proxies `/api/` to `backend:5000`
- All three are on the same default Compose network automatically

### Docker Compose commands

```bash
# Start all services (build images if needed)
docker compose up --build

# Start in detached mode (background)
docker compose up -d --build

# View logs for all services
docker compose logs

# Follow logs for a specific service
docker compose logs -f backend

# List running services
docker compose ps

# Stop all services
docker compose down

# Stop and remove volumes (wipes the database)
docker compose down -v

# Rebuild a specific service
docker compose build backend

# Restart a specific service
docker compose restart backend

# Open a shell in a running service
docker compose exec backend sh

# Run a one-off command in a service
docker compose exec db psql -U postgres -d employees
```

### Practical — Run the Employee Directory with Docker Compose

```bash
cd employee-app
docker compose up --build
```

Docker Compose will:
1. Build the `backend` image from `./backend/Dockerfile`
2. Build the `frontend` image from `./frontend/Dockerfile`
3. Pull the `postgres:15` image from Docker Hub
4. Create a shared network for all three services
5. Start `db` first, then `backend`, then `frontend`

Open `http://localhost:8080` — the full application is running.

Test the API directly:

```bash
# Health check
curl http://localhost:5000/api/health

# List employees
curl http://localhost:5000/api/employees

# Add an employee
curl -X POST http://localhost:5000/api/employees \
  -F "name=Jane Smith" \
  -F "email=jane@example.com" \
  -F "role=Engineer" \
  -F "department=Engineering"

# Stats
curl http://localhost:5000/api/stats
```

Stop everything:

```bash
docker compose down
```

Stop and wipe the database:

```bash
docker compose down -v
```

---

## How the Build Phase Became the Release Phase

Look at what just happened:

```
Build phase (previous lecture):
  requirements.txt  ──▶  pip install  ──▶  python app.py  (runs on YOUR machine only)

Release phase (this lecture):
  requirements.txt  ──▶  Dockerfile  ──▶  docker build  ──▶  image  ──▶  runs ANYWHERE
```

The Dockerfile is the build steps automated and frozen into a reproducible image. Instead of you running `pip install` manually on your machine, Docker runs it inside a controlled environment during `docker build`.

The image is the **release artifact** — the output of the release phase. It is:
- **Immutable** — once built, it never changes
- **Versioned** — tagged with a specific version (`v1`, `be-dev-20240101`)
- **Portable** — runs the same way on any machine with Docker installed
- **Stored** — pushed to a registry (Docker Hub or ECR) so anyone can pull it

This is what gets deployed in the next phase — Kubernetes pulls the image from ECR and runs it as a container on the cluster.

---

## Docker Cheat Sheet

### Images
```bash
docker pull image:tag          # download image
docker images                  # list images
docker rmi image:tag           # remove image
docker build -t name:tag .     # build image from Dockerfile
docker tag src:tag dst:tag     # tag an image
docker push name:tag           # push to registry
```

### Containers
```bash
docker run -d -p host:cont --name name image:tag   # run container
docker ps                      # list running containers
docker ps -a                   # list all containers
docker stop name               # stop container
docker start name              # start stopped container
docker rm name                 # remove container
docker rm -f name              # force remove running container
docker logs -f name            # follow logs
docker exec -it name sh        # shell into container
```

### Networks
```bash
docker network create name     # create network
docker network ls              # list networks
docker network inspect name    # inspect network
docker network rm name         # remove network
```

### Volumes
```bash
docker volume create name      # create volume
docker volume ls               # list volumes
docker volume inspect name     # inspect volume
docker volume rm name          # remove volume
docker volume prune            # remove unused volumes
```

### Docker Compose
```bash
docker compose up --build      # build and start
docker compose up -d           # start in background
docker compose down            # stop and remove
docker compose down -v         # stop and remove including volumes
docker compose ps              # list services
docker compose logs -f svc     # follow service logs
docker compose exec svc sh     # shell into service
docker compose build svc       # rebuild a service
```

### Cleanup
```bash
docker system prune -a         # remove everything unused
docker container prune         # remove stopped containers
docker image prune             # remove dangling images
docker volume prune            # remove unused volumes
```

---

## What Comes Next — Kubernetes

Docker Compose is great for running the application on a single machine. But in production you need:

- **High availability** — if one container crashes, another starts automatically
- **Scaling** — run multiple copies of the backend to handle more traffic
- **Rolling updates** — deploy new versions with zero downtime
- **Load balancing** — distribute traffic across multiple containers
- **Self-healing** — automatically restart failed containers

A single Docker host cannot provide all of this reliably. This is where **Kubernetes** comes in.

```
Docker Compose (now):
  One machine  →  docker compose up  →  3 containers running

Kubernetes (next):
  Many machines (cluster)  →  kubectl apply  →  pods running across nodes
  with auto-scaling, self-healing, rolling updates, load balancing
```

The same Docker images you built and pushed to ECR in this phase are what Kubernetes pulls and runs in the next phase. The image is the bridge between Docker and Kubernetes.

Move to [Kubernetes →](../README.md#deployment-guide)
