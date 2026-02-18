#!/usr/bin/env bash
# ==============================================================================
# build.sh — Build JARs, Docker images, and push to GHCR
# ==============================================================================
# Usage:
#   ./build.sh              # Build everything
#   ./build.sh --push       # Build + push to GHCR
#   ./build.sh --service memo-service --push  # Build only memo-service + push
# ==============================================================================

set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io/growphasetech/business-process}"
TAG="${TAG:-latest}"
PUSH=false
SINGLE_SERVICE=""

# Parse arguments
SELECTED_SERVICES=()
while [[ $# -gt 0 ]]; do
  case $1 in
    --push) PUSH=true; shift ;;
    --service) SELECTED_SERVICES+=("$2"); shift 2 ;;
    --tag) TAG="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# All Java services (all build via cas-parent reactor)
JAVA_SERVICES=(
  cas-server
  admin-gateway
  gateway-product
  memo-service
  workflow-service
  form-service
  document-service
  organization-service
  policy-engine-service
  notification-service
  audit-service
  person-service
)

# Frontend UIs
UI_SERVICES=(
  memo-ui
  cas-admin-ui
)

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# ─── Step 1: Build JARs ───
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Step 1: Building JARs with Maven"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Determine which JAVA services to build
JAVA_TO_BUILD=()
if [[ ${#SELECTED_SERVICES[@]} -eq 0 ]]; then
  JAVA_TO_BUILD=("${JAVA_SERVICES[@]}")
else
  # Filter selected services to only include Java ones
  for svc in "${SELECTED_SERVICES[@]}"; do
    if [[ " ${JAVA_SERVICES[*]} " =~ " ${svc} " ]]; then
      JAVA_TO_BUILD+=("$svc")
    fi
  done
fi

if [[ ${#JAVA_TO_BUILD[@]} -gt 0 ]]; then
    echo "→ Building Java modules: ${JAVA_TO_BUILD[*]}"
    # Construct comma-separated list for -pl
    PL_ARG=""
    for svc in "${JAVA_TO_BUILD[@]}"; do
        PL_ARG="${PL_ARG},../${svc}"
    done
    # Remove leading comma
    PL_ARG="${PL_ARG:1}"
    
    # Always include cas-common if we are building specific services
    mvn clean package -DskipTests -f cas-parent/pom.xml -pl "../cas-common,${PL_ARG}" -am
    echo "✓ JAR build complete"
else
    echo "→ No Java services to build."
fi


# ─── Step 2: Build Docker images ───
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Step 2: Building Docker images"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

build_image() {
  local service=$1
  local image_name="${REGISTRY}/${service}:${TAG}"
  echo "→ Building ${image_name}..."
  docker build -t "$image_name" "./${service}"
  echo "✓ Built ${image_name}"
}

if [[ ${#SELECTED_SERVICES[@]} -eq 0 ]]; then
  # Build ALL
  for svc in "${JAVA_SERVICES[@]}"; do build_image "$svc"; done
  for svc in "${UI_SERVICES[@]}"; do build_image "$svc"; done
else
  # Build SELECTED
  for svc in "${SELECTED_SERVICES[@]}"; do
    build_image "$svc"
  done
fi

# ─── Step 3: Push to GHCR (optional) ───
if [[ "$PUSH" == true ]]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Step 3: Pushing to ${REGISTRY}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # Load .env and login to GHCR
  if [[ -f "${PROJECT_ROOT}/.env" ]]; then
    source "${PROJECT_ROOT}/.env"
  fi
  # ... (Login logic omitted for brevity, it's fine) ...
  if [[ -n "${GHCR_TOKEN:-}" ]]; then
     echo "→ Logging in to ghcr.io..."
     echo "$GHCR_TOKEN" | docker login ghcr.io -u "${GHCR_USER:-prabesh-suwal}" --password-stdin
  else
     echo "⚠ GHCR_TOKEN not set — assuming already logged in"
  fi

  push_image() {
    local service=$1
    local image_name="${REGISTRY}/${service}:${TAG}"
    echo "→ Pushing ${image_name}..."
    docker push "$image_name"
  }

  if [[ ${#SELECTED_SERVICES[@]} -eq 0 ]]; then
    # Push ALL
    for svc in "${JAVA_SERVICES[@]}"; do push_image "$svc"; done
    for svc in "${UI_SERVICES[@]}"; do push_image "$svc"; done
  else
    # Push SELECTED
    for svc in "${SELECTED_SERVICES[@]}"; do
      push_image "$svc"
    done
  fi

  echo "✓ Pushed images"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Done! Images ready."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
