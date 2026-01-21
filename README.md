I am think of creating a very very dynamic process management system. It includes loan management system, memo management system, document management system and other products in future.
SO for this I am thinking Cas admins will configure roles, permissions, users, products, workflow, forms and their configuations. 
So in workflow they will select product eg: loan and then creates workflow eg: Home loan. THere they will create their required workflow for homeloan for their bank. then they will create different forms for different steps and then they will configure forms, steps and workflows along with validations, triggers, events assignees, access etc....

Now then,  In LMS the LMS admin will create a loan product, configure required properties for that loan and then map with the correct workflow.

Now when Bank user need to create a home loan, then the workflow starts. If the very 1st step of workflow is KYC then he will see kyc page that is configured, then it will go to next workflow step and so on.

We have to make it real life banking solution. Is this good? Are we on right track? IS our system going on right direction?

Note: Roles are dynamic not hardcoded, persmissions are assigned to role, there are policies for users. SO no static. Above mentioned users are not static they will be dnamic and based on roles, permissions and policy acccess.

# Central Authentication System (CAS)

A custom OAuth2/OIDC-compliant Central Authentication System for multi-product SSO.

## Architecture

```
                   ┌────────────────────────┐
                   │   CAS Server (9000)    │
                   │                        │
                   │  - Login & Sessions    │
                   │  - Token issuing       │
                   │  - Roles & permissions │
                   │  - API client mgmt     │
                   └──────────┬─────────────┘
                              │
                      OAuth2 / OIDC
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ LMS Gateway     │  │ WFM Gateway     │  │ Future Product  │
│ (8081)          │  │ (8082)          │  │    Gateway      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.1**
- **Spring Cloud 2025.1.x (Oakwood)**
- **PostgreSQL** - Primary database
- **Redis** - Session storage
- **HashiCorp Consul** - Service discovery

## Modules

| Module | Description | Port |
|--------|-------------|------|
| `cas-parent` | Parent POM with dependency management | - |
| `cas-common` | Shared DTOs, security utilities, exceptions | - |
| `cas-server` | Core CAS authentication server | 9000 |
| `lms-gateway` | LMS API Gateway with scope enforcement | 8081 |
| `wfm-gateway` | WFM API Gateway with scope enforcement | 8082 |

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379
- Consul on port 8500

### 2. Build All Modules

```bash
cd cas-parent
mvn clean install -DskipTests
```

### 3. Run CAS Server

```bash
cd cas-server
mvn spring-boot:run
```

### 4. Run Gateways

```bash
# Terminal 2
cd lms-gateway
mvn spring-boot:run

# Terminal 3
cd wfm-gateway
mvn spring-boot:run
```

## API Endpoints

### CAS Server (port 9000)

| Endpoint | Description |
|----------|-------------|
| `POST /auth/login` | User login |
| `POST /auth/logout` | User logout |
| `POST /oauth/token` | Token endpoint (client_credentials, refresh_token) |
| `POST /oauth/revoke` | Token revocation |
| `GET /.well-known/jwks.json` | JWK Set for token verification |
| `GET /.well-known/openid-configuration` | OIDC discovery |

### Admin API (port 9000)

| Endpoint | Description |
|----------|-------------|
| `GET/POST/PUT/DELETE /admin/users` | User management |
| `GET/POST/PUT/DELETE /admin/roles` | Role management |
| `GET /admin/products` | Product listing |
| `GET /admin/permissions` | Permission catalog |
| `GET/POST/PUT/DELETE /admin/clients` | API client management |

## Authentication Flows

### User Login

```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "productCode": "LMS"
  }'
```

### Client Credentials

```bash
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=my-client&client_secret=secret&product_code=LMS"
```

### Refresh Token

```bash
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=<token>&product_code=LMS"
```

## Token Structure

### User Token
```json
{
  "jti": "uuid",
  "sub": "user-id",
  "type": "USER",
  "email": "user@example.com",
  "products": {
    "LMS": {
      "roles": ["LOAN_OFFICER"],
      "scopes": ["loan.read", "loan.create"]
    }
  },
  "aud": "lms-api",
  "exp": 1700000000
}
```

### API Client Token
```json
{
  "jti": "uuid",
  "sub": "client-id",
  "type": "SERVICE",
  "client_name": "Partner API",
  "scopes": ["loan.read"],
  "aud": "lms-api",
  "exp": 1700000000
}
```

## Gateway Scope Enforcement

Gateways enforce scopes based on `scope-mappings.yml`:

```yaml
mappings:
  - pattern: "GET /api/loans"
    scopes:
      - loan.read
  - pattern: "POST /api/loans/*/approve"
    scopes:
      - loan.approve
```

## Default Credentials

| User | Password | Description |
|------|----------|-------------|
| admin | admin123 | System administrator |

## Development

### Generate RSA Keys (Production)

```bash
# Generate private key
openssl genrsa -out private.pem 2048

# Generate public key
openssl rsa -in private.pem -pubout -out public.pem

# Convert to PKCS8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private-pkcs8.pem
```

Configure in `application.yml`:
```yaml
cas:
  jwt:
    private-key-path: classpath:keys/private.pem
    public-key-path: classpath:keys/public.pem
```

## License

Proprietary - All rights reserved.


workflow-service: Flowable's EventRegistry requires Kafka dependencies that aren't present. I need to exclude the problematic auto-configurations.

Next Phase:
Phase 3: Dynamic Forms - Render Form.io forms in task completion modal

Want me to continue with Phase 3?


Add top-up/renewal service methods (calculate available amount, validate, start)
Continue to Phase 3 (Maker-Checker, SLA runtime)
Add Person Search to NewApplication form?
Add Timeline/History view with workflow events?