# CI/CD — From Manual Deployments to GitHub Actions

---

## Where We Are in the SDLC

```
┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   ┌─────────┐   ┌──────────────┐   ┌─────────┐
│  PLAN  │──▶│  CODE  │──▶│ BUILD  │──▶│  TEST  │──▶│ RELEASE │──▶│    DEPLOY    │──▶│ MONITOR │
│        │   │        │   │        │   │        │   │ (Docker)│   │  (CI/CD)     │   │ Grafana │
└────────┘   └────────┘   └────────┘   └────────┘   └─────────┘   └──────────────┘   └─────────┘
                                                                            ↑
                                                                      You are here
```

So far in this course:
- **Build** — we installed dependencies and ran the app in three languages
- **Docker** — we packaged the app into container images

The problem we are left with:

> Every time a developer pushes code, someone has to manually run tests, build the Docker image, push it to a registry, and deploy it. This is slow, error-prone, and does not scale.

This is the problem CI/CD solves.

---

## What is CI/CD?

**CI/CD** stands for **Continuous Integration / Continuous Delivery** (or Continuous Deployment).

These are two related but distinct concepts:

### Continuous Integration (CI)

**CI** is the practice of automatically testing every code change as soon as it is pushed to the repository.

The goal: catch bugs early, before they reach production.

```
Developer pushes code
        │
        ▼
CI system automatically:
  ├── pulls the latest code
  ├── installs dependencies
  ├── runs all tests
  └── reports pass or fail
```

If tests fail, the developer is notified immediately. The broken code never moves forward.

### Continuous Delivery (CD)

**CD** extends CI by automatically building and packaging the application after tests pass, and deploying it to an environment.

```
Tests pass (CI)
        │
        ▼
CD system automatically:
  ├── builds the Docker image
  ├── pushes image to registry (ECR)
  └── deploys to the target environment
```

### CI vs CD — What is the difference?

| | CI | CD |
|---|---|---|
| Stands for | Continuous Integration | Continuous Delivery / Deployment |
| Triggered by | Every push | After CI passes |
| Does | Run tests | Build, push, deploy |
| Goal | Catch bugs early | Ship code fast and reliably |

In practice, most pipelines do both — they are referred to together as **CI/CD**.

### Is GitHub Actions CI or CD?

**Both.** GitHub Actions can run tests (CI) and also build, push, and deploy (CD). You decide what the pipeline does by writing the workflow file. In this project, our pipeline does all three: test → build → deploy.

---

## The Problem Without CI/CD

Imagine a team of 5 developers all pushing code to the same repository:

```
Without CI/CD:
  Dev 1 pushes → someone manually tests → maybe works
  Dev 2 pushes → nobody tested → breaks production
  Dev 3 pushes → conflicts with Dev 2 → nobody knows
  
  Result: broken production, manual firefighting, slow releases
```

```
With CI/CD:
  Dev 1 pushes → pipeline runs tests automatically → passes → deployed
  Dev 2 pushes → pipeline runs tests → FAILS → dev notified, not deployed
  Dev 3 pushes → pipeline runs tests → passes → deployed safely
  
  Result: only tested code reaches production, fast and reliable releases
```

---

## CI/CD Tools

There are many CI/CD tools available. They all do the same thing — run automated pipelines when code changes — but they differ in where they run and how they are configured.

| Tool | Type | Config file | Where it runs |
|------|------|-------------|---------------|
| **GitHub Actions** | Cloud (SaaS) | `.github/workflows/*.yml` | GitHub's servers |
| **Jenkins** | Self-hosted | `Jenkinsfile` | Your own server |
| **CircleCI** | Cloud (SaaS) | `.circleci/config.yml` | CircleCI's servers |
| **GitLab CI** | Cloud or self-hosted | `.gitlab-ci.yml` | GitLab runners |
| **AWS CodePipeline** | Cloud (AWS) | Console / CloudFormation | AWS |
| **ArgoCD** | Self-hosted (K8s) | Kubernetes manifests | Inside your cluster |

### Which tool should you use?

| Situation | Recommended tool |
|-----------|-----------------|
| Code is on GitHub, want simplest setup | GitHub Actions |
| Need full control, self-hosted | Jenkins |
| Already on GitLab | GitLab CI |
| Deploying to Kubernetes with GitOps | ArgoCD |
| All-in on AWS | CodePipeline |

In this project we use **GitHub Actions** because our code is already on GitHub — no extra infrastructure needed.

