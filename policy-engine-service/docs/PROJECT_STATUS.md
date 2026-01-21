# Policy Engine Service - Project Documentation

> **Document Purpose**: This document serves as the single source of truth for the Policy Engine Service project. It captures the vision, implementation status, and future roadmap. Use this document when resuming work or enhancing the service.

---

## 1. What We Wanted (Requirements & Goals)

### 1.1 Problem Statement

We initially used **Open Policy Agent (OPA)** for authorization, but faced several challenges:

| Challenge | Description |
|-----------|-------------|
| **Rego Learning Curve** | OPA's policy language (Rego) requires specialized knowledge |
| **No Admin UI** | Super Admin couldn't manage policies without editing code |
| **File-Based Policies** | Policies stored as files, not in database with versioning |
| **External Dependency** | OPA runs as a separate service, adding complexity |
| **Limited Visibility** | Hard to audit who changed what policy and when |

### 1.2 Goals

1. **Database-Stored Policies**: Store policies in PostgreSQL with full versioning
2. **Super Admin Management**: Allow Super Admin to create/update policies via REST API (future: Admin UI)
3. **Simple API**: Other services call `/evaluate` endpoint for authorization
4. **Familiar Technology**: Use Java/Spring Boot instead of Rego
5. **Audit Trail**: Log every policy change and every authorization decision
6. **Full Control**: Own the codebase, no external policy engine dependency

### 1.3 Authorization Model

We implemented **hybrid RBAC + ABAC**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          AUTHORIZATION FLOW                                  │
│                                                                              │
│  ┌─────────────┐                                                            │
│  │ Other       │  ① Check permission locally (RBAC)                         │
│  │ Services    │     "Does user have loan:approve permission?"              │
│  │             │                                                            │
│  │             │  ② Call Policy Engine for complex checks (ABAC)            │
│  │             │     "Can user approve THIS loan in THIS branch             │
│  │             │      for THIS amount?"                                     │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    POLICY ENGINE SERVICE                             │    │
│  │                                                                      │    │
│  │  POST /api/evaluate                                                  │    │
│  │  {                                                                   │    │
│  │    "subject": { "permissions": [...], "branchIds": [...] },         │    │
│  │    "resource": { "type": "loan", "branchId": "...", "amount": N },  │    │
│  │    "action": "approve"                                               │    │
│  │  }                                                                   │    │
│  │                                                                      │    │
│  │  Response: { "allowed": true/false, "reason": "..." }               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. What We Used (Technology Stack)

### 2.1 Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Language |
| **Spring Boot** | 3.2.0 | Framework |
| **PostgreSQL** | 15+ | Policy storage |
| **Redis** | 7+ | Caching (optional) |
| **Flyway** | 10.10.0 | Database migrations |
| **Lombok** | Latest | Reduce boilerplate |
| **SpringDoc OpenAPI** | 2.3.0 | API documentation |
| **Hypersistence Utils** | 3.7.0 | JSONB support for PostgreSQL |

### 2.2 Project Structure

```
policy-engine-service/
├── pom.xml                                    # Maven configuration
├── README.md                                  # Quick start guide
│
└── src/main/
    ├── java/com/enterprise/policyengine/
    │   │
    │   ├── PolicyEngineApplication.java       # Main entry point
    │   │
    │   ├── config/
    │   │   └── GlobalExceptionHandler.java    # Error handling
    │   │
    │   ├── controller/
    │   │   ├── EvaluationController.java      # POST /evaluate
    │   │   └── PolicyController.java          # CRUD /policies
    │   │
    │   ├── dto/
    │   │   ├── EvaluationRequest.java         # Input for evaluation
    │   │   ├── EvaluationResponse.java        # Output with decision
    │   │   ├── PolicyRequest.java             # Create/update policy
    │   │   └── PolicyResponse.java            # Policy details
    │   │
    │   ├── engine/
    │   │   ├── ExpressionResolver.java        # Parse "subject.branchIds"
    │   │   └── PolicyEvaluator.java           # Core rule engine
    │   │
    │   ├── entity/
    │   │   ├── Policy.java                    # Main policy entity
    │   │   ├── PolicyRule.java                # Individual rules
    │   │   ├── PolicyRuleGroup.java           # AND/OR grouping
    │   │   ├── EvaluationAuditLog.java        # Decision logging
    │   │   ├── Operator.java                  # EQUALS, IN, etc.
    │   │   ├── ValueType.java                 # STRING, NUMBER, EXPRESSION
    │   │   ├── PolicyEffect.java              # ALLOW, DENY
    │   │   └── LogicOperator.java             # AND, OR
    │   │
    │   ├── repository/
    │   │   ├── PolicyRepository.java          # Policy queries
    │   │   └── EvaluationAuditLogRepository.java
    │   │
    │   └── service/
    │       ├── PolicyService.java             # Policy CRUD
    │       └── EvaluationService.java         # Evaluation + audit
    │
    └── resources/
        ├── application.yml                    # Configuration
        └── db/migration/
            └── V1__Create_Policy_Tables.sql   # Schema + seed data
```

