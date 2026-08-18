# CircleCI — CI/CD with CircleCI

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

This project has three CI/CD pipelines that all do the same thing — test, build, push to ECR, deploy — but in different tools:

| Tool | Config file | Where it runs |
|------|-------------|---------------|
| GitHub Actions | `.github/workflows/` | GitHub's servers |
| Jenkins | `Jenkinsfile` | Your own Jenkins server |
| CircleCI | `.circleci/config.yml` | CircleCI's servers |

This guide covers **CircleCI**. See `CICD.md` for GitHub Actions.

---

## What is CircleCI?

CircleCI is a cloud-based CI/CD platform. Like GitHub Actions, it runs your pipeline automatically when you push code — no server to manage. You define the pipeline in a YAML file, connect your GitHub repo, and CircleCI handles the rest.

### CircleCI vs GitHub Actions

| | GitHub Actions | CircleCI |
|---|---|---|
| Config location | `.github/workflows/*.yml` | `.circleci/config.yml` |
| Trigger | Built into GitHub | Connects to GitHub via OAuth |
| Free tier | 2,000 minutes/month | 6,000 credits/month |
| Reusable steps | Actions (marketplace) | Orbs (CircleCI registry) |
| Parallelism | Matrix builds | Parallel jobs + test splitting |
| Setup | Zero — already in GitHub | Requires CircleCI account + project setup |

Both tools are widely used in industry. The concepts are identical — jobs, steps, triggers, secrets. The syntax is different.

---

## Key Concepts

| Concept | What it is |
|---------|-----------|
| **Pipeline** | The full automated process triggered by a push |
| **Workflow** | Defines which jobs run and in what order |
| **Job** | A group of steps that run on the same machine (executor) |
| **Step** | A single task — run a command or use an orb command |
| **Executor** | The environment a job runs in — Docker image, machine, etc. |
| **Orb** | A reusable package of jobs, commands, and executors (like GitHub Actions actions) |
| **Context** | A named set of environment variables shared across projects |
| **Workspace** | Shared storage that passes files between jobs in the same workflow |
| **`persist_to_workspace`** | Save files from one job to the workspace |
| **`attach_workspace`** | Load files saved by a previous job |

---

## CircleCI Config Structure

All CircleCI config lives in a single file:

```
.circleci/
└── config.yml
```

The file has five top-level sections:

```yaml
version: 2.1          # always 2.1

orbs:                 # import reusable packages
  aws-cli: circleci/aws-cli@4.1
  python: circleci/python@2.1

commands:             # reusable step sequences (like functions)
  my-command:
    steps:
      - run: echo "reusable"

jobs:                 # define what each job does
  my-job:
    docker:
      - image: cimg/base:current
    steps:
      - checkout
      - run: echo "hello"

workflows:            # define which jobs run and when
  my-workflow:
    jobs:
      - my-job
```

---

## Setting Up CircleCI

### Step 1 — Create a CircleCI account

