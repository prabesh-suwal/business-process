# MMS Workflow Flow Documentation

> **Document Purpose:** Complete trace of all API calls, service interactions, and database operations in the Memo Management System workflow lifecycle.
>
> **Last Updated:** 2026-01-30 08:04 NPT

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Phase 1: Save Draft Workflow](#phase-1-save-draft-workflow)
3. [Phase 2: Deploy Workflow](#phase-2-deploy-workflow) *(pending)*
4. [Phase 3: Create & Submit Memo](#phase-3-create--submit-memo) *(pending)*
5. [Phase 4: Task Actions (Approve/Reject)](#phase-4-task-actions) *(pending)*
6. [Database Schema Reference](#database-schema-reference)

---

## System Architecture

### Services & Ports

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| **memo-ui** | 5174 | - | React frontend |
| **gateway-product** | 8086 | - | API Gateway (routing + JWT) |
| **cas-server** | 9000 | cas_db | Auth, SSO, users, roles |
| **memo-service** | 9008 | memo_db | Memo CRUD, topics, config |
| **workflow-service** | 9002 | workflow_db | Flowable, process templates |

### Request Flow

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   memo-ui    │────▶│  gateway-product │────▶│  memo-service   │
│   :5174      │     │     :8086        │     │     :9008       │
└──────────────┘     └────────┬─────────┘     └────────┬────────┘
                              │                        │
                              │                        ▼
                              │               ┌─────────────────┐
                              │               │workflow-service │
                              └──────────────▶│     :9002       │
                                              └─────────────────┘
```

### Gateway Route Mappings

| Frontend Path | Rewritten To | Target Service |
|---------------|--------------|----------------|
| `/memo-config/**` | `/api/config/**` | memo-service:9008 |
| `/memos/**` | `/api/memos/**` | memo-service:9008 |
| `/memo/api/tasks/**` | `/api/tasks/**` | memo-service:9008 |
| `/workflow/api/**` | `/api/**` | workflow-service:9002 |
| `/auth/**` | `/auth/**` | cas-server:9000 |

---

## Phase 1: Save Draft Workflow

**Status:** ✅ COMPLETED

### API Call 1.1: Save Workflow BPMN

| Property | Value |
|----------|-------|
| **Timestamp** | 2026-01-30 07:34:12 NPT |
| **Method** | `PUT` |
| **Frontend URL** | `http://localhost:8086/memo-config/topics/{topicId}/workflow` |
| **Backend URL** | `http://localhost:9008/api/config/topics/{topicId}/workflow` |
| **Request Body** | BPMN XML (3 user tasks: Task1 → Task2 → task3) |

#### Sequence

```
1. memo-ui sends PUT request
2. gateway-product:
   - Route: "memo-service-config"
   - JwtAuthenticationFilter validates Bearer token
   - Rewrites: /memo-config/** → /api/config/**
   - Forwards to http://localhost:9008
   
3. memo-service:
   - MemoConfigurationController.updateTopicWorkflow()
   - MemoConfigurationService.updateTopicWorkflow()
     - topicRepository.findById(topicId)
     - topic.setWorkflowXml(workflowXml)
     - topicRepository.save(topic)
   
4. Database (memo_db):
   - UPDATE memo_topic 
     SET workflow_xml = '<bpmn:definitions>...', 
         updated_at = '2026-01-30T07:34:12'
     WHERE id = '52c68d65-4887-4795-84a3-54ae427bc727'
```

#### Code References

- Gateway Route: [RouteConfig.java](file:///home/prabesh/1work/mvp/oauth/gateway-product/src/main/java/com/lms/gateway/config/RouteConfig.java#L75-82)
- Controller: [MemoConfigurationController.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/controller/MemoConfigurationController.java#L47-52)
- Service: [MemoConfigurationService.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/service/MemoConfigurationService.java#L77-81)

---

### API Call 1.2: Save Override Permissions

| Property | Value |
|----------|-------|
| **Timestamp** | 2026-01-30 07:34:12 NPT |
| **Method** | `PATCH` |
| **Frontend URL** | `http://localhost:8086/memo-config/topics/{topicId}/override-permissions` |
| **Request Body** | `{"allowOverrideAssignments":false,"allowOverrideSLA":false,...}` |

#### Sequence

```
1. memo-ui sends PATCH request
2. gateway-product routes to memo-service
3. memo-service:
   - MemoConfigurationController.updateTopicOverridePermissions()
   - MemoConfigurationService.updateTopicOverridePermissions()
     - Sets topic.overridePermissions = jsonb
     - Saves to database

4. Database (memo_db):
   - UPDATE memo_topic 
     SET override_permissions = '{"allowOverrideAssignments":false,...}',
         updated_at = NOW()
     WHERE id = '52c68d65-...'
```

#### Code References

- Controller: [MemoConfigurationController.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/controller/MemoConfigurationController.java#L94-99)
- Service: [MemoConfigurationService.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/service/MemoConfigurationService.java#L150-159)

---

### API Call 1.3: Save Step Configurations (x3)

| Property | Value |
|----------|-------|
| **Timestamp** | 2026-01-30 07:54:55 NPT |
| **Method** | `PUT` (3 times, one per task) |
| **Frontend URLs** | `http://localhost:8086/memo-admin/topics/{topicId}/workflow-config/steps/{taskKey}` |

**Calls made:**
- `PUT .../steps/Activity_0do3yv0` (Task1)
- `PUT .../steps/Activity_03yd0xu` (Task2)
- `PUT .../steps/Activity_1j3mbe2` (task3)

#### Gateway Routing

```
Frontend: /memo-admin/topics/{topicId}/workflow-config/steps/{taskKey}
         ↓
Gateway Route: "memo-workflow-config"
         ↓
Rewrite: /memo-admin/topics/{topicId}/workflow-config/** 
       → /api/topics/{topicId}/workflow-config/**
         ↓
Backend: http://localhost:9008/api/topics/{topicId}/workflow-config/steps/{taskKey}
```

#### Sequence

```
1. memo-ui sends PUT request with step configuration JSON
2. gateway-product:
   - Route: "memo-workflow-config"
   - JwtAuthenticationFilter validates Bearer token
   - Rewrites path
   - Forwards to memo-service:9008
   
3. memo-service:
   - WorkflowConfigController.saveStepConfig()
   - WorkflowConfigService.saveStepConfig()
     - topicRepository.findById(topicId)
     - stepConfigRepository.findByMemoTopicIdAndTaskKey() [upsert logic]
     - Set: taskName, assignmentConfig, formConfig, slaConfig, escalationConfig, viewerConfig, conditionConfig
     - stepConfigRepository.save(config)
   
4. Database (memo_db):
   - INSERT/UPDATE workflow_step_config 
     SET task_key = 'Activity_0do3yv0',
         task_name = 'Task1',
         assignment_config = '{"type":"role","roleId":"..."}',
         ...
     WHERE memo_topic_id = '52c68d65-...' AND task_key = '...'
```

#### Code References

- Gateway Route: [RouteConfig.java](file:///home/prabesh/1work/mvp/oauth/gateway-product/src/main/java/com/lms/gateway/config/RouteConfig.java#L125-132)
- Controller: [WorkflowConfigController.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/controller/WorkflowConfigController.java#L49-64)
- Service: [WorkflowConfigService.java](file:///home/prabesh/1work/mvp/oauth/memo-service/src/main/java/com/enterprise/memo/service/WorkflowConfigService.java#L44-71)

#### Database Changes (per task)

| Database | Table | Operation | Columns |
|----------|-------|-----------|---------|
| `memo_db` | `workflow_step_config` | INSERT/UPDATE | id, memo_topic_id, task_key, task_name, assignment_config, form_config, sla_config, escalation_config, viewer_config, condition_config, active |

---

### State After Phase 1

**MemoTopic Record (memo_db.memo_topic):**

| Column | Value |
|--------|-------|
| `id` | `52c68d65-4887-4795-84a3-54ae427bc727` |
| `code` | `CAPEX` |
| `name` | `Capital Expense Request` |
| `workflow_xml` | ✅ BPMN XML with 3 tasks |
| `override_permissions` | ✅ JSON with override flags |
| `workflow_template_id` | ❌ **NULL** (not deployed yet) |
| `updated_at` | `2026-01-30T07:54:55` |

**WorkflowStepConfig Records (memo_db.workflow_step_config):**

| task_key | task_name | assignment_config | sla_config | escalation_config |
|----------|-----------|-------------------|------------|-------------------|
| `Activity_0do3yv0` | Task1 | ✅ Configured | ✅ | ✅ |
| `Activity_03yd0xu` | Task2 | ✅ Configured | ✅ | ✅ |
| `Activity_1j3mbe2` | task3 | ✅ Configured | ✅ | ✅ |

---

## Phase 2: Deploy Workflow

**Status:** ⏳ PENDING

*Will be documented when Deploy button is clicked*

### Expected Flow

```
1. POST /memo-config/topics/{topicId}/deploy-workflow
2. memo-service:
   - Load topic + WorkflowStepConfigs
   - BpmnEnricher.enrichBpmn() - adds task listeners
   - Call workflow-service to deploy
3. workflow-service:
   - Create ProcessTemplate record
   - Deploy to Flowable engine
   - Return processTemplateId
4. memo-service:
   - Update topic.workflowTemplateId
```

---

## Phase 3: Create & Submit Memo

**Status:** ⏳ PENDING

*Will be documented when memo is created and submitted*

---

## Phase 4: Task Actions

**Status:** ⏳ PENDING

*Will be documented when tasks are claimed/completed*

---

## Database Schema Reference

### memo_db Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `memo_category` | Groups topics | id, code, name |
| `memo_topic` | Workflow definitions | id, code, workflow_xml, workflow_template_id |
| `memo` | Memo instances | id, topic_id, process_instance_id, status |
| `memo_task` | Task assignments | id, memo_id, task_id, assignee_id |
| `workflow_step_config` | Per-step config | id, topic_id, task_key, assignment_config |
| `gateway_config` | Parallel gateway modes | id, topic_id, gateway_id, completion_mode |

### workflow_db Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `process_template` | Workflow templates | id, name, bpmn_xml, process_definition_id |
| `task_config` | Task configurations | id, template_id, task_key, assignment_config |
| `ACT_RE_PROCDEF` | Flowable process defs | ID_, KEY_, VERSION_ |
| `ACT_RU_EXECUTION` | Running executions | ID_, PROC_INST_ID_, ACT_ID_ |
| `ACT_RU_TASK` | Active tasks | ID_, EXECUTION_ID_, ASSIGNEE_ |
| `ACT_HI_TASKINST` | Task history | ID_, TASK_DEF_KEY_, END_TIME_ |

---

## Appendix: BPMN Structure

Your current workflow has this structure:

```
StartEvent_1 ─────▶ Activity_0do3yv0 (Task1)
                          │
                          ▼
                    Activity_03yd0xu (Task2)
                          │
                          ▼
                    Activity_1j3mbe2 (task3)
                          │
                          ▼
                    Event_0oki05u (End)
```

**Task IDs for reference:**
- Task1: `Activity_0do3yv0`
- Task2: `Activity_03yd0xu`  
- task3: `Activity_1j3mbe2`
