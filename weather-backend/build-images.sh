#!/bin/bash
set -e
echo "================================================"
echo "  Weather App - Minikube Deploy Script"
echo "================================================"
echo ""
echo "[1/7] Resetting minikube..."
minikube stop || true
minikube delete || true
minikube start --driver=docker
echo ""
echo "[2/7] Waiting for kube-system pods to be ready..."
kubectl wait --for=condition=Ready pods --all -n kube-system --timeout=120s
echo "kube-system is ready!"
echo ""
echo "[3/7] Switching to minikube Docker daemon..."
eval $(minikube docker-env)
echo "Docker daemon switched to minikube"
echo ""
echo "[4/7] Building images..."
docker build -t api-gateway:latest -f api-gateway/Dockerfile .
echo "api-gateway built!"
docker build -t weather-service:latest -f weather-service/Dockerfile .
echo "weather-service built!"
echo ""
echo "[5/7] Verifying images..."
docker images | grep -E "api-gateway|weather-service"
echo "Images verified!"
echo ""
echo "[6/7] Deploying to Kubernetes..."
kubectl apply -k k8s/overlays/dev
echo ""
echo "Waiting for pods to be ready..."
kubectl wait --for=condition=Ready pods --all -n weather-app --timeout=300s || true
echo ""
echo "[7/7] Configuring Keycloak realm..."
kubectl create configmap keycloak-realm-config \
  --from-file=realm.json=infrastructure/keycloak/realms/weather-dashboard-realm.json \
  -n weather-app \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Realm ConfigMap updated!"
echo ""
echo "Restarting Keycloak to import realm..."
kubectl rollout restart deployment/keycloak -n weather-app
echo ""
echo "Waiting for Keycloak to be ready after restart..."
kubectl rollout status deployment/keycloak -n weather-app --timeout=300s
echo "Keycloak ready!"
echo ""
echo "================================================"
echo "  Deployment complete! Pod status:"
echo "================================================"
kubectl get pods -n weather-app
echo ""
echo "================================================"
echo "  Services:"
echo "================================================"
kubectl get services -n weather-app
echo ""
echo "Starting port-forwards..."
kubectl port-forward -n weather-app svc/api-gateway 8081:8080 &
kubectl port-forward -n weather-app svc/keycloak 8080:8080 &
echo ""
echo "================================================"
echo "  Ready! Endpoints:"
echo "  API Gateway: http://localhost:8081"
echo "  Keycloak:    http://localhost:8080"
echo "================================================"
echo ""
echo "Press Ctrl+C to stop port-forwarding"
wait