> This repo also has a `Jenkinsfile` and `.circleci/config.yml` so you can compare how the same pipeline looks in different tools.

---

## Introduction to GitHub Actions

GitHub Actions is GitHub's built-in CI/CD platform. It is free for public repositories and has a generous free tier for private repositories.

### Key concepts

| Concept | What it is |
|---------|-----------|
| **Workflow** | A YAML file that defines an automated process |
| **Trigger** | The event that starts the workflow (push, PR, schedule) |
| **Job** | A group of steps that run on the same machine |
| **Step** | A single task inside a job (run a command, use an action) |
| **Action** | A reusable step published by the community (e.g. `actions/checkout`) |
| **Runner** | The machine that executes the job (`ubuntu-latest`, `self-hosted`) |

### Workflow file location

All workflow files live in:

```
.github/
└── workflows/
    ├── test.yml       ← runs on every branch push
    ├── deploy-dev.yml ← runs on push to develop
    └── deploy-prod.yml← runs on push to main
```

### Basic workflow structure

```yaml
name: My Pipeline          # name shown in GitHub UI

on:                        # what triggers this workflow
  push:
    branches: [main]

jobs:
  my-job:                  # job name
    runs-on: ubuntu-latest # machine to run on

    steps:
      - name: Checkout code
        uses: actions/checkout@v4    # use a community action

      - name: Run a command
        run: echo "Hello from CI"    # run a shell command
```

---

## Secrets, Variables, and Environments

Before writing the workflows, you need to understand how GitHub Actions handles sensitive data and configuration.

### The problem

Your pipeline needs:
- AWS credentials to push to ECR and deploy
- SSH keys to connect to EC2
- Database URLs, API keys, region names

You must **never** put these directly in your workflow YAML file — that file is committed to the repo and visible to everyone.

GitHub provides three ways to store this data safely.

---

### 1. Repository Secrets

Secrets are encrypted values stored in GitHub. They are injected into the workflow as environment variables at runtime. They are **never** visible in logs.

**Where to set them:**
```
GitHub repo → Settings → Secrets and variables → Actions → Secrets → New repository secret
```

**How to use them in a workflow:**
```yaml
- name: Configure AWS
  env:
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

**Rules:**
- Values are masked in logs (shown as `***`)
- Cannot be read back once saved
- Available to all branches and all workflows in the repo

---

### 2. Repository Variables

Variables are plain (non-secret) configuration values. Use these for things that are not sensitive but may change — like a region name or registry URL.

**Where to set them:**
```
GitHub repo → Settings → Secrets and variables → Actions → Variables → New repository variable
```

**How to use them:**
```yaml
run: docker build -t ${{ vars.ECR_REGISTRY }}/employee-backend:v1 .
```

---

### 3. Environments

An **environment** in GitHub Actions is a named deployment target (e.g. `development`, `production`). Each environment can have its own secrets and variables that override the repository-level ones.

```
Repository secrets/vars  ← available to all workflows
        │
        ▼
Environment secrets/vars ← override repo-level, only available when that environment is used
```

**Why this matters:**

Your `development` environment connects to a dev EC2 instance. Your `production` environment connects to a prod EC2 instance. They need different SSH keys, different server IPs, different database URLs. Environments let you store these separately.

**Where to set them:**
```
GitHub repo → Settings → Environments → New environment
```

Then in your workflow:
```yaml
jobs:
  deploy:
    environment: production   # ← this job uses production environment secrets
```

**Environments also support protection rules:**
- Require manual approval before deploying to production
- Restrict which branches can deploy to an environment

---

### Secrets needed for this project

#### Repository-level secrets (shared across all environments)

| Secret name | Value | Purpose |
|-------------|-------|---------|
| `AWS_ACCESS_KEY_ID` | Your IAM key ID | Authenticate to AWS |
| `AWS_SECRET_ACCESS_KEY` | Your IAM secret key | Authenticate to AWS |

#### Repository-level variables (shared across all environments)

| Variable name | Value | Purpose |
|---------------|-------|---------|
| `AWS_REGION` | `us-east-1` | AWS region |
| `ECR_REGISTRY` | `075120018043.dkr.ecr.us-east-1.amazonaws.com` | ECR registry URL |

#### `development` environment secrets

| Secret name | Value | Purpose |
|-------------|-------|---------|
| `EC2_HOST` | Public IP of dev EC2 | SSH target |
| `EC2_USER` | `ec2-user` | SSH username |
| `EC2_SSH_KEY` | Private key (PEM contents) | SSH authentication |

#### `production` environment secrets

| Secret name | Value | Purpose |
|-------------|-------|---------|
| `EC2_HOST` | Public IP of prod EC2 | SSH target |
| `EC2_USER` | `ec2-user` | SSH username |
| `EC2_SSH_KEY` | Private key (PEM contents) | SSH authentication |

---

### How to give GitHub Actions access to AWS

GitHub Actions needs AWS credentials to:
- Push Docker images to ECR
- (Later) deploy to EKS

**Step 1 — Create an IAM user for CI/CD**

In the AWS console:
```
IAM → Users → Create user → Name: github-actions-ci
Attach policies:
  - AmazonEC2ContainerRegistryPowerUser   (push/pull ECR images)
  - AmazonEC2FullAccess                   (deploy to EC2 — for this phase)
