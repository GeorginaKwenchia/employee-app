# Monitoring — Prometheus + Grafana

Observability stack for the Employee Directory app.

## Components

| Component | Purpose | Port |
|-----------|---------|------|
| Prometheus | Scrapes and stores metrics | 9090 |
| Grafana | Dashboards — Prometheus + CloudWatch | 3000 (NLB :80) |
| node-exporter | Host-level metrics (CPU, memory, disk) per node | 9100 |
| kube-state-metrics | Kubernetes object metrics (pods, deployments) | 8080 |
| ServiceMonitor | Tells Prometheus Operator to scrape backend /metrics | — |

## Apply order

```bash
kubectl apply -f 01-prometheus.yaml
kubectl apply -f 02-grafana.yaml
kubectl apply -f 03-exporters.yaml
kubectl apply -f 04-servicemonitor.yaml   # only if using Prometheus Operator
```

Or apply everything at once:

```bash
kubectl apply -f .
```

## Access

```bash
# Prometheus UI (port-forward)
kubectl port-forward svc/prometheus -n monitoring 9090:9090
# Open http://localhost:9090

# Grafana URL (NLB — wait ~2 min for provisioning)
kubectl get svc grafana -n monitoring
# Use EXTERNAL-IP in browser: http://<EXTERNAL-IP>
# Login: admin / admin123
```

## Metrics flow

```
Flask backend (/metrics)
      │  scraped every 30s
      ▼
Prometheus (stores 7 days in 10Gi EBS)
      │  queried by
      ▼
Grafana dashboards
      │
      ├── Platform: node CPU/memory, pod counts, restarts
      ├── Application: request rate, p95 latency, 5xx errors
      └── CloudWatch: logs-based request rate, error rate, slow endpoints
```

## Dashboards

Dashboard JSON files are in `kubernetes/helm/dashboards/` and are loaded
into Grafana automatically via the `grafana-custom-dashboards` ConfigMap.

| Dashboard | Datasource | Key panels |
|-----------|-----------|------------|
| `platform.json` | Prometheus | Node CPU, memory, pod count, restarts |
| `application.json` | Prometheus | HTTP request rate, p95 latency, 5xx errors |
| `cloudwatch-logs.json` | CloudWatch | Request rate, avg response time, error rate |
