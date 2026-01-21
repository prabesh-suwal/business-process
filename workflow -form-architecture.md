Deep Dive: Workflow & Form Architecture
1. External Integration Architecture
Since you'll have multiple external integrations (core banking, credit bureaus, DMS), we need a robust pattern:

┌─────────────────────────────────────────────────────────────────────────────────┐
│                        INTEGRATION LAYER ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  workflow-service                                                                │
│       │                                                                          │
│       ▼                                                                          │
│  ┌─────────────────┐     ┌─────────────────────────────────────────────┐        │
│  │ Service Task    │────►│  integration-service (NEW MICROSERVICE)     │        │
│  │ (Flowable)      │     │                                             │        │
│  └─────────────────┘     │  ┌───────────────────────────────────────┐ │        │
│                          │  │  IntegrationRegistry                  │ │        │
│                          │  │  - Connector configurations           │ │        │
│                          │  │  - Credential vault (encrypted)       │ │        │
│                          │  │  - Retry policies                     │ │        │
│                          │  └───────────────────────────────────────┘ │        │
│                          │                                             │        │
│                          │  ┌───────────────────────────────────────┐ │        │
│                          │  │  Connectors                           │ │        │
│                          │  │  ├── CoreBankingConnector             │ │        │
│                          │  │  ├── CreditBureauConnector            │ │        │
│                          │  │  ├── DocumentServiceConnector         │ │        │
│                          │  │  ├── EmailConnector                   │ │        │
│                          │  │  ├── SMSConnector                     │ │        │
│                          │  │  └── WebhookConnector (generic)       │ │        │
│                          │  └───────────────────────────────────────┘ │        │
│                          └─────────────────────────────────────────────┘        │
│                                         │                                        │
│                          ┌──────────────┼──────────────┐                        │
│                          ▼              ▼              ▼                        │
│                    ┌──────────┐  ┌──────────┐  ┌──────────────┐                 │
│                    │ Core     │  │ Credit   │  │ Document     │                 │
│                    │ Banking  │  │ Bureau   │  │ Management   │                 │
│                    │ (T24/    │  │ (Experian│  │ System       │                 │
│                    │ Finacle) │  │ /etc.)   │  │              │                 │
│                    └──────────┘  └──────────┘  └──────────────┘                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
Key Design Decisions:

Aspect	Decision	Reason
Separate integration-service	Yes	Isolates external dependencies, easier credential management, circuit breaker patterns
Async processing	Yes, with callbacks	External systems can be slow; don't block workflow
Retry & Circuit Breaker	Resilience4j	Handle external failures gracefully
Response mapping	Configurable transformers	Different CBS have different response formats
2. Complete Variable History Tracking
Since you need full variable history, here's how we'll implement it:

┌─────────────────────────────────────────────────────────────────────────────────┐
│                        AUDIT & HISTORY TABLES                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Flowable Native:                     Custom Extensions:                         │
│  ├── ACT_HI_PROCINST (process hist)   ├── wf_variable_audit                     │
│  ├── ACT_HI_ACTINST  (activity hist)  │   ├── id                                │
│  ├── ACT_HI_TASKINST (task history)   │   ├── process_instance_id               │
│  ├── ACT_HI_VARINST  (var snapshot)   │   ├── variable_name                     │
│  └── ACT_HI_COMMENT  (comments)       │   ├── old_value (JSONB)                 │
│                                        │   ├── new_value (JSONB)                 │
│                                        │   ├── changed_by (user_id)              │
│                                        │   ├── changed_at (timestamp)            │
│                                        │   ├── task_id (nullable)                │
│                                        │   └── change_reason                     │
│                                        │                                         │
│                                        ├── wf_action_timeline                    │
│                                        │   ├── id                                │
│                                        │   ├── process_instance_id               │
│                                        │   ├── action_type (TASK_COMPLETE,       │
│                                        │   │               VARIABLE_UPDATE,      │
│                                        │   │               FORM_SUBMIT, etc.)    │
│                                        │   ├── actor_id                          │
│                                        │   ├── actor_name                        │
│                                        │   ├── actor_role                        │
│                                        │   ├── metadata (JSONB)                  │
│                                        │   ├── ip_address                        │
│                                        │   └── timestamp                         │
│                                        │                                         │
│                                        └── wf_document_audit                     │
│                                            ├── document_id                       │
│                                            ├── version                           │
│                                            ├── action (UPLOAD, VIEW, DOWNLOAD)   │
│                                            └── actor_id                          │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
Implementation Approach:

Use Flowable's Event Listeners to capture all state changes
Custom VariableAuditListener intercepts all setVariable() calls
Every task completion captures form snapshot + actor context
3. Form Service Deep Dive
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           FORM SERVICE ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  form-service                                                                    │
│  ├── FormDefinition                                                              │
│  │   ├── id, name, version                                                       │
│  │   ├── product_id                                                              │
│  │   ├── process_template_id (nullable - can be standalone)                      │
│  │   ├── task_key (nullable - ties to specific task in BPMN)                     │
│  │   ├── schema (JSONB) ────────────────────────────────────┐                    │
│  │   └── ui_schema (JSONB) - layout/styling hints           │                    │
│  │                                                           │                    │
│  │   Example Schema:                                         │                    │
│  │   {                                                       │                    │
│  │     "type": "object",                                     │                    │
│  │     "properties": {                                       │                    │
│  │       "loanAmount": {                                     │                    │
│  │         "type": "number",                                 │                    │
│  │         "title": "Loan Amount",                           │                    │
│  │         "minimum": 10000,                                 │                    │
│  │         "maximum": 10000000                               │                    │
│  │       },                                                  │                    │
│  │       "loanPurpose": {                                    │                    │
│  │         "type": "string",                                 │                    │
│  │         "enum": ["HOME", "VEHICLE", "EDUCATION"]          │                    │
│  │       },                                                  │                    │
│  │       "applicantName": {                                  │                    │
│  │         "type": "string",                                 │                    │
│  │         "minLength": 2                                    │                    │
│  │       }                                                   │                    │
│  │     },                                                    │                    │
│  │     "required": ["loanAmount", "applicantName"]           │                    │
│  │   }                                                       │                    │
│  │                                                           │                    │
│  ├── FieldDefinition                                         │                    │
│  │   ├── Field Types:                                        │                    │
│  │   │   ├── TEXT, NUMBER, DATE, DATETIME                    │                    │
│  │   │   ├── DROPDOWN (static options)                       │                    │
│  │   │   ├── DROPDOWN_DYNAMIC (API-fetched options)          │                    │
│  │   │   ├── MULTI_SELECT                                    │                    │
│  │   │   ├── FILE_UPLOAD                                     │                    │
│  │   │   ├── SIGNATURE                                       │                    │
│  │   │   ├── RICH_TEXT                                       │                    │
│  │   │   ├── TABLE (repeatable rows)                         │                    │
│  │   │   ├── CALCULATED (formula-based)                      │                    │
│  │   │   └── REFERENCE (lookup from another entity)          │                    │
│  │   │                                                       │                    │
│  │   ├── Visibility Rules:                                   │                    │
│  │   │   "showIf": { "loanAmount": { "gt": 500000 } }        │                    │
│  │   │                                                       │                    │
│  │   └── Validation Rules:                                   │                    │
│  │       "validation": {                                     │                    │
│  │         "custom": "age >= 18 && age <= 65",               │                    │
│  │         "asyncValidator": "/api/validate/pan-number"      │                    │
│  │       }                                                   │                    │
│  │                                                                               │
│  └── FormSubmission                                                              │
│      ├── id, form_definition_id, process_instance_id, task_id                    │
│      ├── data (JSONB) - submitted values                                         │
│      ├── submitted_by, submitted_at                                              │
│      └── validation_status                                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
Form Builder UI Components:

