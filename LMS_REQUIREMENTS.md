# LMS Implementation Plan & Requirements Document

## Executive Summary

This document captures the complete vision, requirements, architecture, and implementation plan for the Loan Management System (LMS). It includes all discussions, Q&A sessions, design decisions, and implementation details to serve as a reference for future development.

---

## Table of Contents

1. [Vision & Goals](#vision--goals)
2. [Requirements Discussion (Q&A)](#requirements-discussion-qa)
3. [Architecture Overview](#architecture-overview)
4. [Microservices Breakdown](#microservices-breakdown)
5. [Core Features](#core-features)
6. [Data Model](#data-model)
7. [API Design](#api-design)
8. [Implementation Phases](#implementation-phases)
9. [Progress Tracker](#progress-tracker)

---

## Vision & Goals

### What We're Building

A comprehensive **Loan Origination System (LOS)** built on Flowable workflow engine that supports:

- Multiple loan products (Home, Vehicle, Personal, etc.)
- Dynamic forms configured by admin
- Complex approval hierarchies
- Enterprise banking features (Maker-Checker, SLA, Delegation)
- Multi-product support (LMS, Memo, future products)

### Core Principles

1. **Admin Configures, System Executes**: Business users configure workflows/forms without coding
2. **One Workflow Per Product**: Each loan type has its own workflow definition
3. **Data Isolation by Product**: Each product (LMS, Memo) owns its business data
4. **Shared Infrastructure**: Workflow and Form services are product-agnostic, reusable

---

## Requirements Discussion (Q&A)

### Session 1: Core Architecture

**Q: How should LMS integrate with Flowable?**

Three options were discussed:

| Option | Description | Verdict |
|--------|-------------|---------|
| Option A: LMS-Centric | LMS orchestrates workflow, calls workflow-service | More control but duplicate state |
| Option B: Workflow-Centric | Flowable drives LMS via service tasks | Single source but LMS becomes passive |
| **Option C: Hybrid** ✓ | LMS owns business data, Flowable owns process state | **CHOSEN** - Best of both |

**Chosen Approach (Hybrid):**
```
lms-ui → lms-gateway → lms-service → workflow-service
                            ↓
                      LoanApplication entity
                      (owns business data)
                            ↓
                      workflow-service
                      (owns process state)
```

---

### Session 2: Versioning

**Q: What happens when admin changes a workflow?**

**A: Versioning with effective dates**
- Old applications continue with their original workflow version
- New applications use the latest active version
- `ProcessTemplate` has `version`, `effective_from`, `effective_to` fields

**Q: What about form versioning?**

**A: Immutable once published**
- Forms are immutable once status = ACTIVE
- Editing creates a new version
- Task stores which form version to use

---

### Session 3: Workflow Patterns

**Q: Do you need parallel tasks?**

**A: Yes, absolutely**
- Credit Check AND Property Valuation can happen simultaneously
- Use Flowable's **Parallel Gateway** (fork/join)
- Committee voting uses **Multi-instance tasks** (all must approve)

**Q: Can an approver return to previous step (not just reject)?**

**A: Yes**
- BPMN design includes explicit "Return for Info" transitions
- Variables: `returnReason`, `returnedBy`
- Task goes back to previous assignee

```
[Credit Review] ←── Return for Info ──┐
       │                               │
       └──────► [BM Approval] ─────────┘
                      │
                      └── Reject → [END]
```

---

### Session 4: User & Task Management

**Q: Who can be assigned to tasks?**

**A: Configurable by admin during workflow design**
- Users (specific person)
- Roles (any CREDIT_ANALYST)
- Departments (all users in Credit Dept)
- Groups (custom groups)
- Customers (for external tasks)
- **Array/multiple** combinations

**Q: Do you need Maker-Checker?**

**A: Yes, configurable per task**
- Admin toggles `requires_maker_checker: true` for specific tasks
- Maker completes → system creates Checker task
- Checker approves/rejects
- Configurable `checker_roles`

**Q: What about delegation?**

**A: Both manual and automatic**

| Type | Description |
|------|-------------|
| Manual | User delegates to colleague with appropriate permission |
| Auto | Based on leave calendar from org-service |

Both are configurable.

---

### Session 5: Data Ownership

**Q: Where should form data live?**

**A: Each product owns its data**

```
lms-service (lms_db)          memo-service (memo_db)
├── LoanApplication           ├── Memo
├── LoanProduct               ├── MemoTemplate
├── Collateral                └── MemoApproval
└── Disbursement

workflow-service (workflow_db)  ←── Product-agnostic!
├── ProcessTemplate (linked to product via productId)
├── ProcessInstanceMetadata
└── ActionTimeline

form-service (form_db)          ←── Product-agnostic!
├── FormDefinition (linked to product via productId)
└── FormSubmission
```

**Why this is correct:**
- Each product has its own business logic, validation, calculations
- Workflow & Forms are shared infrastructure
- Adding new product just needs new `xxx-service`

---

### Session 6: Application Lifecycle

**Q: Can RM save application as draft?**

**A: Yes, with partial save**
- Long forms can be saved incrementally
- Draft state before workflow starts
- Workflow starts only on SUBMIT
- Uses `FormDraft` entity for partial data

**Q: What about post-approval modifications?**

**A: Configurable per product**

| Mode | Description |
|------|-------------|
| FRESH | Cancel existing, start new application with reference |
| PARTIAL | Amendment subprocess, only affected steps re-executed |

Bank configures which mode for each product.

---

### Session 7: Loan Types

**Q: What loan types are needed?**

**A: Multiple types confirmed**

| Type | Description | Special Handling |
|------|-------------|------------------|
| Standard | Home, Vehicle, Personal | Normal workflow |
| Top-up | Additional amount on existing loan | Link to parent loan, calculate available |
| Renewal | Extend/renew existing loan | Link to parent, renewal workflow |
| Joint | 2+ applicants on same loan | Co-applicants array, combined credit check |
| Group | Microfinance style | LoanGroup entity, member allocations |

---

### Session 8: Customer Portal

**Q: Do customers use the system directly?**

**A: Yes, but later**

- **Phase 1**: Bank staff acts on behalf of customer (visits branch)
- **Future**: Customer portal for:
  - Document upload
  - Application tracking
  - Digital signature
  - Clarification responses

Implementation uses **External Task pattern**:
- Workflow waits for customer action
- Customer portal completes the external task
- Message event signals Flowable to continue

---

### Session 9: SLA & Escalation

**Q: Do you need SLA tracking?**

**A: Yes**

- Per-task SLA configuration (`sla_hours`)
- BPMN: Boundary Timer Event
- When timer fires → escalate to configured role
- Notification sent to escalated user
- Recorded in ActionTimeline

---

### Session 10: Documents

**Q: How to handle document storage?**

**A: Internal system storage**

- Local file system for development
- MinIO for production (S3-compatible)
- Storage abstraction via `StorageService` interface
- **Document Service created ✓**

---

### Session 11: Notifications

**Q: When should notifications be sent?**

**A: Everywhere, configurable by admin**

- Task assigned → Email/SMS to assignee
- SLA approaching → Warning notification
- SLA breached → Escalation notification
- Application status change → Customer notification
- Approval/Rejection → Customer notification

Admin configures notification templates and triggers.

---

### Session 12: Reporting

**Q: What reports are needed?**

**A: Comprehensive reporting**

| Report | Purpose |
|--------|---------|
| TAT (Turn-Around-Time) | Average processing time by product/branch |
| Pipeline | Applications by stage (funnel view) |
| Rejection Analysis | Reasons, rates, trends |
| Staff Productivity | Tasks completed per user |
| SLA Breach | Overdue tasks, escalations |

---

## Architecture Overview

### High-Level Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SYSTEM ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ADMIN FLOW:                                                                 │
│  ══════════                                                                  │
│  cas-admin-ui → admin-gateway → workflow-service → Create ProcessTemplate   │
│                              → form-service → Create FormDefinition          │
│                              → Link form to workflow task                    │
│                                                                              │
│  LOAN OFFICER FLOW:                                                          │
│  ══════════════════                                                          │
│  lms-ui → lms-gateway → lms-service → Create LoanApplication (DRAFT)        │
│                                    → Submit → workflow-service.startProcess()│
│                                    → Store processInstanceId                  │
│                                                                              │
│  TASK PROCESSING:                                                            │
│  ════════════════                                                            │
│  lms-ui → lms-gateway → workflow-service → Get my tasks                      │
│                      → form-service → Get task form                          │
│                      → workflow-service → Complete task with variables       │
│                      → lms-service → Update LoanApplication status           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Typical Loan Workflow (Home Loan Example)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     HOME LOAN WORKFLOW STAGES                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. ORIGINATION (RM/Customer)                                               │
│     ├── Select Loan Product                                                  │
│     ├── Fill Application Form                                                │
│     └── Upload Initial Documents                                             │
│                                                                              │
│  2. DATA ENTRY (Back-office)                                                 │
│     ├── Verify/Complete data                                                 │
│     ├── Calculate eligibility                                                │
│     └── Mark "Ready for Processing"                                          │
│                                                                              │
│  3. CREDIT ASSESSMENT (Credit Analyst)         ← PARALLEL EXECUTION         │
│     ├── Credit Bureau Check (Service Task)     │                            │
│     ├── Risk Scoring                           │                            │
│     └── Recommendation                         │                            │
│                                                │                            │
│  4. COLLATERAL VALUATION (For secured loans)   │                            │
│     ├── Site Visit                             │                            │
│     ├── Valuation Report                       │                            │
│     └── Legal Verification                     ↓                            │
│                                                                              │
│  5. APPROVAL (Amount-based routing)                                          │
│     ├── < 10L: Branch Manager                                                │
│     ├── 10L-50L: Regional Manager                                            │
│     └── > 50L: Credit Committee (Multi-instance)                            │
│                                                                              │
│  6. DOCUMENTATION (Legal/Ops)                                                │
│     ├── Generate Sanction Letter                                             │
│     ├── Collect Signed Documents                                             │
│     └── Create Account in CBS                                                │
│                                                                              │
│  7. DISBURSEMENT (Operations)                                                │
│     ├── Pre-disbursement Checklist                                           │
│     ├── Fund Transfer (Service Task)                                         │
│     └── Notify Customer                                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Microservices Breakdown

### Service Inventory

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| **cas-server** | 9000 | cas_db | Auth, Users, Roles, Products, SSO |
| **workflow-service** | 9002 | workflow_db | Flowable engine, Processes, Tasks |
| **form-service** | 9003 | form_db | Form definitions, Submissions |
| **notification-service** | 9004 | notification_db | Email, SMS, Push notifications |
| **document-service** | 9005 | document_db | File storage (Local/MinIO) |
| **organization-service** | 9006 | org_db | Branches, Departments, Hierarchy |
| **person-service** | 9007 | person_db | **Person/Party Master** (cross-product identity) |
| **policy-engine-service** | 9001 | policy_db | ABAC/RBAC policies |
| **lms-service** | 9010 | lms_db | Loan domain entities |

### Gateways

| Gateway | Port | Routes To |
|---------|------|-----------|
| **admin-gateway** | 8085 | CAS, Workflow Admin, Form Admin |
| **lms-gateway** | 8086 | LMS, Workflow, Forms, Documents |
| **wfm-gateway** | 8087 | Workflow management APIs |

### Frontends

| UI | Port | Purpose |
|----|------|---------|
| **cas-admin-ui** | 5173 | CAS administration |
| **lms-ui** | 5174 | Loan officers, Branch staff |
| **customer-portal** | 5175 | Customer self-service (Future) |

---

## Core Features

### Feature Matrix

| Feature | Configured By | Enforced By | Status |
|---------|--------------|-------------|--------|
| Workflow versioning | workflow-service | Start process uses latest active | 🔜 Planned |
| Form versioning | form-service | Task stores version used | 🔜 Planned |
| Parallel tasks | BPMN design | Flowable engine | ✅ Supported |
| Multi-instance tasks | BPMN design | Flowable engine | ✅ Supported |
| Dynamic assignment | BPMN + rules | AssignmentRuleEvaluator | ✅ Exists |
| Maker-Checker | Task config | workflow-service | 🔜 Planned |
| SLA/Escalation | Task config + BPMN timer | Flowable engine | 🔜 Planned |
| Manual delegation | API | workflow-service | 🔜 Planned |
| Auto delegation | Leave calendar | workflow-service + org-service | 🔜 Planned |
| Document upload | API | document-service | ✅ Created |
| Notifications | Templates + triggers | notification-service | 🔜 Planned |
| Partial/Draft save | form-service | FormDraft entity | 🔜 Planned |
| Top-up loans | LMS config | lms-service | 🔜 Planned |
| Renewal loans | LMS config | lms-service | 🔜 Planned |
| Joint applicants | LMS entity | lms-service | 🔜 Planned |
| Group loans | LMS entity | lms-service | 🔜 Planned |

---

## Data Model

### Person Master (Central Identity)

> **Key Concept**: A single person can have multiple roles across the system - borrower on one loan, guarantor on another, co-signer on a third, father of an applicant, member of a group. The Person Master provides a unified view.

```
Person (Central Identity)
├── id (UUID - unique identifier)
├── person_code (auto-generated: P-2026-00001)
├── 
│   === IDENTITY ===
├── salutation: MR | MRS | MS | DR
├── first_name, middle_name, last_name
├── full_name (computed)
├── date_of_birth, gender
├── 
│   === IDENTIFIERS ===
├── citizenship_number (unique)
├── national_id
├── passport_number
├── pan_number
├── photo_url
├── 
│   === CONTACT ===
├── primary_phone, secondary_phone
├── email
├── current_address: JSON {street, city, district, province, country, postal}
├── permanent_address: JSON
├── 
│   === EMPLOYMENT/INCOME ===
├── occupation_type: SALARIED | SELF_EMPLOYED | BUSINESS | RETIRED | STUDENT
├── employer_name, designation
├── monthly_income, annual_income
├── employment_details: JSON
├── 
│   === KYC ===
├── kyc_status: PENDING | VERIFIED | EXPIRED
├── kyc_verified_at, kyc_verified_by
├── kyc_documents: [document_ids]
├── 
│   === METADATA ===
├── is_active: boolean
├── created_at, updated_at
└── created_by, branch_id

PersonRelationship (Family/Business Links)
├── id
├── person_id → Person (primary)
├── related_person_id → Person
├── relationship_type:
│   ├── SPOUSE
│   ├── FATHER | MOTHER
│   ├── SON | DAUGHTER
│   ├── SIBLING
│   ├── GRANDPARENT | GRANDCHILD
│   ├── BUSINESS_PARTNER
│   └── OTHER
├── is_verified: boolean
└── created_at

PersonRole (Roles across System)
├── id
├── person_id → Person
├── role_type: BORROWER | CO_BORROWER | GUARANTOR | NOMINEE | GROUP_MEMBER
├── entity_type: LOAN_APPLICATION | LOAN_GROUP
├── entity_id → LoanApplication or LoanGroup
├── role_details: JSON (share_percent, liability_type, etc.)
├── is_active: boolean
└── created_at
```

### 360° Customer View

With Person Master, you can answer:
- **All Loans**: Find all loans where person is involved (any role)
- **Total Exposure**: Sum of liabilities across roles
- **Family Exposure**: Loans where any family member is involved
- **Group Memberships**: All microfinance groups
- **Document Sharing**: KYC docs linked once, used everywhere
- **Relationship Risk**: Pattern detection (e.g., same guarantor on many loans)

---

### LMS Core Entities (Updated)

```
LoanProduct
├── id, code, name, description
├── 
│   === TYPE ===
├── loan_type: SECURED | UNSECURED | GOLD | MICROFINANCE
├── 
│   === AMOUNTS & TERMS ===
├── min_amount, max_amount
├── interest_rate (annual %)
├── min_tenure, max_tenure (months)
├── processing_fee_percent
├── 
│   === WORKFLOW & FORMS ===
├── workflow_template_id → ProcessTemplate
├── application_form_id → FormDefinition
├── application_form_version (immutability)
├── 
│   === DOCUMENT REQUIREMENTS ===
├── document_checklist: JSON [{
│       documentType: "ID_PROOF",
│       name: "Citizenship",
│       required: true,
│       forCoApplicant: true
│   }]
├── 
│   === CONFIGURATION ===
├── allow_topup: boolean
├── allow_renewal: boolean
├── allow_joint_applicants: boolean
├── max_co_applicants: integer (default 3)
├── amendment_mode: FRESH | PARTIAL
├── eligibility_criteria: JSON (rules)
├── config: JSON (product-specific)
├── 
│   === AUDIT ===
├── active: boolean
├── created_by, created_at, updated_at
└── product_id → CAS Product

LoanApplication
├── id
├── application_number: "LN-2026-00001"
├── loan_product_id → LoanProduct
├── 
│   === APPLICATION TYPE ===
├── application_type: NEW | TOPUP | RENEWAL | AMENDMENT
├── parent_loan_id (for topup/renewal)
├── parent_application_id
├── 
│   === PRIMARY APPLICANT ===
├── person_id → Person  ← NEW: Link to Person Master
├── customer_id (legacy/external)
├── applicant_name, email, phone (denormalized)
├── 
│   === CO-APPLICANTS ===
├── co_applicants: JSON [{
│       personId: UUID,  ← Link to Person Master
│       customerId: UUID,
│       name, email, phone,
│       role: CO_BORROWER | GUARANTOR | CO_SIGNER,
│       details: {}
│   }]
├── 
│   === LOAN DETAILS ===
├── requested_amount, approved_amount
├── interest_rate
├── requested_tenure, approved_tenure (months)
├── topup_amount (for top-up)
├── loan_purpose
├── 
│   === STATUS ===
├── status: DRAFT | SUBMITTED | UNDER_REVIEW | PENDING_DOCS |
│           PENDING_APPROVAL | APPROVED | CONDITIONALLY_APPROVED |
│           REJECTED | CANCELLED | ON_HOLD | DISBURSEMENT_PENDING |
│           DISBURSED
├── sub_status (custom tracking)
├── 
│   === WORKFLOW INTEGRATION ===
├── process_instance_id (Flowable)
├── current_task_id
├── current_task_name
├── current_task_assignee
├── task_assigned_at
├── task_sla_deadline ← NEW: SLA tracking
├── 
│   === FORM DATA ===
├── application_data: JSON (all form fields)
├── form_version_used (immutability)
├── 
│   === DECISION ===
├── decided_by, decided_by_name
├── decided_at
├── decision_comments
├── rejection_reason
├── 
│   === ORGANIZATION ===
├── branch_id, branch_name
├── 
│   === AUDIT ===
├── created_by, created_by_name
├── submitted_by, submitted_by_name
├── submitted_at
└── created_at, updated_at

LoanGroup (for microfinance/group loans)
├── id, group_name, group_code
├── group_type: SHG | JLG | COOPERATIVE
├── leader_id → Person
├── members: [{person_id, share_percent, member_since}]
├── max_members
├── formation_date
├── branch_id
└── is_active

Collateral (for secured loans)
├── id, loan_application_id
├── collateral_type: PROPERTY | VEHICLE | FD | GOLD | SHARES
├── description
├── owner_person_id → Person
├── estimated_value
├── valuation_amount, valuation_date
├── valuation_by
├── legal_status: CLEAR | ENCUMBERED | DISPUTED
├── documents: [document_ids]
└── is_primary: boolean

Disbursement
├── id, loan_application_id
├── tranche_number (for phased disbursement)
├── disbursement_date, amount
├── bank_account: JSON {bank, branch, account_number, ifsc}
├── transaction_reference
├── disbursed_by
└── status: PENDING | PROCESSING | COMPLETED | FAILED
```

### Workflow Enhancements (Implemented ✅)

```
ProcessTemplate (Enhanced)
├── id, product_id, name, description
├── flowable_process_def_key
├── flowable_deployment_id
├── version: integer
├── status: DRAFT | ACTIVE | DEPRECATED
├── bpmn_xml: TEXT
├── 
│   === VERSIONING (NEW) ===
├── effective_from: timestamp
├── effective_to: timestamp (null = forever)
├── previous_version_id: UUID
├── 
│   === FORM LINKS (NEW) ===
├── start_form_id → FormDefinition
├── start_form_version: integer
├── 
│   === CONFIGURATION (NEW) ===
├── default_sla_hours: integer
├── config: JSON
├── 
│   === AUDIT ===
├── created_by, created_by_name
└── created_at, updated_at

TaskConfiguration (NEW ✅)
├── id
├── process_template_id → ProcessTemplate
├── task_key (BPMN task ID, e.g., "creditReviewTask")
├── task_name, description
├── task_order (display order)
├── 
│   === FORM MAPPING ===
├── form_id → FormDefinition
├── form_version: integer
├── 
│   === MAKER-CHECKER ===
├── requires_maker_checker: boolean
├── checker_roles: JSON [role_codes]
├── 
│   === SLA ===
├── sla_hours: integer
├── warning_hours: integer (hours before SLA to warn)
├── escalation_role: role_code
├── 
│   === RETURN PATHS ===
├── can_return_to: JSON [task_keys]
├── 
│   === NOTIFICATIONS ===
├── assignment_notification_code
├── completion_notification_code
├── sla_warning_notification_code
├── sla_breach_notification_code
├── 
│   === CONFIGURATION ===
├── assignment_config: JSON (override default assignment)
└── config: JSON

FormDefinition (Enhanced)
├── id, product_id, name, description
├── version: integer
├── 
│   === TYPE (NEW) ===
├── form_type: GENERAL | START_FORM | TASK_FORM | APPROVAL_FORM |
│              DOCUMENT_FORM | CUSTOMER_FORM
├── 
│   === SCHEMA ===
├── schema: JSON (JSON Schema)
├── ui_schema: JSON (rendering hints)
├── layout_config: JSON (NEW - multi-step layout)
├── validation_rules: JSON (NEW - custom validation)
├── 
│   === VERSIONING (NEW) ===
├── status: DRAFT | ACTIVE | DEPRECATED
├── previous_version_id
├── published_at, published_by
├── 
│   === AUDIT ===
├── created_by, created_by_name
└── created_at, updated_at

FormDraft (NEW ✅ - Partial Save)
├── id
├── form_definition_id → FormDefinition
├── form_version
├── user_id, user_name
├── 
│   === PROGRESS ===
├── form_data: JSON (partial data)
├── completed_fields: JSON (field → boolean)
├── current_step: integer
├── total_steps: integer
├── 
│   === CONTEXT ===
├── linked_entity_type (e.g., "LOAN_APPLICATION")
├── linked_entity_id
├── context: string
├── is_auto_save: boolean
├── 
│   === LIFECYCLE ===
├── expires_at: timestamp
└── created_at, updated_at
```

### Notification Entities (Implemented ✅)

```
NotificationTemplate
├── id, code (unique identifier)
├── name, description
├── channel: EMAIL | SMS | PUSH | IN_APP
├── trigger_event: TASK_ASSIGNED | SLA_WARNING | APPLICATION_APPROVED | etc.
├── subject_template: "Loan ${applicationNumber} - ${status}"
├── body_template: "Dear ${applicantName}, ..."
├── placeholders: JSON (list of available placeholders)
├── is_active: boolean
└── product_id (null = global)

NotificationLog
├── id, template_id
├── channel, recipient
├── subject, body (rendered)
├── status: PENDING | SENT | FAILED | READ
├── sent_at, read_at
├── error_message
├── retry_count, max_retries
├── linked_entity_type, linked_entity_id
└── created_at
```

---


## API Design

### Key Endpoints

#### LMS Service
```
POST   /api/loan-products                    # Create product
GET    /api/loan-products                    # List products
GET    /api/loan-products/{code}             # Get by code

POST   /api/applications                     # Create draft
PUT    /api/applications/{id}                # Update draft
POST   /api/applications/{id}/submit         # Submit (starts workflow)
GET    /api/applications/{id}                # Get details
GET    /api/applications/my-submissions      # User's applications
GET    /api/applications/by-branch/{id}      # Branch applications
POST   /api/applications/{id}/topup          # Start topup
```

#### Workflow Service
```
POST   /api/processes/start                  # Start process
GET    /api/processes/{id}                   # Get process details
GET    /api/processes/{id}/timeline          # Get history

GET    /api/tasks/my-tasks                   # Assigned to me
GET    /api/tasks/claimable                  # Can claim
POST   /api/tasks/{id}/claim                 # Claim task
POST   /api/tasks/{id}/complete              # Complete with variables
POST   /api/tasks/{id}/delegate              # Delegate to user
POST   /api/tasks/{id}/return                # Return to previous
```

#### Document Service ✅ (Created)
```
POST   /api/documents                        # Upload
GET    /api/documents/{id}                   # Get metadata
GET    /api/documents/{id}/download          # Download file
GET    /api/documents/by-entity/{type}/{id}  # By linked entity
DELETE /api/documents/{id}                   # Soft delete
```

#### Notification Service (Planned)
```
POST   /api/notifications/send               # Send notification
GET    /api/notification-templates           # List templates
POST   /api/notification-templates           # Create template
GET    /api/notification-logs                # Delivery logs
```

#### Person Service ✅ (Created)
```
POST   /api/persons                          # Create person
GET    /api/persons/{id}                     # Get by ID
GET    /api/persons/code/{code}              # Get by person code
GET    /api/persons/by-identifier?type=&value= # By citizenship/phone
GET    /api/persons/search?q=                # Search by name/phone

POST   /api/persons/{id}/relationships       # Add relationship
GET    /api/persons/{id}/relationships       # Get relationships

POST   /api/persons/{id}/roles               # Add role (cross-product)
GET    /api/persons/{id}/roles               # Get all roles
GET    /api/persons/{id}/roles?product=LMS   # Filter by product

GET    /api/persons/{id}/360-view            # 360° view (all info)
```

---

## Implementation Phases

### Phase 1: Core Infrastructure ✅ COMPLETE
- [x] Document Service ✓
- [x] Notification Service ✓
- [x] Workflow versioning (ProcessTemplate, TaskConfiguration) ✓
- [x] Form versioning + draft save (FormDraft entity) ✓

### Phase 2: LMS Domain (IN PROGRESS)
- [x] LoanProduct enhancements ✓
- [x] LoanApplication enhancements ✓
- [ ] Person Master entities
- [ ] Workflow integration (callbacks)
- [ ] Top-up/Renewal service methods

### Phase 3: Advanced Workflow (Weeks 5-6)
- [ ] Maker-Checker pattern (runtime)
- [ ] SLA tracking + escalation (runtime)
- [ ] Manual delegation
- [ ] Auto delegation (leave calendar)

### Phase 4: LMS UI (Weeks 7-8)
- [ ] Dashboard
- [ ] New Application flow
- [ ] Task Inbox
- [ ] Application detail view

### Phase 5: Advanced Features (Weeks 9-10)
- [ ] Joint applicants (uses Person Master)
- [ ] Group loans (LoanGroup entity)
- [ ] Amendment workflow
- [ ] Reporting

### Phase 6: Customer Portal (Future)
- [ ] Customer authentication
- [ ] Application tracking
- [ ] Document upload
- [ ] Digital signature

---

## Progress Tracker

### Completed ✅
- SSO implementation across products
- Product-level authorization (token-for-product, login checks)
- Document Service with Local/MinIO storage
- Notification Service (templates, logs, email sender)
- Workflow versioning (effective dates, version chain)
- TaskConfiguration entity (SLA, maker-checker, forms)
- Form versioning + FormDraft (partial save)
- LoanProduct enhancements (loan types, document checklist, top-up/renewal)
- LoanApplication enhancements (application type, co-applicants, SLA tracking)
- Person Master design documented

### In Progress �
- Person Master entity implementation
- Workflow integration callbacks

### Planned 📋
- Phases 3-6

---

## Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.x, Java 21 |
| Workflow Engine | Flowable 7.x |
| Database | PostgreSQL 15+ (with JSONB) |
| File Storage | Local FS (dev), MinIO (prod) |
| Cache | Redis |
| Frontend | React 18, Vite |
| Gateway | Spring Cloud Gateway |
| Authentication | JWT (RS256), SSO via CAS |
| Messaging | To be decided (Kafka/RabbitMQ for events) |

---

*Document created: 2026-01-19*
*Last updated: 2026-01-19*
