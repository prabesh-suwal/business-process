#!/usr/bin/env bash
# ==============================================================================
# deploy.sh — Server-side deployment script
# ==============================================================================
# Usage:
#   ./deploy.sh memo            # Deploy memo product
#   ./deploy.sh admin           # Deploy admin product
#   ./deploy.sh all             # Deploy both products
#   ./deploy.sh memo --pull     # Pull latest images + deploy
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PRODUCT="${1:-all}"
PULL=false

# Check for --pull flag
for arg in "$@"; do
  [[ "$arg" == "--pull" ]] && PULL=true
done

# Load .env
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  source "${SCRIPT_DIR}/.env"
fi

# GHCR login (if token is set)
if [[ -n "${GHCR_TOKEN:-}" ]]; then
  echo "→ Logging into GHCR..."
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "${GHCR_USER:-prabesh-suwal}" --password-stdin
fi

BASE="-f docker-compose.base.yml"

case "$PRODUCT" in
  memo)
    COMPOSE_FILES="$BASE -f docker-compose.memo.yml"
    ;;
  admin)
    COMPOSE_FILES="$BASE -f docker-compose.admin.yml"
    ;;
  all)
    COMPOSE_FILES="$BASE -f docker-compose.memo.yml -f docker-compose.admin.yml"
    ;;
  *)
    echo "Usage: $0 {memo|admin|all} [--pull]"
    exit 1
    ;;
esac

cd "$SCRIPT_DIR"

if [[ "$PULL" == true ]]; then
  echo "→ Pulling latest images..."
  docker compose $COMPOSE_FILES pull
fi

echo "→ Starting services ($PRODUCT)..."
docker compose $COMPOSE_FILES up -d

echo ""
echo "→ Service status:"
docker compose $COMPOSE_FILES ps

echo ""
echo "✓ Deployment complete ($PRODUCT)"