Frontend Form Builder (React)
├── FieldPalette (drag-and-drop field types)
├── FormCanvas (drop zone, reorder fields)
├── FieldConfigurator (properties panel)
│   ├── Basic Settings (label, placeholder, help text)
│   ├── Validation Tab
│   ├── Visibility Rules Tab
│   └── Advanced (CSS class, custom attributes)
├── FormPreview (live preview)
└── SchemaViewer (JSON schema output)
4. LMS-Specific: Loan Workflow Patterns
Here's a comprehensive Home Loan workflow as an example:

┌─────────────────────────────────────────────────────────────────────────────────┐
│                     HOME LOAN WORKFLOW - BPMN STRUCTURE                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────┐      ┌─────────────┐      ┌───────────────────────────────┐        │
│  │  START  │─────►│ Application │─────►│    Document Collection        │        │
│  │ (Form 1)│      │  Review     │      │    (Multi-instance subtask)   │        │
│  └─────────┘      │  (Form 2)   │      │    ├── ID Proof               │        │
│                   └──────┬──────┘      │    ├── Income Proof           │        │
│                          │             │    ├── Property Docs           │        │
│                          │             │    └── Bank Statements         │        │
│                          │             └───────────────┬────────────────┘        │
│                          │                             │                         │
│                          │         ┌───────────────────┴────────────────┐        │
│                          │         ▼                                     │        │
│                          │  ┌─────────────────────────────────────────┐ │        │
│                          │  │         PARALLEL GATEWAY                │ │        │
│                          │  └────┬────────────────┬───────────────────┘ │        │
│                          │       │                │                     │        │
│                          │       ▼                ▼                     │        │
│                          │ ┌───────────┐   ┌───────────────┐           │        │
│                          │ │ Credit    │   │ Property      │           │        │
│                          │ │ Check     │   │ Valuation     │           │        │
│                          │ │(Service)  │   │ (User Task)   │           │        │
│                          │ └─────┬─────┘   └───────┬───────┘           │        │
│                          │       │                 │                    │        │
│                          │       └────────┬────────┘                    │        │
│                          │                ▼                             │        │
│                          │  ┌─────────────────────────────────────────┐ │        │
│                          │  │          JOIN GATEWAY                   │ │        │
│                          │  └────────────────┬────────────────────────┘ │        │
│                          │                   ▼                          │        │
│                          │        ┌──────────────────────┐              │        │
│                          │        │   AMOUNT-BASED       │              │        │
│                          │        │   EXCLUSIVE GATEWAY  │              │        │
│                          │        └────┬─────────┬───────┘              │        │
│                          │             │         │                      │        │
│                    < 10L │             │         │ >= 10L               │        │
│                          │             ▼         ▼                      │        │
│                          │      ┌──────────┐ ┌──────────────┐           │        │
│                          │      │ Branch   │ │ Regional     │           │        │
│                          │      │ Manager  │ │ Committee    │           │        │
│                          │      │ Approval │ │ Approval     │           │        │
│                          │      └────┬─────┘ │ (Multi-user) │           │        │
│                          │           │       └──────┬───────┘           │        │
│                          │           └──────────────┤                   │        │
│                          │                          ▼                   │        │
│                          │  ┌────────────────────────────────────┐      │        │
│                          │  │       APPROVAL DECISION GATEWAY    │      │        │
│                          │  └────┬─────────────────────┬─────────┘      │        │
│                          │       │ APPROVED            │ REJECTED       │        │
│                          │       ▼                     ▼                │        │
│                          │ ┌───────────┐        ┌────────────┐          │        │
│                          │ │ Disburse  │        │ Rejection  │          │        │
│                          │ │ (Service) │        │ Notification│         │        │
│                          │ └─────┬─────┘        └──────┬─────┘          │        │
│                          │       │                     │                │        │
│                          │       └──────────┬──────────┘                │        │
│                          │                  ▼                           │        │
│                          │            ┌──────────┐                      │        │
│                          │            │   END    │                      │        │
│                          │            └──────────┘                      │        │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
Key Flowable Features Used:

