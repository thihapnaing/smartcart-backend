# syntax=docker/dockerfile:1
#
# Two-stage build for the SmartCart AI (FastAPI) microservice:
#   1. builder - installs Python dependencies into a virtualenv
#   2. runtime - copies the venv + app code into a slim image, keeping
#                compilers and build headers out of the final image
#
# Build:  docker build -t smartcart-ai-service .
# Run:    docker run -p 8001:8001 --env-file .env smartcart-ai-service

# ---------------------------------------------------------------------------
# Stage 1: builder
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS builder
WORKDIR /app

# Needed to build a couple of native wheels (e.g. chromadb/tensorflow deps)
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS runtime
WORKDIR /app

# libgl1/libglib2.0-0 are required by opencv-python at import time.
# apt-get upgrade picks up Debian security patches released after this
# base image was built (e.g. the util-linux/libblkid family - CVE-2026-53615)
# that Trivy's own DB can lag behind reporting as fixed.
RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system app && adduser --system --ingroup app app

COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1

COPY --chown=app:app . .

# ChromaDB persists to ./chroma_db at runtime; mount a volume here so the
# index survives container restarts instead of baking it into the image.
VOLUME ["/app/chroma_db"]

USER app
EXPOSE 8001
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]