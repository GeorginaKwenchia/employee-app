# Jenkins CI/CD

## What is Jenkins?

Jenkins is an open-source automation server that builds, tests, and deploys your application automatically. Every time you push code to GitHub, Jenkins picks it up and runs your pipeline — or tells you it broke before it ever reaches production.

It is self-hosted — you run it on your own EC2 instance. Full control, no per-minute billing, no vendor lock-in.

The pipeline for this app does:

```
Clone → Test (pytest) → Build Docker images → Push to DockerHub → Deploy to EC2
```

---

## Part 1 — Launch the Jenkins EC2 Instance

| Setting | Value |
|---------|-------|
| AMI | Amazon Linux 2023 |
| Instance type | `t3.medium` (2 vCPU, 4 GB RAM) |
| Storage | 20 GB gp3 |
| Security group | 22 (SSH), 8080 (Jenkins UI) |

---

## Part 2 — Install Jenkins

SSH into the instance and run:

```bash
# Java 17 — required by Jenkins
sudo dnf install -y java-17-amazon-corretto-headless

# Add the Jenkins repo
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo

sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key

# Install and start
sudo dnf install -y jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Verify
sudo systemctl status jenkins
```

---

## Part 3 — Install Docker and Python on the Jenkins Server

Jenkins needs Docker to build images and Python/pip to run the backend tests.

```bash
# Docker
sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker

# Allow Jenkins to run Docker without sudo
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# Python + pip (for pytest)
sudo dnf install -y python3 python3-pip
```

---

## Part 4 — Access Jenkins

Open your browser:

```
http://<JENKINS_EC2_PUBLIC_IP>:8080
```

### Unlock Jenkins

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Paste the password into the browser.

### Setup Wizard

1. Click **Install suggested plugins** and wait
2. Create your admin user (username, password, email)
3. Set Jenkins URL to `http://<JENKINS_EC2_PUBLIC_IP>:8080`
4. Click **Save and Finish → Start using Jenkins**

---

## Part 5 — Install Plugins

Go to **Manage Jenkins → Plugins → Available plugins** and install:

### Core / Pipeline

| Plugin | Why |
|--------|-----|
| **Pipeline** | Runs `Jenkinsfile` pipelines (usually pre-installed) |
| **Pipeline: Stage View** | Shows stages as a visual table in classic UI |
| **Git** | Clones your GitHub repo |
| **GitHub Integration** | Webhook trigger from GitHub |
| **SSH Agent** | SSH into the deploy EC2 using a private key |
| **Docker Pipeline** | Docker commands inside pipelines |
| **Credentials Binding** | `withCredentials` block in pipelines |

### Blue Ocean

| Plugin | Why |
|--------|-----|
| **Blue Ocean** | Modern pipeline UI — installs all Blue Ocean plugins in one go |

After installing Blue Ocean, access it at:
```
http://<JENKINS_EC2_PUBLIC_IP>:8080/blue
```
Blue Ocean shows each pipeline run as a visual stage-by-stage flow with colour-coded pass/fail and live log streaming per stage.

### Useful Extras

| Plugin | Why |
|--------|-----|
| **Timestamper** | Adds timestamps to every console log line |
| **AnsiColor** | Renders colour in console output |
| **Build Timeout** | Kills a hung build after N minutes |
| **Workspace Cleanup** | Wipes the workspace before each build |
| **Email Extension** | Send rich HTML email notifications on failure |
| **Slack Notification** | Post build results to a Slack channel |

Search each one, tick it, click **Install**. Restart Jenkins when prompted.

---

## Part 6 — Add Credentials

Go to **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

### 1. DockerHub

| Field | Value |
|-------|-------|
| Kind | Username with password |
| Username | your DockerHub username |
| Password | your DockerHub access token |
| ID | `dockerhub-credentials` |

> Generate a token at: DockerHub → Account Settings → Personal access tokens → New token

### 2. EC2 SSH Key

| Field | Value |
|-------|-------|
| Kind | SSH Username with private key |
| ID | `ec2-ssh-key` |
| Username | `ec2-user` |
| Private Key | paste the full contents of your `.pem` file |

### 3. EC2 Host (deploy server IP)

| Field | Value |
|-------|-------|
| Kind | Secret text |
| Secret | your deploy EC2 public IP |
| ID | `ec2-host` |

### 4. Database URL

| Field | Value |
|-------|-------|
| Kind | Secret text |
| Secret | `postgresql://postgres:postgres@db:5432/employees` |
| ID | `database-url` |

---

## Part 7 — Prepare the Deploy EC2 Instance

The EC2 instance that runs the app needs Docker installed.

```bash
# On the deploy EC2
sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
newgrp docker
```

Make sure the Jenkins server can SSH into this instance using the key you added in Part 6.

---

## Part 8 — Update the Jenkinsfile

Open `Jenkinsfile` in the repo and replace the placeholder with your actual DockerHub username:

```groovy
DOCKERHUB_BACKEND  = 'chafah/employee-backend'
DOCKERHUB_FRONTEND = 'chafah/employee-frontend'
```

---

## Part 9 — Jenkins Job Types