---

## 3. How We Did It (Implementation Approach)

### 3.1 Database Schema

```sql
-- Core tables
policies              -- Policy definitions (name, resource, action, effect)
policy_rules          -- Conditions (attribute, operator, value)
policy_rule_groups    -- AND/OR grouping of rules
policy_versions       -- Audit trail of policy changes
evaluation_audit_logs -- Log of every authorization decision
```

### 3.2 Rule Engine Design

The `PolicyEvaluator` implements this evaluation flow:

```
1. Find Active Policies
   └── Match by (resource_type, action)
   └── Sort by priority (highest first)

2. For Each Policy:
   └── Group rules by ruleGroup
   └── Evaluate each group (AND within group)
   └── All groups must pass (AND between groups)

3. If Policy Matches:
   └── Return ALLOW or DENY based on policy.effect
   └── First matching policy wins

4. If No Policy Matches:
   └── Default DENY
```

### 3.3 Expression Resolution

The `ExpressionResolver` parses paths like `subject.branchIds`:

```java
// Input: "subject.branchIds"
// Steps:
//   1. Split by "." → ["subject", "branchIds"]
//   2. Get root object → request.getSubject()
//   3. Navigate path → subject.getBranchIds()
// Output: ["branch-1", "branch-2"]
```

### 3.4 Operators Implemented

| Category | Operators |
|----------|-----------|
| **Equality** | EQUALS, NOT_EQUALS |
| **Collection** | IN, NOT_IN, CONTAINS, CONTAINS_ANY |
| **Comparison** | GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL |
| **String** | STARTS_WITH, ENDS_WITH, MATCHES_REGEX |
| **Null** | IS_NULL, IS_NOT_NULL |
| **Boolean** | IS_TRUE, IS_FALSE |

### 3.5 API Design

```yaml
# Evaluation (for other services)
POST /api/evaluate           # Evaluate single request
POST /api/evaluate/dry-run   # Test without audit log
POST /api/evaluate/batch     # Multiple requests at once

# Policy Management (for Super Admin)
GET    /api/policies         # List all
GET    /api/policies/{id}    # Get by ID
POST   /api/policies         # Create
PUT    /api/policies/{id}    # Update
DELETE /api/policies/{id}    # Delete
POST   /api/policies/{id}/activate    # Enable
POST   /api/policies/{id}/deactivate  # Disable
```

---

## 4. What We Achieved (Current Status)

### 4.1 Completed Features ✅

| Feature | Status | Notes |
|---------|--------|-------|
| Project setup | ✅ Done | Spring Boot 3.2, Java 21 |
| Database schema | ✅ Done | Flyway migration with seed data |
| Entity classes | ✅ Done | Policy, PolicyRule, PolicyRuleGroup, etc. |
| DTOs | ✅ Done | Request/Response for API |
| Repositories | ✅ Done | With optimized queries |
| Expression resolver | ✅ Done | Handles nested paths |
| Rule engine | ✅ Done | 16 operators implemented |
| Evaluation API | ✅ Done | Single, batch, dry-run |
| Policy CRUD API | ✅ Done | Full management |
| Audit logging | ✅ Done | Every decision logged |
| Exception handling | ✅ Done | Global handler |
| OpenAPI docs | ✅ Done | Swagger UI available |
| Compilation | ✅ Done | Builds successfully |

### 4.2 Seed Policies Included