```

**Step 2 — Create access keys**
```
IAM → Users → github-actions-ci → Security credentials → Create access key
```

Copy the `Access key ID` and `Secret access key`.

**Step 3 — Add to GitHub secrets**
```
GitHub repo → Settings → Secrets and variables → Actions
Add:
  AWS_ACCESS_KEY_ID     = <your access key id>
  AWS_SECRET_ACCESS_KEY = <your secret access key>
```

---

### How to give GitHub Actions SSH access to EC2

**Step 1 — Generate an SSH key pair (or use your existing one)**

```bash
ssh-keygen -t rsa -b 4096 -f github-actions-key -N ""
# Creates: github-actions-key (private) and github-actions-key.pub (public)
```

**Step 2 — Add the public key to your EC2 instance**

```bash
# On your EC2 instance
cat github-actions-key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**Step 3 — Add the private key to GitHub environment secrets**

```
GitHub repo → Settings → Environments → development → Add secret
  EC2_SSH_KEY = <paste the entire contents of github-actions-key>
  EC2_HOST    = <your EC2 public IP>
  EC2_USER    = ec2-user
```

The private key should look like:
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAA...
-----END OPENSSH PRIVATE KEY-----
```

---

## Branching Strategy and Environments

This project uses a three-branch strategy that maps directly to environments:

```
feature/my-feature  ──┐
fix/some-bug        ──┤──▶  test only (no deploy)
chore/update-deps   ──┘

develop             ──────▶  test + build + deploy to DEVELOPMENT EC2

main                ──────▶  test + build + deploy to PRODUCTION EC2
```

### Branch rules

| Branch | Who pushes here | What pipeline runs | Where it deploys |
|--------|----------------|-------------------|-----------------|
| Any feature/fix branch | Developers | `test.yml` — tests only | Nowhere |
| `develop` | Merge from feature branches | `deploy-dev.yml` — test + build + deploy | Dev EC2 |
| `main` | Merge from develop (release) | `deploy-prod.yml` — test + build + deploy | Prod EC2 |

### Why this matters

- Developers work on feature branches — their code is tested automatically on every push
- When a feature is ready, it is merged to `develop` — automatically deployed to the dev server for testing
- When the team is happy with `develop`, it is merged to `main` — automatically deployed to production
- Production deployments require a manual approval step (configured in the GitHub environment settings)

---

## The Three Workflow Files

### Workflow 1 — `test.yml` (any branch except develop and main)

Runs on every push to any branch that is not `develop` or `main`. Only runs tests — no build, no deploy.

```
.github/workflows/test.yml
```

```yaml
name: Test

on:
  push:
    branches-ignore:
      - develop
      - main

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.11"

      - name: Install dependencies
        run: pip install -r backend/requirements.txt

      - name: Run tests
        run: |
          cd backend
          pytest -v
        env:
          DATABASE_URL: sqlite:///test.db
```

**What this does:**
- Triggers on any push to feature branches, fix branches, etc.
- Checks out the code
- Installs Python and dependencies
- Runs pytest
- If tests fail, the developer sees a red ✗ on their commit in GitHub

---

### Workflow 2 — `deploy-dev.yml` (push to develop)

Runs on every push to `develop`. Tests, builds Docker images, pushes to ECR, deploys to the dev EC2 instance.

```
.github/workflows/deploy-dev.yml
```

```yaml
name: Deploy to Development

