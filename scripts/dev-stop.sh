#!/usr/bin/env bash
set -euo pipefail

# 停止本地开发依赖（默认保留数据卷；加 --clean 连数据一起删）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT/deploy/docker-compose.dev.yml"

if [ "${1:-}" = "--clean" ]; then
  echo "==> 停止依赖并删除数据卷 ..."
  docker compose -f "$COMPOSE_FILE" down -v
else
  echo "==> 停止依赖（保留数据卷）..."
  docker compose -f "$COMPOSE_FILE" down
fi
