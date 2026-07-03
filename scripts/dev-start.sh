#!/usr/bin/env bash
set -euo pipefail

# 本地开发一键启动：拉起 MySQL + Redis，等待就绪后用 mvn 跑后端（dev profile，Flyway 自动迁移）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT/deploy/docker-compose.dev.yml"
POM="$ROOT/server/pom.xml"

echo "==> 启动依赖 (MySQL + Redis) ..."
docker compose -f "$COMPOSE_FILE" up -d

echo "==> 等待 MySQL 就绪 ..."
for i in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Health.Status}}' koala-mysql-dev 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ]; then
    echo "    MySQL 已就绪"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "    MySQL 等待超时，请检查：docker compose -f $COMPOSE_FILE logs mysql" >&2
    exit 1
  fi
  sleep 2
done

echo "==> 启动后端 (dev profile, Flyway 自动迁移) ..."
exec mvn -f "$POM" spring-boot:run
