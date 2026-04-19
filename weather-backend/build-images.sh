#!/bin/bash
set -e

echo "================================================"
echo "  Weather App - Minikube Deploy Script"
echo "================================================"

echo ""
echo "[1/6] Resetting minikube..."
minikube stop || true
minikube delete || true
minikube start

echo ""
echo "[2/6] Waiting for kube-system pods to be ready..."
kubectl wait --for=condition=Ready pods --all -n kube-system --timeout=120s
echo "kube-system is ready!"

echo ""
echo "[3/6] Switching to minikube Docker daemon..."
eval $(minikube docker-env)
echo "Docker daemon switched to minikube"

echo ""
echo "[4/6] Building images..."
docker build -t api-gateway:latest -f api-gateway/Dockerfile .
echo "api-gateway built!"

docker build -t weather-service:latest -f weather-service/Dockerfile .
echo "weather-service built!"

echo ""
echo "[5/6] Verifying images..."
docker images | grep -E "api-gateway|weather-service"
echo "Images verified!"

echo ""
echo "[6/6] Deploying to Kubernetes..."
kubectl apply -k k8s/overlays/dev

echo ""
echo "Waiting for pods to be ready..."
kubectl wait --for=condition=Ready pods --all -n weather-app --timeout=300s || true

echo ""
echo "================================================"
echo "  Deployment complete! Pod status:"
echo "================================================"
kubectl get pods -n weather-app