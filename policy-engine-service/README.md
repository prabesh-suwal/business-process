# Policy Engine Service

A custom ABAC/RBAC policy engine microservice that replaces OPA with database-stored policies manageable by Super Admin.

## Features

- **Database-Stored Policies**: Policies are stored in PostgreSQL, easily managed via REST API
- **Rule Engine**: Full operator support (EQUALS, IN, LESS_THAN, CONTAINS, etc.)
- **Expression Resolution**: Dynamic paths like `subject.branchIds`, `resource.amount`
- **Audit Logging**: Every evaluation is logged for compliance
- **Super Admin Management**: Create, update, activate/deactivate policies via API

## Quick Start

### 1. Start Dependencies
```bash
# From opa-mvp directory
docker compose up -d postgres redis
```

### 2. Create Policy Database
```bash
docker exec -it postgres psql -U postgres -c "CREATE DATABASE policy_db;"
```

### 3. Run the Service
```bash
cd policy-engine-service
mvn spring-boot:run
```

### 4. Access Swagger UI
Open http://localhost:8082/api/swagger-ui.html

## API Endpoints

### Evaluation (for other services)
```
POST /api/evaluate           - Evaluate authorization
POST /api/evaluate/dry-run   - Test without audit logging
POST /api/evaluate/batch     - Batch evaluation
```

### Policy Management (for Super Admin)
```
GET    /api/policies         - List all policies
GET    /api/policies/{id}    - Get policy by ID
POST   /api/policies         - Create new policy
PUT    /api/policies/{id}    - Update policy
DELETE /api/policies/{id}    - Delete policy
POST   /api/policies/{id}/activate    - Enable policy
POST   /api/policies/{id}/deactivate  - Disable policy
```

## Example: Evaluate Authorization

```bash
curl -X POST http://localhost:8082/api/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "id": "user-123",
      "permissions": ["loan:approve"],
      "branchIds": ["branch-1", "branch-2"],
      "approvalLimit": 500000
    },
    "resource": {
      "type": "loan",
      "id": "loan-456",
      "branchId": "branch-1",
      "amount": 300000
    },
    "action": "approve"
  }'
```

Response:
```json
{
  "allowed": true,
  "effect": "ALLOW",
  "matchedPolicy": "loan_branch_approval",
  "reason": "Access granted",
  "evaluationTimeMs": 5,
  "details": {
    "permission": { "passed": true, "message": "All conditions met" },
    "scope": { "passed": true, "message": "All conditions met" },
    "limit": { "passed": true, "message": "All conditions met" }
  }
}
```

## Example: Create Policy

```bash
curl -X POST http://localhost:8082/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "customer_branch_view",
    "description": "Users can only view customers in their branches",
    "resourceType": "customer",
    "action": "view",
    "effect": "ALLOW",
    "priority": 100,
    "rules": [
      {
        "ruleGroup": "permission",
        "attribute": "subject.permissions",
        "operator": "CONTAINS",
        "valueType": "STRING",
        "value": "customer:view"
      },
      {
        "ruleGroup": "scope",
        "attribute": "resource.branchId",
        "operator": "IN",
        "valueType": "EXPRESSION",
        "value": "subject.branchIds"
      }
    ]
  }'
```

## Supported Operators

| Operator | Description | Example |
|----------|-------------|---------|
| EQUALS | Exact match | `status EQUALS "ACTIVE"` |
| NOT_EQUALS | Not equal | `status NOT_EQUALS "DELETED"` |
| IN | Value in list | `branchId IN subject.branchIds` |
| NOT_IN | Value not in list | `status NOT_IN ["DELETED", "SUSPENDED"]` |
| CONTAINS | List contains value | `permissions CONTAINS "loan:approve"` |
| CONTAINS_ANY | Lists overlap | `roles CONTAINS_ANY ["ADMIN", "MANAGER"]` |
| GREATER_THAN | Numeric comparison | `amount GREATER_THAN 1000` |
| LESS_THAN_OR_EQUAL | Numeric comparison | `amount LESS_THAN_OR_EQUAL subject.approvalLimit` |
| IS_NULL | Check for null | `ownerId IS_NULL` |
| IS_TRUE | Boolean check | `isVerified IS_TRUE` |

## Configuration

Environment variables:
```
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POLICY_DB=policy_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
POLICY_ENGINE_PORT=8082
```

## Integration with Main App

Add to your authorization app's `application.yml`:
```yaml
policy-engine:
  url: http://localhost:8082/api
```

Create a Feign client:
```java
@FeignClient(name = "policy-engine", url = "${policy-engine.url}")
public interface PolicyEngineClient {
    @PostMapping("/evaluate")
    EvaluationResponse evaluate(@RequestBody EvaluationRequest request);
}
```