on:
  push:
    branches: [develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: "3.11"

      - name: Install dependencies
        run: pip install -r backend/requirements.txt

      - name: Run tests
        run: |
          cd backend
          pytest -v
        env:
          DATABASE_URL: sqlite:///test.db

  build-and-push:
    runs-on: ubuntu-latest
    needs: test
    outputs:
      tag: ${{ steps.tag.outputs.tag }}
    steps:
      - uses: actions/checkout@v4

      - name: Generate image tag
        id: tag
        run: echo "tag=dev-$(date +'%Y%m%d-%H%M%S')" >> $GITHUB_OUTPUT

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ vars.AWS_REGION }}

      - name: Login to ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push backend
        run: |
          docker build -t ${{ vars.ECR_REGISTRY }}/employee-backend:${{ steps.tag.outputs.tag }} backend/
          docker push ${{ vars.ECR_REGISTRY }}/employee-backend:${{ steps.tag.outputs.tag }}

      - name: Build and push frontend
        run: |
          docker build -t ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ steps.tag.outputs.tag }} frontend/
          docker push ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ steps.tag.outputs.tag }}

  deploy:
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: development
    steps:
      - name: Deploy to dev EC2
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            # Authenticate Docker to ECR
            aws ecr get-login-password --region ${{ vars.AWS_REGION }} | \
              docker login --username AWS --password-stdin ${{ vars.ECR_REGISTRY }}

            # Pull latest images
            docker pull ${{ vars.ECR_REGISTRY }}/employee-backend:${{ needs.build-and-push.outputs.tag }}
            docker pull ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ needs.build-and-push.outputs.tag }}

            # Stop and remove old containers
            docker rm -f backend frontend || true

            # Start new containers
            docker run -d --name backend --restart unless-stopped \
              --network employee-network \
              -p 5000:5000 \
              -e DATABASE_URL=${{ secrets.DATABASE_URL }} \
              ${{ vars.ECR_REGISTRY }}/employee-backend:${{ needs.build-and-push.outputs.tag }}

            docker run -d --name frontend --restart unless-stopped \
              --network employee-network \
              -p 8080:80 \
              ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ needs.build-and-push.outputs.tag }}
```

---

### Workflow 3 — `deploy-prod.yml` (push to main)

Identical pipeline to dev but uses the `production` environment — which has different secrets (prod EC2 host, prod SSH key) and requires manual approval.

```
.github/workflows/deploy-prod.yml
```

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: "3.11"

      - name: Install dependencies
        run: pip install -r backend/requirements.txt

      - name: Run tests
        run: |
          cd backend
          pytest -v
        env:
          DATABASE_URL: sqlite:///test.db

  build-and-push:
    runs-on: ubuntu-latest
    needs: test
    outputs:
      tag: ${{ steps.tag.outputs.tag }}
    steps:
      - uses: actions/checkout@v4

      - name: Generate image tag
        id: tag
        run: echo "tag=prod-$(date +'%Y%m%d-%H%M%S')" >> $GITHUB_OUTPUT

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ vars.AWS_REGION }}

      - name: Login to ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push backend
        run: |
          docker build -t ${{ vars.ECR_REGISTRY }}/employee-backend:${{ steps.tag.outputs.tag }} backend/
          docker push ${{ vars.ECR_REGISTRY }}/employee-backend:${{ steps.tag.outputs.tag }}

      - name: Build and push frontend
        run: |
          docker build -t ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ steps.tag.outputs.tag }} frontend/
          docker push ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ steps.tag.outputs.tag }}

  deploy:
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: production        # ← uses production secrets + requires approval
    steps:
      - name: Deploy to prod EC2
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            aws ecr get-login-password --region ${{ vars.AWS_REGION }} | \
              docker login --username AWS --password-stdin ${{ vars.ECR_REGISTRY }}

            docker pull ${{ vars.ECR_REGISTRY }}/employee-backend:${{ needs.build-and-push.outputs.tag }}
            docker pull ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ needs.build-and-push.outputs.tag }}

            docker rm -f backend frontend || true

            docker run -d --name backend --restart unless-stopped \
              --network employee-network \
              -p 5000:5000 \
              -e DATABASE_URL=${{ secrets.DATABASE_URL }} \
              ${{ vars.ECR_REGISTRY }}/employee-backend:${{ needs.build-and-push.outputs.tag }}

            docker run -d --name frontend --restart unless-stopped \
              --network employee-network \
              -p 8080:80 \
              ${{ vars.ECR_REGISTRY }}/employee-frontend:${{ needs.build-and-push.outputs.tag }}
```

