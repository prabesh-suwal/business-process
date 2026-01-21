# Enterprise Central Authentication & Authorization System (CAS)

## Project Vision

A comprehensive, multi-tenant **Central Authentication System (CAS)** that provides:
- **OAuth 2.0 / OIDC authentication** for all enterprise products
- **Product-scoped roles and permissions** (RBAC)
- **Attribute-based access control** (ABAC) via custom Policy Engine
- **Single sign-on (SSO)** across multiple product gateways

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND LAYER                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│  cas-admin-ui (React)  │  LMS UI  │  WFM UI  │  Other Product UIs              │
└────────────┬────────────┴─────┬────┴─────┬────┴─────────────────────────────────┘
             │                  │          │
             ▼                  ▼          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              GATEWAY LAYER                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│  admin-gateway    │   lms-gateway    │   wfm-gateway                            │
│  (Spring Cloud)   │   (Spring Cloud) │   (Spring Cloud)                         │
│  - JWT Validation │   - JWT Validation│  - JWT Validation                       │
│  - Routes → CAS   │   - Routes → LMS │   - Routes → WFM                         │
└────────────┬──────┴────────┬─────────┴────────┬─────────────────────────────────┘
             │               │                  │
             ▼               ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            MICROSERVICES LAYER                                   │
├──────────────────┬───────────────────┬────────────────────┬─────────────────────┤
│   cas-server     │ policy-engine     │ organization-svc   │  LMS/WFM Services   │
│                  │                   │                    │                     │
│ • User Auth      │ • Policy CRUD     │ • Branches         │  • Domain Logic     │
│ • Token Issue    │ • Evaluation      │ • Departments      │  • Business Rules   │
│ • Role/Product   │ • Audit Logging   │ • GeoLocations     │                     │
│ • Client Auth    │                   │ • OrgGroups        │                     │
└────────────┬─────┴────────┬──────────┴─────────┬──────────┴─────────────────────┘
             │              │                    │
             ▼              ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           DATABASE LAYER                                         │
├──────────────────┬───────────────────┬────────────────────┬─────────────────────┤
│    cas_db        │    policy_db      │   organization_db  │  product_dbs        │
│   (PostgreSQL)   │   (PostgreSQL)    │   (PostgreSQL)     │  (PostgreSQL)       │
└──────────────────┴───────────────────┴────────────────────┴─────────────────────┘
```

---

## Module Descriptions

### 1. cas-server (Central Authentication Server)
**Port:** 9000  
**Purpose:** OAuth 2.0 / OIDC provider, user management, token issuance

**Key Features:**
- User registration & authentication
- OAuth 2.0 flows (Authorization Code + PKCE, Client Credentials)
- JWT access/refresh token generation with RS256
- Product-scoped role management
- API Client (service-to-service) authentication
- JWKS endpoint for public key distribution

### 2. cas-common (Shared Library)
**Purpose:** Shared DTOs, utilities, and policy enforcement components

**Key Components:**
- `PolicyEvaluationRequest` / `PolicyEvaluationResponse` - Shared DTOs
- `PolicyClient` - HTTP client for policy evaluation
- `PolicyInterceptor` - Spring interceptor for automatic policy checks
- `RequiresPolicy` - Annotation for declarative authorization
- `TokenClaims` - JWT claim representation

### 3. policy-engine-service (Custom Policy Engine)
**Port:** 9001  
**Purpose:** ABAC/RBAC policy evaluation engine

**Key Features:**
- Policy CRUD with versioning
- Rule-based evaluation with multiple operators
- Product-scoped policy matching
- Temporal conditions (time-based access)
- Audit logging of all decisions

### 4. organization-service
**Port:** 8081  
**Purpose:** Organizational structure management

**Key Features:**
- Branch management
- Department hierarchy
- Geographic locations (Region → Zone → District → Municipality)
- Organizational groups

### 5. admin-gateway / lms-gateway / wfm-gateway
**Ports:** 8080, 8082, 8083  
**Purpose:** API gateways with JWT validation

**Key Features:**
- JWKS-based JWT verification
- Route to backend services
- Pass user context via headers (X-User-Id, X-Product-Claims)

### 6. cas-admin-ui (React Frontend)
**Port:** 5173  
**Purpose:** Admin dashboard for CAS management

---

## Entity Relationships

### CAS Server Entities

```
User ─────────────┬────────────── UserRole ──────────────── Role
                  │                   │                      │
                  │                   │ (product-scoped)     │
                  │                   ▼                      ▼
                  │              ┌─────────┐           Permission
                  │              │ Product │                │
                  │              └─────────┘                │
                  │                   ▲                     │
                  └───────────────────┼─────────────────────┘
                                      │
                                 ApiClient
```

| Entity | Description |
|:-------|:------------|
| **User** | System users with email, password (bcrypt), profile |
| **Role** | Product-scoped roles (e.g., ADMIN for LMS, USER for WFM) |
| **Permission** | Fine-grained permissions assigned to roles |
| **Product** | Registered products (LMS, WFM, CAS-ADMIN) |
| **ApiClient** | Service accounts for machine-to-machine auth |
| **UserRole** | Join table with ABAC constraints (branchIds, approvalLimit) |

### Policy Engine Entities

```
Policy ─────────────────────────────── PolicyRule
   │                                       │
   │ (many-to-many)                       │
   ▼                                       ▼
