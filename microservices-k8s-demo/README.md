# Microservices Demo (Java / Spring Boot + Kubernetes)

A 5-service demo application showing a typical microservices architecture,
containerized with Docker and deployed to Kubernetes.

## Services

| Service              | Port | Responsibility                                             |
|-----------------------|------|-------------------------------------------------------------|
| user-service           | 5001 | Create/list/get users                                       |
| product-service        | 5002 | Create/list/get products in the catalog                     |
| cart-service            | 5003 | Manage a per-user shopping cart (calls product-service)     |
| order-service           | 5004 | Place orders (calls user-, cart-, notification-service)     |
| notification-service    | 5005 | Simulates sending order-confirmation notifications           |

Request flow for placing an order:
`client -> order-service -> user-service (validate user)`
`order-service -> cart-service (fetch cart, then clear it)`
`order-service -> notification-service (send confirmation)`

## Tech stack / dependencies

- **Java 17**
- **Spring Boot 3.3.4** (`spring-boot-starter-web`, `spring-boot-starter-actuator`)
- **Maven 3.9** for builds
- **Docker** (multi-stage builds: `maven:3.9-eclipse-temurin-17` -> `eclipse-temurin:17-jre-alpine`)
- **Kubernetes** (any cluster: minikube, kind, GKE, EKS, AKS, etc.)
- Data is stored **in-memory** in each service (`ConcurrentHashMap`) purely for
  demo simplicity — swap in a real database (Postgres, MySQL, etc.) for production use.

No external dependency needs to be installed manually beyond Java, Maven, Docker,
and `kubectl` — Maven pulls the Spring Boot libraries automatically at build time.

## Project structure

```
microservices-k8s-demo/
├── docker-compose.yml          # local multi-service run
├── k8s/                        # Kubernetes manifests
│   ├── namespace.yaml
│   ├── user-service.yaml
│   ├── product-service.yaml
│   ├── cart-service.yaml
│   ├── order-service.yaml
│   └── notification-service.yaml
├── user-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/userservice/...
├── product-service/    (same layout)
├── cart-service/       (same layout)
├── order-service/      (same layout)
└── notification-service/ (same layout)
```

## 1. Run locally with Maven (no Docker)

Each service is independent. Open 5 terminals (or run in the background) and start
each one from its folder:

```bash
cd user-service && ./mvnw spring-boot:run          # or: mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd cart-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
```

By default each service talks to the others on `localhost` at the ports listed above
(see each `application.properties`). Requires **JDK 17+** and **Maven 3.9+** installed.

## 2. Run locally with Docker Compose (recommended for local testing)

```bash
docker compose up --build
```

This builds all 5 images and wires them together on a shared Docker network using
service names for inter-service DNS resolution (e.g. `http://product-service:5002`).

Test it:

```bash
curl http://localhost:5001/users
curl http://localhost:5002/products
curl -X POST http://localhost:5003/cart/u1/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"p1","quantity":2}'
curl http://localhost:5003/cart/u1
curl -X POST http://localhost:5004/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"u1"}'
curl http://localhost:5005/notifications
```

## 3. Deploy to Kubernetes

### Step 1 — Build and push images

Each service has its own `Dockerfile`. Build and push all 5 to a registry
(Docker Hub, GHCR, ECR, GCR, etc.):

```bash
for svc in user-service product-service cart-service order-service notification-service; do
  docker build -t YOUR_DOCKERHUB_USERNAME/$svc:1.0.0 ./$svc
  docker push YOUR_DOCKERHUB_USERNAME/$svc:1.0.0
done
```

> If you're using **minikube**, you can skip pushing to a registry by running
> `eval $(minikube docker-env)` before the `docker build` commands, so the images
> are built directly into minikube's local Docker daemon.

### Step 2 — Update image references

Replace `YOUR_DOCKERHUB_USERNAME` in each `k8s/*.yaml` file with your actual
registry/username (or use `sed`):

```bash
sed -i 's/YOUR_DOCKERHUB_USERNAME/<your-username>/g' k8s/*.yaml
```

### Step 3 — Apply the manifests

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/ -n microservices-demo
```

### Step 4 — Verify

```bash
kubectl get pods -n microservices-demo
kubectl get svc -n microservices-demo
```

Wait until all pods show `Running` and `READY 1/1` (the readiness probe hits `/health`).

### Step 5 — Access order-service from outside the cluster

`order-service` is exposed as a `NodePort` so you can reach it externally.

**minikube:**
```bash
minikube service order-service -n microservices-demo --url
```

**kind / bare-metal cluster:**
```bash
kubectl get svc order-service -n microservices-demo
# note the NodePort, then curl http://<node-ip>:<node-port>/orders
```

Internal services (`user-service`, `product-service`, `cart-service`,
`notification-service`) stay as `ClusterIP` and are only reachable from inside the
cluster, via their Kubernetes DNS names (e.g. `http://user-service:5001`), which
is exactly what `order-service` and `cart-service` use internally.

### Step 6 — Clean up

```bash
kubectl delete namespace microservices-demo
```

## Notes / production hardening ideas

This is a learning/demo project. Before using anything like this in production, consider:

- Replace in-memory maps with a real database per service (one DB per service is
  the standard microservices pattern).
- Add an API Gateway (e.g. Spring Cloud Gateway, NGINX Ingress, or Kong) instead of
  exposing `order-service` directly via NodePort.
- Add centralized logging/tracing (ELK, Jaeger/OpenTelemetry) since a single request
  now spans 4 services.
- Add a `ConfigMap`/`Secret` for environment-specific configuration instead of
  hardcoding URLs in the Deployment specs.
- Add `HorizontalPodAutoscaler`s for each Deployment.
- Add resilience (timeouts, retries, circuit breakers — e.g. Resilience4j) around
  the inter-service HTTP calls, since `order-service` currently does a synchronous
  chain of 3 calls.
