#!/usr/bin/env bash
set -e
echo "Building frontend..."
cd frontend && npm ci && npm run build && cd ..

echo "Building backend..."
cd backend && mvn clean package -q -DskipTests && cd ..

echo "Building Docker image..."
docker compose build friday-backend

echo "Done. Run: docker compose up -d"
