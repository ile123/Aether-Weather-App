#!/bin/bash
set -e
echo "================================================"
echo "  Weather App - Minikube Deploy Script"
echo "================================================"
echo ""
echo "[1/6] Resetting minikube..."
minikube delete --purge || true
minikube start --driver=docker
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
docker build --no-cache -t api-gateway:latest -f api-gateway/Dockerfile .
echo "api-gateway built!"
docker build --no-cache -t weather-service:latest -f weather-service/Dockerfile .
echo "weather-service built!"
docker build --no-cache -t alert-service:latest -f alert-service/Dockerfile .
echo "alert-service built!"
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
echo ""
echo "================================================"
echo "  Services:"
echo "================================================"
kubectl get services -n weather-app
echo ""
echo "Starting port-forwards..."
kubectl port-forward -n weather-app svc/api-gateway 8081:8080 &
kubectl port-forward -n weather-app svc/kafka-ui 8090:8080 &
kubectl port-forward -n weather-app svc/alert-service 8083:8083 &
kubectl port-forward -n weather-app svc/zipkin 9411:9411 &
sleep 3
echo ""
echo "================================================"
echo "  Ready! Endpoints:"
echo "  API Gateway: http://localhost:8081"
echo "  Kafka UI: http://localhost:8090"
echo "  Alert Service: http://localhost:8083"
echo "  Zipkin UI: http://localhost:9411"
echo "  WARNING: Sometimes the port fowarding might die so please just copy the port-foward commands from above and manually run them"
echo "================================================"
echo ""
echo "Press Ctrl+C to stop port-forwarding"
wait