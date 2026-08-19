# SmartCart Backend

Spring Boot 4 backend for SmartCart, an e-commerce platform with AI-assisted product
recommendations and chat. It exposes the REST API consumed by the SmartCart web frontend,
persists data to MySQL, and delegates AI features (recommendations, vector search, chat) to
the companion [`smartcart-ai-service`](./smartcart-ai-service) Python microservice.

## Features

- **Auth** — JWT-based authentication and authorization (`AuthController`, `security/`)
- **Catalog** — products, categories, merchant profiles (`ProductController`, `CategoryController`, `MerchantProfileController`)
- **Cart & Orders** — shopping cart and order management (`CartController`, `OrderController`)
- **Recommendations & vector search** — proxies to the AI service for product recommendations and semantic product search (`RecommendationController`, `ProductVectorController`)
- **Chat** — AI shopping assistant chat (`chat/`)
- **Admin** — dashboard, account, merchant, and product administration (`admin/`)
- **User profiles, home content, public stats, image upload** — supporting APIs (`UserProfileController`, `HomeContentController`, `PublicStatsController`, Cloudinary integration)

## Tech Stack

- Java 17, Spring Boot 4.1 (Web MVC, WebFlux client, Data JPA, Validation, Security)
- MySQL 8 (H2 for tests)
- JWT (`jjwt`), Cloudinary (image hosting)
- Maven, JaCoCo (coverage), SonarCloud
- Docker / Kubernetes (`k8s/`), Terraform for AWS EKS (`terraform/`)

## Project Structure

```
src/main/java/nus/iss/smartcart/backend/
├── admin/          # Admin dashboard, accounts, merchants, products
├── chat/           # AI chat assistant
├── config/         # Spring configuration (beans, CORS, etc.)
├── controller/     # Core REST controllers (auth, cart, orders, catalog, ...)
├── dto/            # Request/response DTOs
├── exception/      # Global exception handling
├── model/          # JPA entities
├── repository/     # Spring Data repositories
├── security/       # JWT filter/service, Spring Security config
├── service/        # Business logic
└── tools/          # Data/tool endpoints used by the AI service

smartcart-ai-service/   # Python microservice: recommendations, vector search, chat (see its own docs)
k8s/                    # Kubernetes manifests for backend, ai-service, frontend, MySQL, ingress
terraform/              # AWS infra (EKS, ECR, VPC) — see terraform/README.md
```

## Prerequisites

- Java 17+
- Maven (or use the bundled `./mvnw`)
- MySQL 8 (or Docker, see below)
- Python 3 environment for `smartcart-ai-service` if you want AI features locally (see its own setup)

## Configuration

Copy `.env.example` to `.env` and adjust as needed — `docker compose` loads it automatically:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | MySQL credentials/database |
| `JWT_SECRET` | Signing key for JWT auth — generate your own for real deployments |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Cloudinary credentials for image upload |
| `AI_PYTHON_SERVICE_BASE_URL` | Base URL of `smartcart-ai-service` (defaults to `http://localhost:8001`) |

These map to properties in `src/main/resources/application.properties`.

## Running Locally

### Option 1: Docker Compose (recommended)

Brings up MySQL, the AI service, and the backend together:

```bash
docker compose up --build
```

Backend will be available at `http://localhost:8080`.

### Option 2: Run directly with Maven

Start a local MySQL instance matching the settings in `application.properties` (or override via
env vars), then:

```bash
./mvnw spring-boot:run
```

## Testing

```bash
./mvnw test
```

Coverage is collected with JaCoCo (`target/site/jacoco/jacoco.xml`) and reported to SonarCloud
in CI; the build enforces a minimum 85% line coverage.

## Building

```bash
./mvnw clean package
```

Or build the Docker image directly:

```bash
docker build -t smartcart-backend .
docker run -p 8080:8080 --env SPRING_DATASOURCE_PASSWORD=*** smartcart-backend
```

## Deployment

- `k8s/` — Kubernetes manifests for the backend, AI service, frontend, MySQL, and ingress
- `terraform/` — Terraform for the AWS EKS/ECR/VPC infrastructure (see `terraform/README.md`)
- `.github/workflows/backend.yml` — CI pipeline (build, test, SonarCloud, image scan/build)

## License

No license specified.
