# ArgoCD

GitOps continuous delivery for the Employee Directory app and monitoring stack.

## Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=available deployment/argocd-server -n argocd --timeout=120s

# Get the initial admin password
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath='{.data.password}' | base64 --decode

# Port-forward to access the UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Open https://localhost:8080  (admin / <password above>)
```

## Deploy the applications

```bash
# Deploy the employee app (syncs from kubernetes/helm)
kubectl apply -f employee-app.yaml

# Deploy the monitoring stack (Prometheus + Grafana)
kubectl apply -f monitoring.yaml
```

## Check sync status

```bash
# List all ArgoCD applications
kubectl get applications -n argocd

# Describe an application
kubectl describe application employee-app -n argocd
kubectl describe application monitoring -n argocd
```

## Files

| File | What it deploys |
|------|----------------|
| `employee-app.yaml` | Employee app via `kubernetes/helm` — auto-syncs on every git push |
| `monitoring.yaml` | kube-prometheus-stack (Prometheus + Grafana + node-exporter + kube-state-metrics) |

## How it works

```
Git push to main
      │
      ▼
ArgoCD detects change (polls every 3 minutes or via webhook)
      │
      ▼
ArgoCD syncs kubernetes/helm → EKS cluster
      │
      ▼
New pods rolling out in employee-app namespace
```

`automated.selfHeal: true` — if someone manually changes a resource in the cluster,
ArgoCD reverts it back to match the Git state.

`automated.prune: true` — if a resource is removed from Git, ArgoCD deletes it from the cluster.
