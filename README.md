# SmartCart Backend

Spring Boot 4 backend for SmartCart, an e-commerce platform with AI-assisted product
recommendations and chat. It exposes the REST API consumed by the SmartCart web frontend,
persists data to MySQL, and delegates AI features (recommendations, vector search, chat) to
the companion [`smartcart-ai-service`](./smartcart-ai-service) Python microservice.

## Demo

**URL** 
- *Customer/Merchant:* http://localhost:4200/login
- *Admin:* http://localhost:4200/admin

| Role | Email | Password |
|---|---|---|
| Merchant | `merchant@smartcart.demo` | `password123` |
| Customer | `grace@smartcart.demo` | `password123` |
| Admin | `admin@smartcart.demo` | `password123` |

> Demo accounts only, seeded via `data.sql`.

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

# SmartCart AI Service

Python/FastAPI microservice powering SmartCart's AI features: the AI shopping assistant chat
(LangGraph + MCP tool-calling over OpenAI), product recommendations, image search (CNN), and
vector search. Called internally by the [`smartcart-backend`](../) Spring Boot API — it is never
exposed directly to the browser.

## Features

- **Chat assistant** — agentic tool-calling loop over OpenAI, with tools exposed via MCP (`services/agent_service.py`, `services/workflow.py`, `smartcart_mcp_server.py`)
- **Recommendations** — product recommendations (`services/recommendation_service.py`, `routers/recommendation_router.py`)
- **Image search** — CNN-based visual product search (`services/cnn_service.py`, `routers/image_search.py`)
- **Vector search** — ChromaDB-backed product catalog search (`services/vector_store.py`)
- **Trends & promotions** — supporting endpoints (`routers/trend_router.py`, `routers/promotions_router.py`)

## Tech Stack

- FastAPI, Uvicorn
- LangGraph, LangChain, OpenAI SDK
- MCP (`mcp`, `langchain-mcp-adapters`) — tool server over stdio
- ChromaDB — vector database
- TensorFlow, OpenCV, scikit-learn — CNN image search
- pytest — testing

## Project Structure

```
routers/                  # FastAPI route handlers (chat, image_search, recommendation, trend, promotions)
services/                 # Business logic (agent_service, workflow, cnn_service, recommendation_service, smartcart_tools, vector_store)
smartcart_mcp_server.py   # MCP tool server (search_products, get_order_history, get_spending_summary, get_cart)
prompts/                  # System prompt(s) for the chat agent
tests/                    # pytest unit tests
main.py                   # FastAPI app entrypoint
```

## Prerequisites

- Python 3.10+
- A running [`smartcart-backend`](../) instance (default `http://localhost:8080`) — the chat/recommendation tools call back into it via `/internal/tools/**`

## Configuration

### 1. Get an API key

This service needs an LLM API key to answer chat/recommendation requests. Use one of:

- **OpenAI** — go to [platform.openai.com](https://platform.openai.com) → API keys → "Create new secret key." Requires a payment method on file (not covered by a ChatGPT Plus subscription).
- **OpenRouter** (alternative, has free-tier models) — go to [openrouter.ai](https://openrouter.ai) and create a key.
- **TAVILY_API_KEY** (alternative, has free-tier models) — go to [Travily](https://www.tavily.com/) and create a key.

### 2. Set environment variables

Copy `.env.example` to `.env` (already gitignored — never commit real keys):

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `OPENAI_API_KEY` | Direct OpenAI key. Set this **or** `OPENROUTER_API_KEY`, not both. |
| `OPENROUTER_API_KEY` | OpenRouter key, used instead of OpenAI — `services/agent_service.py` prefers this if both are set. |
| `SMARTCART_BACKEND_URL` | Base URL of the Spring Boot backend (default `http://localhost:8080`). |
| `PORT` | Port this FastAPI service listens on (default `8001`). |

## Running Locally

```powershell
python -m venv .venv
.venv\Scripts\activate          # Windows; use `source .venv/bin/activate` on macOS/Linux
pip install -r requirements.txt
python main.py
```

Runs on `http://localhost:8001` with auto-reload. Verify with `GET /api/health` →
`{"status": "ok", "service": "smartcart-ai-service"}`.

**Note (Windows):** `requirements.txt` includes `pywin32` (required by the MCP stdio transport)
and `pip-system-certs` (fixes `SSL: CERTIFICATE_VERIFY_FAILED` errors caused by antivirus HTTPS
inspection, e.g. Norton) — both install automatically with the command above.

## Testing

```bash
pytest
```

## Related Repositories

- [`smartcart-backend`](../) — Spring Boot REST API (this service's caller)
- `smartcart-web` — Angular frontend