1. Go to [https://circleci.com](https://circleci.com)
2. Click **Sign Up** → **Sign up with GitHub**
3. Authorise CircleCI to access your GitHub account

### Step 2 — Connect your project

1. In the CircleCI dashboard, click **Projects**
2. Find `employee-app` in the list
3. Click **Set Up Project**
4. Choose **Fastest** (use the existing `.circleci/config.yml` in the repo)
5. Click **Set Up Project**

CircleCI will immediately trigger a pipeline on the current branch.

### Step 3 — Add environment variables

CircleCI needs AWS credentials and other config to run the pipeline. These are set as environment variables in the project settings.

```
CircleCI dashboard → Projects → employee-app → Project Settings → Environment Variables
```

Add the following:

| Variable | Value | Purpose |
|----------|-------|---------|
| `AWS_ACCESS_KEY_ID` | Your IAM access key | Authenticate to AWS |
| `AWS_SECRET_ACCESS_KEY` | Your IAM secret key | Authenticate to AWS |
| `AWS_REGION` | `us-east-1` | AWS region |
| `ECR_REGISTRY` | `075120018043.dkr.ecr.us-east-1.amazonaws.com` | ECR registry URL |
| `GITHUB_TOKEN` | GitHub personal access token | Push updated tags back to repo |

> **Never** put these values directly in `config.yml` — that file is committed to the repo.

### Step 4 — Create a GitHub personal access token

The pipeline updates `kubernetes/helm/values.yaml` with new image tags and pushes the change back to GitHub. It needs a token with write access to do this.

```
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
→ Generate new token
→ Scopes: check repo (full control of private repositories)
→ Generate token → copy the value
```

Add it to CircleCI as `GITHUB_TOKEN`.

### Step 5 — Give GitHub Actions access to AWS (IAM)

Same IAM setup as GitHub Actions — create a dedicated IAM user for CI:

```
AWS Console → IAM → Users → Create user
Name: circleci-ci
Attach policies:
  - AmazonEC2ContainerRegistryPowerUser
```

Create access keys for this user and add them to CircleCI as `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.

---

## Branching Strategy

The CircleCI pipeline mirrors the GitHub Actions branching strategy exactly:

```
feature/* / fix/* / chore/*  ──▶  test only (no build, no deploy)
develop                       ──▶  test → build → push to ECR → deploy to DEV EC2
main                          ──▶  test → build → push to ECR → deploy to PROD EC2
```

This is controlled in the `workflows` section using `filters`:

```yaml
workflows:
  build-and-deploy:
    jobs:
      - test                        # runs on ALL branches

      - build-and-push:
          requires:
            - test                  # only after test passes
          filters:
            branches:
              only:                 # only on these branches
                - develop
                - main

      - deploy:
          requires:
            - build-and-push
          filters:
            branches:
              only:
                - develop
                - main
```

| Branch | Jobs that run |
|--------|--------------|
| `feature/anything` | `test` only |
| `fix/anything` | `test` only |
| `develop` | `test` → `build-and-push` → `deploy` (dev EC2) |
| `main` | `test` → `build-and-push` → `deploy` (prod EC2) |

---

## The Config File — `.circleci/config.yml`

```yaml
version: 2.1

orbs:
  aws-cli: circleci/aws-cli@4.1
  python: circleci/python@2.1

# ── Reusable commands ──────────────────────────────────────────────────────────
commands:
  ecr-login:
    steps:
      - run:
          name: Authenticate Docker to ECR
          command: |
            aws ecr get-login-password --region $AWS_REGION | \
              docker login --username AWS --password-stdin $ECR_REGISTRY

# ── Jobs ───────────────────────────────────────────────────────────────────────
jobs:

  # ── Job 1: Test ───────────────────────────────────────────────────────────────
  test:
    docker:
      - image: cimg/python:3.11
    steps:
      - checkout
      - python/install-packages:
          pkg-manager: pip
          pip-dependency-file: backend/requirements.txt
      - run:
          name: Run pytest
          command: |
            cd backend
            DATABASE_URL=sqlite:///test.db \
            AWS_REGION=us-east-1 \
            pytest -v

  # ── Job 2: Build & Push ───────────────────────────────────────────────────────
  build-and-push:
    docker:
      - image: cimg/base:current
    steps:
      - checkout
      - setup_remote_docker:
          docker_layer_caching: true
      - aws-cli/setup:
          aws_access_key_id: AWS_ACCESS_KEY_ID
          aws_secret_access_key: AWS_SECRET_ACCESS_KEY
          region: AWS_REGION
      - ecr-login
      - run:
          name: Set image tags
          command: |
            TIMESTAMP=$(date +'%Y%m%d-%H%M%S')
            if [ "$CIRCLE_BRANCH" = "main" ]; then
              echo "export BACKEND_TAG=be-prod-${TIMESTAMP}" >> $BASH_ENV
              echo "export FRONTEND_TAG=fe-prod-${TIMESTAMP}" >> $BASH_ENV
            else
              echo "export BACKEND_TAG=be-dev-${TIMESTAMP}" >> $BASH_ENV
              echo "export FRONTEND_TAG=fe-dev-${TIMESTAMP}" >> $BASH_ENV
            fi
      - run:
          name: Build and push backend
          command: |
            docker build -t $ECR_REGISTRY/employee-backend:$BACKEND_TAG backend/
            docker push $ECR_REGISTRY/employee-backend:$BACKEND_TAG
      - run:
          name: Build and push frontend
          command: |
            docker build -t $ECR_REGISTRY/employee-frontend:$FRONTEND_TAG frontend/
            docker push $ECR_REGISTRY/employee-frontend:$FRONTEND_TAG
      - run:
          name: Save tags for next job
          command: |
            echo $BACKEND_TAG > /tmp/backend_tag
            echo $FRONTEND_TAG > /tmp/frontend_tag
      - persist_to_workspace:
          root: /tmp
          paths:
            - backend_tag
            - frontend_tag

  # ── Job 3: Deploy ─────────────────────────────────────────────────────────────
  deploy:
    docker:
      - image: cimg/base:current
    steps:
      - checkout
      - attach_workspace:
          at: /tmp
      - run:
          name: Load tags
          command: |
            echo "export BACKEND_TAG=$(cat /tmp/backend_tag)" >> $BASH_ENV
            echo "export FRONTEND_TAG=$(cat /tmp/frontend_tag)" >> $BASH_ENV
      - aws-cli/setup:
          aws_access_key_id: AWS_ACCESS_KEY_ID
          aws_secret_access_key: AWS_SECRET_ACCESS_KEY
          region: AWS_REGION
      - add_ssh_keys:
          fingerprints:
            - "$SSH_FINGERPRINT"
      - run:
          name: Deploy to EC2
          command: |
            ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST << EOF
              # Authenticate with ECR
              aws ecr get-login-password --region $AWS_REGION | \
                docker login --username AWS --password-stdin $ECR_REGISTRY

              # Create network if it does not exist
              docker network inspect employee-network >/dev/null 2>&1 || \
                docker network create employee-network

              # Start postgres if not already running
              if ! docker ps --format '{{.Names}}' | grep -q '^db$'; then
                docker rm -f db || true
                docker run -d --name db --restart unless-stopped \
                  --network employee-network \
                  -e POSTGRES_USER=postgres \
                  -e POSTGRES_PASSWORD=postgres \
                  -e POSTGRES_DB=employees \
                  -v postgres-data:/var/lib/postgresql/data \
                  postgres:15
                until docker exec db pg_isready -U postgres; do sleep 2; done
              fi

              # Pull new images
              docker pull $ECR_REGISTRY/employee-backend:$BACKEND_TAG
              docker pull $ECR_REGISTRY/employee-frontend:$FRONTEND_TAG

              # Replace old containers
              docker rm -f backend frontend || true

              docker run -d --name backend --restart unless-stopped \
                --network employee-network \
                -p 5000:5000 \
                -e DATABASE_URL=postgresql://postgres:postgres@db:5432/employees \
                $ECR_REGISTRY/employee-backend:$BACKEND_TAG

              docker run -d --name frontend --restart unless-stopped \
                --network employee-network \
                -p 8080:80 \
                $ECR_REGISTRY/employee-frontend:$FRONTEND_TAG

              # Clean up old images
              docker image prune -f
            EOF

  # ── Job 4: Update Helm values ─────────────────────────────────────────────────
  update-image-tags:
    docker:
      - image: cimg/base:current
    steps:
      - checkout
      - attach_workspace:
          at: /tmp
      - run:
          name: Load tags
          command: |
            echo "export BACKEND_TAG=$(cat /tmp/backend_tag)" >> $BASH_ENV
            echo "export FRONTEND_TAG=$(cat /tmp/frontend_tag)" >> $BASH_ENV
      - run:
          name: Update kubernetes/helm/values.yaml
          command: |
            sed -i "0,/tag: .*/s/tag: .*/tag: ${BACKEND_TAG}/" kubernetes/helm/values.yaml
            sed -i "0,/tag: ${BACKEND_TAG}/!s/tag: .*/tag: ${FRONTEND_TAG}/" kubernetes/helm/values.yaml
      - run:
          name: Commit and push updated tags
          command: |
            git config user.name "circleci"
            git config user.email "circleci@landmark.dev"
            git add kubernetes/helm/values.yaml
            git commit -m "ci: update tags - backend:${BACKEND_TAG} frontend:${FRONTEND_TAG}" || true
            git push https://${GITHUB_TOKEN}@github.com/LandmakTechnology/employee-app.git HEAD:$CIRCLE_BRANCH

# ── Workflows ──────────────────────────────────────────────────────────────────
workflows:
  build-and-deploy:
    jobs:
      - test

      - build-and-push:
          requires:
            - test
          filters:
            branches:
              only:
                - develop
                - main

      - deploy:
          requires:
            - build-and-push
          filters:
            branches:
              only:
                - develop
                - main

      - update-image-tags:
          requires:
            - build-and-push
          filters:
            branches:
              only:
                - main
```

---

## Config Walkthrough

### `version: 2.1`

Always `2.1`. This enables orbs, commands, and other modern features.

---

### Orbs

```yaml
orbs:
  aws-cli: circleci/aws-cli@4.1
  python: circleci/python@2.1
```

Orbs are reusable packages published to the CircleCI registry — equivalent to GitHub Actions' `uses:` actions. They wrap common tasks so you don't have to write them from scratch.

| Orb | What it provides |
|-----|-----------------|
| `circleci/aws-cli` | `aws-cli/setup` command — installs and configures the AWS CLI using env vars |
| `circleci/python` | `python/install-packages` command — installs pip dependencies with caching |

---

### Commands

```yaml
commands:
  ecr-login:
    steps:
      - run:
          name: Authenticate Docker to ECR
          command: |
            aws ecr get-login-password --region $AWS_REGION | \
              docker login --username AWS --password-stdin $ECR_REGISTRY
```

A `command` is a reusable sequence of steps — like a function. Define it once, call it in any job. Here `ecr-login` wraps the ECR authentication so it can be reused without repeating the same code.

---

### Job 1 — `test`

```yaml
test:
  docker:
    - image: cimg/python:3.11
  steps:
    - checkout
    - python/install-packages:
        pkg-manager: pip
        pip-dependency-file: backend/requirements.txt
    - run:
        name: Run pytest
        command: |
          cd backend
          DATABASE_URL=sqlite:///test.db pytest -v
```

- Runs on **every branch** — no filter
- `cimg/python:3.11` — CircleCI's convenience image with Python 3.11 pre-installed
- `checkout` — built-in step that clones the repo
- `python/install-packages` — from the python orb, installs deps with pip caching
- `DATABASE_URL=sqlite:///test.db` — uses SQLite so no real database is needed in CI

---

### Job 2 — `build-and-push`

```yaml
build-and-push:
  docker:
    - image: cimg/base:current
  steps:
    - checkout
    - setup_remote_docker:
        docker_layer_caching: true
    - aws-cli/setup: ...
    - ecr-login
    - run:
        name: Set image tags
        command: |
          if [ "$CIRCLE_BRANCH" = "main" ]; then
            echo "export BACKEND_TAG=be-prod-${TIMESTAMP}" >> $BASH_ENV
          else
            echo "export BACKEND_TAG=be-dev-${TIMESTAMP}" >> $BASH_ENV
          fi
```

Key points:

- `setup_remote_docker` — CircleCI jobs run inside Docker containers. To run Docker commands inside a job, you need a separate remote Docker environment. `docker_layer_caching: true` caches image layers between runs to speed up builds.
- `$CIRCLE_BRANCH` — built-in CircleCI variable with the current branch name. Used to tag images differently for `develop` vs `main`.
- `$BASH_ENV` — CircleCI's way of persisting environment variables between steps. Writing `export VAR=value >> $BASH_ENV` makes `VAR` available in all subsequent steps.
- `persist_to_workspace` — saves the tag files to a shared workspace so the `deploy` and `update-image-tags` jobs can read them.

---

### Job 3 — `deploy`

```yaml
deploy:
  steps:
    - attach_workspace:
        at: /tmp
    - add_ssh_keys:
        fingerprints:
          - "$SSH_FINGERPRINT"
    - run:
        name: Deploy to EC2
        command: |
          ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST << EOF
            ...docker commands...
          EOF
```

- `attach_workspace` — loads the tag files saved by `build-and-push`
- `add_ssh_keys` — injects the SSH private key stored in CircleCI into the job so it can SSH into EC2
- The heredoc (`<< EOF`) runs multiple commands on the remote EC2 server in a single SSH session

---

### Job 4 — `update-image-tags`

Only runs on `main`. Updates `kubernetes/helm/values.yaml` with the new image tags and pushes the commit back to GitHub. This is what triggers ArgoCD to deploy the new version to EKS.

```yaml
- run:
    name: Commit and push updated tags
    command: |
      git push https://${GITHUB_TOKEN}@github.com/LandmakTechnology/employee-app.git HEAD:$CIRCLE_BRANCH
```

`$CIRCLE_BRANCH` ensures the push goes back to the branch that triggered the pipeline — `main` in this case.

---

### Workflow

```yaml
workflows:
  build-and-deploy:
    jobs:
      - test                          # every branch

      - build-and-push:
          requires: [test]            # only after test passes
          filters:
            branches:
              only: [develop, main]   # only on these two branches

      - deploy:
          requires: [build-and-push]
          filters:
            branches:
              only: [develop, main]

      - update-image-tags:
          requires: [build-and-push]
          filters:
            branches:
              only: [main]            # only on main (EKS deploy)
```

The `requires` field creates a dependency chain — a job won't start until all its required jobs have passed. Combined with `filters`, this gives you full control over what runs where.

---

## Setting Up SSH for EC2 Deployment

CircleCI deploys to EC2 via SSH. You need to add your SSH key to CircleCI and the EC2 instance.

### Step 1 — Generate an SSH key pair

```bash
ssh-keygen -t rsa -b 4096 -f circleci-deploy-key -N ""
# Creates: circleci-deploy-key (private) and circleci-deploy-key.pub (public)
```

### Step 2 — Add the public key to EC2

```bash
# On your EC2 instance
cat circleci-deploy-key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### Step 3 — Add the private key to CircleCI

```
CircleCI dashboard → Projects → employee-app → Project Settings
→ SSH Keys → Additional SSH Keys → Add SSH Key
→ Hostname: <your EC2 IP>
→ Private Key: paste the contents of circleci-deploy-key
→ Add SSH Key
```

CircleCI will show you the **fingerprint** of the key. Copy it.

### Step 4 — Add environment variables for the deploy job

```
CircleCI → Project Settings → Environment Variables
```

| Variable | Value |
|----------|-------|
| `EC2_HOST` | Your EC2 public IP |
| `EC2_USER` | `ec2-user` |
| `SSH_FINGERPRINT` | The fingerprint from Step 3 |

---

## Environment Variables Reference

All variables are set in:
```
CircleCI → Projects → employee-app → Project Settings → Environment Variables
```

| Variable | Value | Used in |
|----------|-------|---------|
| `AWS_ACCESS_KEY_ID` | IAM access key | `build-and-push`, `deploy` |
| `AWS_SECRET_ACCESS_KEY` | IAM secret key | `build-and-push`, `deploy` |
| `AWS_REGION` | `us-east-1` | All jobs |
| `ECR_REGISTRY` | `075120018043.dkr.ecr.us-east-1.amazonaws.com` | `build-and-push`, `deploy` |
| `GITHUB_TOKEN` | GitHub personal access token | `update-image-tags` |
| `EC2_HOST` | EC2 public IP | `deploy` |
| `EC2_USER` | `ec2-user` | `deploy` |
| `SSH_FINGERPRINT` | SSH key fingerprint from CircleCI | `deploy` |

---

## Built-in CircleCI Variables

CircleCI injects these automatically — no setup needed:

| Variable | Value |
|----------|-------|
| `$CIRCLE_BRANCH` | Current branch name (`develop`, `main`, `feature/x`) |
| `$CIRCLE_SHA1` | Full git commit SHA |
| `$CIRCLE_BUILD_NUM` | Pipeline build number |
| `$CIRCLE_PROJECT_REPONAME` | Repository name |
| `$CIRCLE_USERNAME` | GitHub username that triggered the build |
| `$BASH_ENV` | Path to file used to persist env vars between steps |

---

## Pipeline Flow — End to End

```
Developer pushes to feature/add-search
        │
        ▼
CircleCI triggers pipeline
  └── test job runs (pytest)
        ├── FAIL → red ✗, developer notified, pipeline stops
        └── PASS → green ✓
              └── build-and-push? NO — feature branch filtered out
              └── deploy? NO — feature branch filtered out

Developer merges to develop
        │
        ▼
CircleCI triggers pipeline
  └── test → PASS
        └── build-and-push (develop branch passes filter)
              ├── docker build backend → be-dev-20240101-120000
              ├── docker push to ECR
              ├── docker build frontend → fe-dev-20240101-120000
              ├── docker push to ECR
              └── persist tags to workspace
        └── deploy (develop branch passes filter)
              ├── attach workspace (load tags)
              ├── SSH into DEV EC2
              ├── docker pull new images
              ├── docker rm old containers
              └── docker run new containers

Team merges develop to main
        │
        ▼
CircleCI triggers pipeline
  └── test → PASS
        └── build-and-push (main branch passes filter)
              ├── docker build backend → be-prod-20240101-130000
              ├── docker push to ECR
              ├── docker build frontend → fe-prod-20240101-130000
              └── persist tags to workspace
        └── deploy (main branch passes filter)
              ├── SSH into PROD EC2
              └── docker run new containers
        └── update-image-tags (main only)
              ├── update kubernetes/helm/values.yaml
              └── git push → triggers ArgoCD → EKS deploy
```

---

## Switching Between Branches

To test the pipeline on a feature branch:

```bash
# Create and push a feature branch
git checkout -b feature/my-change
git push origin feature/my-change
# → only test job runs
```

To trigger a dev deploy:

```bash
git checkout develop
git merge feature/my-change
git push origin develop
# → test → build-and-push → deploy to dev EC2
```

To trigger a prod deploy:

```bash
git checkout main
git merge develop
git push origin main
# → test → build-and-push → deploy to prod EC2 → update helm values
```

---

## Viewing Pipeline Results

```
CircleCI dashboard → Pipelines
```

Every pipeline run shows:
- Which branch triggered it
- Which jobs ran and their status (pass/fail)
- Duration of each job
- Full logs for every step

Click any failed step to see the exact error output.

To re-run a failed pipeline:
```
CircleCI → Pipelines → select the run → Rerun → Rerun from failed
```

---

## Contexts — Sharing Variables Across Projects

If you have multiple projects that all need the same AWS credentials, use a **Context** instead of setting variables on each project individually.

```
CircleCI → Organisation Settings → Contexts → Create Context
Name: aws-credentials
Add: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION, ECR_REGISTRY
```

Reference it in the workflow:

```yaml
workflows:
  build-and-deploy:
    jobs:
      - build-and-push:
          context: aws-credentials   # inject all variables from this context
```

---

## Summary

| Concept | CircleCI | GitHub Actions equivalent |
|---------|----------|--------------------------|
| Config file | `.circleci/config.yml` | `.github/workflows/*.yml` |
| Reusable package | Orb | Action (`uses:`) |
| Reusable steps | `commands:` | Composite action |
| Pass data between jobs | `persist_to_workspace` / `attach_workspace` | `outputs` + `needs` |
| Secrets | Project environment variables | Repository secrets |
| Shared secrets | Contexts | Organisation secrets |
| Branch filter | `filters.branches.only` | `on.push.branches` |
| Job dependency | `requires` | `needs` |
| Current branch | `$CIRCLE_BRANCH` | `${{ github.ref_name }}` |
| Docker in job | `setup_remote_docker` | Available by default |

---

## What Comes Next — Kubernetes

The same images pushed to ECR by this pipeline are what Kubernetes pulls when deploying to EKS. The `update-image-tags` job updates `kubernetes/helm/values.yaml` with the new tag, which ArgoCD detects and uses to roll out the new version automatically.

```
CircleCI pipeline (now):
  test → build → push to ECR → SSH deploy to EC2

With Kubernetes (next):
  test → build → push to ECR → update values.yaml → ArgoCD detects change → helm upgrade on EKS
```

The test and build stages stay exactly the same. Only the deploy stage changes.