Jenkins has three main job types. Each one is created from **New Item** on the home page.

---

### Job Type 1 — Freestyle Job

The simplest job type. You configure everything through the UI — no script file needed. Good for one-off tasks, running a shell script, or teaching what Jenkins does before introducing pipelines.

**Create it:**

1. Click **New Item**
2. Name: `employee-app-freestyle`
3. Select **Freestyle project** → **OK**

**Configure:**

- **Source Code Management** → Git
  - Repository URL: `https://github.com/LandmakTechnology/employee-app.git`
  - Branch: `*/main`
- **Build Steps** → Add build step → **Execute shell**

```bash
pip install -r backend/requirements.txt
cd backend
DATABASE_URL=sqlite:///test.db pytest -v
```

Click **Save** → **Build Now**.

**What it teaches:** Jenkins basics — source control integration, build steps, console output, build history. No Groovy, no Jenkinsfile.

---

### Job Type 2 — Multibranch Pipeline

Automatically discovers every branch and pull request in your repo that has a `Jenkinsfile`. Each branch gets its own pipeline. When a branch is deleted, its pipeline disappears too.

This is the most production-like setup — `main` runs the full deploy pipeline, `develop` runs tests only, feature branches run tests on every push.

**Create it:**

1. Click **New Item**
2. Name: `employee-app-multibranch`
3. Select **Multibranch Pipeline** → **OK**

**Configure:**

- **Branch Sources** → Add source → **Git**
  - Project Repository: `https://github.com/LandmakTechnology/employee-app.git`
- **Build Configuration**
  - Mode: by Jenkinsfile
  - Script Path: `Jenkinsfile`
- **Scan Multibranch Pipeline Triggers** → tick **Periodically if not otherwise run** → interval: `1 minute`

Click **Save**. Jenkins immediately scans the repo and creates a pipeline for every branch that has a `Jenkinsfile`.

**What it teaches:** Branch-based CI/CD — different branches can have different pipeline behaviour, pull requests get their own build, branch cleanup is automatic.

---

### Job Type 3 — Pipeline (from SCM)

A single pipeline tied to one branch, driven by the `Jenkinsfile` in the repo. This is what the `employee-app` pipeline uses.

**Create it:**

1. Click **New Item**
2. Name: `employee-app`
3. Select **Pipeline** → **OK**

**Configure:**

Scroll to the **Pipeline** section:

| Field | Value |
|-------|-------|
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `https://github.com/LandmakTechnology/employee-app.git` |
| Branch | `*/main` |
| Script Path | `Jenkinsfile` |

Click **Save** → **Build Now**.

**What it teaches:** Pipeline as code — the `Jenkinsfile` lives in the repo, versioned alongside the application code.

---

### Comparison

| | Freestyle | Pipeline | Multibranch Pipeline |
|---|---|---|---|
| Config location | Jenkins UI | `Jenkinsfile` in repo | `Jenkinsfile` in repo |
| Multi-branch support | No | No (one branch) | Yes (all branches) |
| Pipeline as code | No | Yes | Yes |
| Best for | Simple tasks / demos | Single-branch CI/CD | Full team workflow |

---

### Run It

Click **Build Now** on any job. Click the build number → **Console Output** to watch it live.
Or open Blue Ocean at `http://<JENKINS_EC2_PUBLIC_IP>:8080/blue` for the visual view.

---

## Part 10 — Auto-trigger on Push (Webhook)

### On Jenkins

1. Pipeline → **Configure**
2. Under **Build Triggers** → tick **GitHub hook trigger for GITScm polling**
3. Save

### On GitHub

1. Repo → **Settings → Webhooks → Add webhook**
2. Payload URL: `http://<JENKINS_EC2_PUBLIC_IP>:8080/github-webhook/`
3. Content type: `application/json`
4. Trigger: **Just the push event**
5. Click **Add webhook**

Every push to `main` now triggers the pipeline automatically.

---

## Pipeline Flow

```
git push to main
       │
       ▼
┌──────────┐    ┌─────────────────────┐    ┌──────────────────────────┐
│   Test   │───▶│   Build & Push      │───▶│        Deploy            │
│          │    │                     │    │                          │
│ pip      │    │ docker build        │    │ SSH into EC2             │
│ install  │    │ backend/ → DockerHub│    │ docker pull              │
│ pytest   │    │ frontend/→ DockerHub│    │ docker run backend :5000 │
│          │    │                     │    │ docker run frontend :80  │
└──────────┘    └─────────────────────┘    └──────────────────────────┘
```

Access the app at `http://<DEPLOY_EC2_PUBLIC_IP>`

---

## Troubleshooting

**`docker: permission denied`**
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

**`pip: command not found`**
```bash
sudo dnf install -y python3-pip
```

**`Host key verification failed`**
The `-o StrictHostKeyChecking=no` flag in the Jenkinsfile handles this automatically.

**`Cannot connect to Docker daemon`**
```bash
sudo systemctl start docker
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

**`invalid reference format`** (DockerHub push fails)
Make sure `DOCKERHUB_BACKEND` and `DOCKERHUB_FRONTEND` use lowercase only — DockerHub repo names must be lowercase.