Products                              PolicyRuleGroup
                                           │
                                           ▼
                                    TemporalCondition
```

| Entity | Description |
|:-------|:------------|
| **Policy** | Authorization policy with effect (ALLOW/DENY), priority |
| **PolicyRule** | Individual condition: `attribute OPERATOR value` |
| **PolicyRuleGroup** | Logical grouping of rules (AND within, OR between) |
| **TemporalCondition** | Time-based restrictions (active hours, days) |

### Organization Entities

```
GeoLocation (Region → Zone → District → Municipality)
      │
      ▼
  Branch ──────────────── Department
      │
      ▼
  OrgGroup ─────────────── GroupMember
```

---

## Authentication Flows

### 1. User Login (Authorization Code + PKCE)
```
Frontend → /oauth/authorize → Login Page → /oauth/token → JWT
```

### 2. Service-to-Service (Client Credentials)
```
Service → /oauth/token (client_id, client_secret) → JWT
```

### 3. Token Structure
```json
{
  "sub": "user-uuid",
  "type": "USER",
  "email": "user@example.com",
  "products": {
    "LMS": { "roles": ["MANAGER"], "scopes": ["read", "write"] },
    "WFM": { "roles": ["USER"], "scopes": ["read"] }
  },
  "aud": "lms-api",
  "iss": "http://localhost:9000",
  "exp": 1736912345
}
```

---

## Authorization Flow

### Request → Policy Evaluation
```
1. Request hits Gateway
2. Gateway validates JWT, extracts claims
3. Gateway adds headers: X-User-Id, X-Product-Claims
4. Request forwarded to microservice
5. PolicyInterceptor intercepts request
   ├── Detects product (header/annotation/service-name)
   ├── Extracts product-specific roles from X-Product-Claims
   ├── Builds PolicyEvaluationRequest
   └── Calls policy-engine-service
6. PolicyEvaluator:
   ├── Finds matching policies (resource, action, product)
   ├── Evaluates rules (subject.roles CONTAINS 'MANAGER')
   └── Returns ALLOW/DENY
7. If ALLOW → proceed; if DENY → 403 Forbidden
```

### Policy Matching SQL
```sql
SELECT * FROM policies WHERE
  is_active = true
  AND (resource_type = :resource OR resource_type = '*')
  AND (action = :action OR action = '*')
  AND (:product IN products OR '*' IN products)
ORDER BY priority DESC
```

---

## Key Design Decisions

| Decision | Rationale |
|:---------|:----------|
| **Product-scoped roles** | User can be ADMIN in LMS but USER in WFM |
| **Centralized policy engine** | Single source of truth for authorization |
| **Gateway JWT validation** | Offload auth from microservices |
| **Header-based context** | X-Product-Claims passes roles to services |
| **Priority-based policies** | Higher priority policies override lower |
| **Wildcard matching** | `*` for resource/action/product for global policies |

---

## Database Schema

### CAS Database (cas_db)
- users, roles, permissions, products
- user_roles (with ABAC constraints)
- api_clients, refresh_tokens, revoked_tokens
- audit_logs

### Policy Database (policy_db)
- policies, policy_rules, policy_rule_groups
- policy_products (many-to-many)
- temporal_conditions
- evaluation_audit_logs

### Organization Database (organization_db)
- branches, departments
- geo_locations, geo_hierarchy_types
- org_groups, group_members

---

## Configuration

### Environment Variables
```properties
# CAS Server
cas.issuer=http://localhost:9000
cas.jwt.access-token-expiry=3600
cas.jwt.refresh-token-expiry=2592000

# Policy Engine
policy-engine.url=http://localhost:9001
policy-engine.evaluation.audit-enabled=true

# Gateways
cas.jwks-url=http://localhost:9000/.well-known/jwks.json
```

---

## API Endpoints Summary

### CAS Server
| Endpoint | Method | Description |
|:---------|:-------|:------------|
| `/oauth/authorize` | GET | Start authorization flow |
| `/oauth/token` | POST | Exchange code/credentials for tokens |
| `/.well-known/jwks.json` | GET | Public keys for JWT verification |
| `/api/users` | CRUD | User management |
| `/api/roles` | CRUD | Role management |
| `/api/products` | CRUD | Product management |

### Policy Engine
| Endpoint | Method | Description |
|:---------|:-------|:------------|
| `/evaluate` | POST | Evaluate authorization request |
| `/evaluate/dry-run` | POST | Test evaluation (no audit) |
| `/api/policies` | CRUD | Policy management |

---

## Running the System

```bash
# 1. Start databases
docker-compose up -d

# 2. Start CAS Server
cd cas-server && mvn spring-boot:run

# 3. Start Policy Engine
cd policy-engine-service && mvn spring-boot:run

# 4. Start Organization Service
cd organization-service && mvn spring-boot:run

# 5. Start Gateway
cd admin-gateway && mvn spring-boot:run

# 6. Start Admin UI
cd cas-admin-ui && npm run dev
```

---

## Future Roadmap

- [ ] Refresh token rotation
- [ ] Multi-factor authentication
- [ ] OAuth 2.0 device flow
- [ ] Policy caching with Redis
- [ ] Real-time policy updates via WebSocket
- [ ] Audit dashboard