Feature	Use Case
User Task	Human approvals, form filling
Service Task	Credit check API, disbursement API
Parallel Gateway	Credit check + Valuation simultaneously
Exclusive Gateway	Amount-based routing
Multi-instance	Multiple document uploads, committee voting
Boundary Timer Event	SLA escalation (if not approved in 3 days)
Error Boundary Event	Handle credit check failures
Signal/Message Events	External system callbacks
5. Product-Workflow Mapping in Database
sql
-- Maps Products (from CAS) to their available Process Templates
CREATE TABLE process_template (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,              -- FK to CAS products table
    name VARCHAR(255) NOT NULL,
    description TEXT,
    flowable_process_def_key VARCHAR(255), -- Key in Flowable
    flowable_deployment_id VARCHAR(255),
    version INT DEFAULT 1,
    status VARCHAR(50),                    -- DRAFT, ACTIVE, DEPRECATED
    created_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    UNIQUE(product_id, name, version)
);
-- Associates Forms with specific tasks in a Process Template
CREATE TABLE process_template_form_mapping (
    id UUID PRIMARY KEY,
    process_template_id UUID NOT NULL,
    task_key VARCHAR(255),                 -- BPMN task definition key
    form_definition_id UUID NOT NULL,
    form_type VARCHAR(50),                 -- START_FORM, TASK_FORM
    
    FOREIGN KEY (process_template_id) REFERENCES process_template(id),
    FOREIGN KEY (form_definition_id) REFERENCES form_definition(id)
);
6. Security & Role Integration
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DYNAMIC ROLE INTEGRATION                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Task Assignment Expression Examples:                                            │
│                                                                                  │
│  1. Direct Role Assignment:                                                      │
│     candidateGroups="${roleService.getUsersByRole('LOAN_OFFICER', productId)}"   │
│                                                                                  │
│  2. Hierarchy-Based (Submitter's Manager):                                       │
│     assignee="${orgService.getManager(submitterId)}"                             │
│                                                                                  │
│  3. Branch-Scoped Role:                                                          │
│     candidateUsers="${roleService.getUsersByRoleAndBranch(                       │
│                         'BRANCH_MANAGER', submitterBranchId)}"                   │
│                                                                                  │
│  4. Amount-Based Escalation (via Execution Listener):                            │
│     if (loanAmount > 1000000) {                                                  │
│       assignToRole('REGIONAL_MANAGER');                                          │
│     } else {                                                                     │
│       assignToRole('BRANCH_MANAGER');                                            │
│     }                                                                            │
│                                                                                  │
│  5. Pool/Claim Model:                                                            │
│     candidateGroups="CREDIT_ANALYSTS"    // Any analyst can claim               │
│                                                                                  │
│  Integration with CAS:                                                           │
│  ┌─────────────────────┐     ┌─────────────────────┐                            │
│  │  Flowable Task      │────►│  CAS User/Role API  │                            │
│  │  Assignment         │     │                     │                            │
│  │                     │◄────│  Returns user list  │                            │
│  └─────────────────────┘     │  with permissions   │                            │
│                              └─────────────────────┘                            │
│                                                                                  │
│  Integration with Policy Engine:                                                 │
│  - Before task completion: Check if user has permission                          │
│  - Before process start: Check if user can initiate this process type           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
📊 Complete Microservices Overview
Service	Port	Database	Key Responsibilities
cas-server	9000	cas_db	Auth, Users, Roles, Products
policy-engine-service	9001	policy_db	ABAC/RBAC policies
organization-service	8081	organization_db	Branches, Departments, Hierarchy
workflow-service	9002	workflow_db	Flowable engine, Process definitions, Tasks
form-service	9003	form_db	Form definitions, Submissions, Validation
integration-service	9004	integration_db	External connectors, CBS, Credit Bureau
admin-gateway	8080	-	Routes to CAS, Workflow Admin
lms-gateway	8082	-	Routes to LMS services