```
1. loan_branch_approval
   - Permission: loan:approve required
   - Scope: loan.branchId must be in user's branchIds
   - Limit: loan.amount <= user's approvalLimit

2. loan_branch_view
   - Permission: loan:view required
   - Scope: loan.branchId must be in user's branchIds

3. super_admin_all
   - Permission: *:* grants access to everything
```

### 4.3 Current Limitations

| Limitation | Impact | Workaround |
|------------|--------|------------|
| No Redis caching yet | Policies loaded from DB each time | Low impact for moderate load |
| No Admin UI | Super Admin uses REST API | Use Swagger UI or curl |
| No policy testing | Can't validate before activation | Use /evaluate/dry-run |
| No integration with main app | Need to add Feign client | Document provided |

---

## 5. What Next We Can Do (Future Enhancements)

### 5.1 High Priority (Phase 2)

| Enhancement | Description | Effort |
|-------------|-------------|--------|
| **Redis Caching** | Cache policies in Redis, invalidate on update | 2-3 hours |
| **Policy Client Library** | Feign client for main app integration | 1-2 hours |
| **Integration Testing** | End-to-end tests with Testcontainers | 3-4 hours |
| **Metrics/Monitoring** | Prometheus metrics for evaluation latency | 2 hours |

### 5.2 Medium Priority (Phase 3)

| Enhancement | Description | Effort |
|-------------|-------------|--------|
| **Admin UI** | React/Angular UI for policy management | 2-3 days |
| **Policy Templates** | Pre-built templates for common use cases | 3-4 hours |
| **Policy Import/Export** | JSON export for backup/migration | 2-3 hours |
| **Policy Versioning UI** | View and rollback to previous versions | 4-5 hours |
| **OR Logic Between Groups** | Currently AND only, add OR support | 3 hours |

### 5.3 Low Priority (Phase 4)

| Enhancement | Description | Effort |
|-------------|-------------|--------|
| **Policy Simulation** | "What if" analysis for new policies | 1 day |
| **A/B Policy Testing** | Test new policies on subset of users | 2 days |
| **Machine Learning** | Anomaly detection in authorization patterns | 1 week |
| **Multi-Tenant Support** | Policies scoped per organization | 3-4 hours |
| **GraphQL API** | Alternative to REST for complex queries | 1 day |

### 5.4 Security Enhancements

| Enhancement | Description |
|-------------|-------------|
| Add authentication to Policy Engine API | Currently anyone can call it |
| Rate limiting on /evaluate | Prevent abuse |
| Policy approval workflow | Changes require approval before activation |
| Encryption of sensitive policy data | If policies contain sensitive info |

---

## 6. Quick Reference

### 6.1 Running the Service

```bash
# Prerequisites
docker compose up -d postgres redis

# Create database
docker exec -it postgres psql -U postgres -c "CREATE DATABASE policy_db;"

# Run
cd policy-engine-service
mvn spring-boot:run

# Access Swagger
open http://localhost:8082/api/swagger-ui.html
```

### 6.2 Environment Variables

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

### 6.3 Key Files to Modify

| File | When to Modify |
|------|----------------|
| `PolicyEvaluator.java` | Add new operators or change evaluation logic |
| `ExpressionResolver.java` | Support new attribute paths |
| `V1__Create_Policy_Tables.sql` | Add new seed policies |
| `application.yml` | Configuration changes |

---

## 7. Comparison: OPA vs Policy Engine Service

| Aspect | OPA | Policy Engine Service |
|--------|-----|----------------------|
| Policy Language | Rego (learning curve) | Java (familiar) |
| Policy Storage | File-based | PostgreSQL (versioned) |
| Admin Management | Edit files | REST API / Future UI |
| Performance | Very fast (<1ms) | Fast (~5-10ms) |
| Deployment | Separate container | Part of your stack |
| Audit Trail | Manual | Built-in |
| Flexibility | Rego limitations | Full Java power |
| Maintenance | OPA community | Your team |

---

## 8. Contact & Resources

| Resource | Location |
|----------|----------|
| Source Code | `/home/prabesh/1work/mvp/opa-mvp/policy-engine-service/` |
| README | `policy-engine-service/README.md` |
| API Docs | `http://localhost:8082/api/swagger-ui.html` |
| Database | PostgreSQL `policy_db` |

---

*Last Updated: 2026-01-07*
*Version: 1.0.0*