---

## How the Three Workflows Fit Together

```
Developer workflow:

1. Create a feature branch
   git checkout -b feature/add-search

2. Push code → test.yml runs automatically
   git push origin feature/add-search
   → GitHub runs: pytest ✓ or ✗

3. Merge to develop (PR or direct push)
   git checkout develop && git merge feature/add-search && git push
   → GitHub runs: pytest → docker build → docker push → SSH deploy to DEV EC2

4. Test on dev server: http://<DEV-EC2-IP>:8080

5. Merge develop to main (release)
   git checkout main && git merge develop && git push
   → GitHub runs: pytest → docker build → docker push → APPROVAL REQUIRED → SSH deploy to PROD EC2
```

---

## Setting Up the Production Approval Gate

To require manual approval before deploying to production:

```
GitHub repo → Settings → Environments → production
  → Required reviewers → Add yourself or your team lead
  → Save protection rules
```

Now when the `deploy-prod.yml` pipeline reaches the `deploy` job, it will pause and send a notification. A reviewer must click **Approve** in GitHub before the deployment continues.

This prevents accidental production deployments.

---

## EC2 Prerequisites Before Deploying

Before the pipeline can deploy to EC2, the server needs to be set up once manually:

```bash
# On the EC2 instance — run once

# Install Docker
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
newgrp docker

# Install AWS CLI (to authenticate with ECR)
sudo dnf install -y awscli

# Create the Docker network
docker network create employee-network

# Start the database (only needed if not using RDS)
docker run -d --name db --restart unless-stopped \
  --network employee-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:15
```

The pipeline only deploys the `backend` and `frontend` containers — the database runs separately and persists across deployments.

---

## Pipeline Flow — End to End

```
Developer pushes to feature/login-fix
        │
        ▼
test.yml triggers
  └── pytest runs
        ├── PASS → green tick on commit ✓
        └── FAIL → red cross, developer notified ✗

Developer merges to develop
        │
        ▼
deploy-dev.yml triggers
  ├── Job 1: test
  │     └── pytest → must pass to continue
  ├── Job 2: build-and-push (needs: test)
  │     ├── docker build backend → push to ECR as dev-20240101-120000
  │     └── docker build frontend → push to ECR as dev-20240101-120000
  └── Job 3: deploy (needs: build-and-push, environment: development)
        └── SSH into dev EC2
              ├── docker pull new images
              ├── docker rm -f old containers
              └── docker run new containers

Team merges develop to main
        │
        ▼
deploy-prod.yml triggers
  ├── Job 1: test → pytest
  ├── Job 2: build-and-push → ECR tag: prod-20240101-130000
  └── Job 3: deploy (environment: production)
        ├── ⏸ WAITING FOR APPROVAL
        ├── Reviewer approves in GitHub
        └── SSH into prod EC2 → deploy
```

---

## Checking Pipeline Status

```
GitHub repo → Actions tab
```

You will see every workflow run with:
- Which branch triggered it
- Which jobs passed or failed
- Logs for every step
- How long each job took

Click any failed step to see the exact error output.

---

## Summary

| Concept | What it means |
|---------|--------------|
| CI | Automatically test every code push |
| CD | Automatically build and deploy after tests pass |
| Workflow | A YAML file defining the pipeline |
| Job | A group of steps running on one machine |
| Step | A single command or action |
| Secret | Encrypted value — never visible in logs |
| Variable | Plain config value — not sensitive |
| Environment | Named deployment target with its own secrets |
| Runner | The machine that runs the job |
| `needs` | Makes one job wait for another to finish |
| `environment:` | Tells a job which environment secrets to use |

---

## What Comes Next — Kubernetes

Right now the pipeline deploys to a single EC2 instance using `docker run`. This works for learning but has limitations:

- If the EC2 instance goes down, the app goes down
- You can only run one copy of each container
- Scaling requires manual work
- No automatic recovery from failures

In the next phase, the same Docker images pushed to ECR will be deployed to **EKS** (Kubernetes) instead. The pipeline will run `helm upgrade` instead of SSH + `docker run`. Everything else — the test stage, the build stage, the ECR push — stays exactly the same.

```
Now (EC2):
  pipeline → ECR → SSH → docker run on one server

Next (EKS):
  pipeline → ECR → helm upgrade → pods running across multiple nodes
                                  with auto-scaling and self-healing
```
