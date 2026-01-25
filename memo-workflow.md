Big Picture (Important)

BPMN = structure only
Rules = configuration only

Admins should never edit logic inside BPMN beyond:

Tasks

Gateways

Sequence flow names

Everything else is external configuration mapped to BPMN elements.

Admin’s Mental Model (Very Important)

Admin does NOT think in technical terms.

Admin thinks:

“This memo goes through these steps”

“This step goes to branch credit”

“If delayed, escalate”

“This form is used here”

So your UI should be step-centric, not engine-centric.

Step 0: BPMN Design (Already Done)

Admin uses bpmn.io to:

Design process

Add:

User Tasks

Gateways

End events

Mandatory convention (you must enforce this)

Every UserTask must have a unique taskKey

Example:

makerTask
branchManagerReview
creditReview
riskReview
finalApproval


No logic here. Just names.

Step 1: Workflow Mapping (Very Simple for Admin)
Admin UI

Memo Topic → Workflow

Memo Topic: Credit Approval Memo
--------------------------------
Workflow:
  Process Key: CREDIT_APPROVAL_PROCESS
  Version: Latest


That’s it.

What you store
{
  "memoTopicCode": "CREDIT_APPROVAL",
  "processKey": "CREDIT_APPROVAL_PROCESS",
  "versionStrategy": "LATEST"
}


Admin does this once per MemoTopic.

Step 2: Assignment Rules (Step-wise Configuration)
Admin UI Concept

After BPMN is uploaded:

“Configure each step”

Admin sees a list like:

Steps in Workflow
-----------------
✔ makerTask
✔ branchManagerReview
✔ creditReview
✔ riskReview
✔ finalApproval


Admin clicks one step at a time.

Assignment Rule UI (User Friendly)

Example: creditReview

Admin selects:

Assignment Type

⭕ Role

⭕ Department

⭕ Group / Committee

⭕ Reporting Manager

⭕ Rule-based

Let’s say:

Type: Role
Role: CREDIT_REVIEWER
Scope: Same Branch as Initiator

Optional conditions (advanced)
If loanAmount > 10,000,000 → REGIONAL_CREDIT_HEAD
Else → BRANCH_CREDIT_MANAGER

What you store
{
  "taskKey": "creditReview",
  "assignment": {
    "type": "ROLE",
    "primary": {
      "role": "CREDIT_REVIEWER",
      "scope": "INITIATOR_BRANCH"
    },
    "fallback": {
      "role": "REGIONAL_CREDIT_HEAD"
    }
  }
}

Runtime

Flowable creates task

memo-service:

Reads assignment rule

Calls cas-server

Resolves users

Assigns task

Step 3: Forms Mapping (Per Step)
Admin UI

For each step:

Form Configuration for creditReview
----------------------------------
Form: Credit Approval Form
Editable Fields:
  ☐ Loan Amount
  ☐ Tenure
  ☑ Comments
Mandatory Fields:
  ☑ Comments


Admins understand this instantly.

What you store
{
  "taskKey": "creditReview",
  "form": {
    "formCode": "CREDIT_APPROVAL_FORM",
    "editableFields": ["comments"],
    "mandatoryFields": ["comments"],
    "mode": "REVIEW"
  }
}

Runtime

memo-ui loads task

memo-service gives:

form schema

field permissions

form-service renders dynamically

Step 4: Visibility Rules (Who Can See This Memo)

This is NOT per task, it’s per memo.

Admin UI
Who can VIEW this memo?
----------------------
✔ Initiator
✔ Assigned Users
✔ Branch Manager (same branch)
✔ Audit Department


Optional:

Sensitive Fields:
  riskScore → only Risk Department

What you store
{
  "visibility": {
    "memo": [
      { "type": "ROLE", "role": "BRANCH_MANAGER", "scope": "SAME_BRANCH" },
      { "type": "DEPARTMENT", "code": "AUDIT" }
    ],
    "fields": {
      "riskScore": ["RISK_TEAM"]
    }
  }
}

Enforcement

memo-service filters:

memos list

memo details

fields

Flowable is not involved at all.

Step 5: SLA Rules (Per Step)
Admin UI

For each step:

SLA for creditReview
-------------------
Duration: 2 Working Days
Warn Before: 4 Hours
Calendar: Bank Working Days


Admins already know SLAs, this feels natural.

What you store
{
  "taskKey": "creditReview",
  "sla": {
    "duration": "P2D",
    "warningBefore": "PT4H",
    "calendar": "BANK_WORKING_DAYS"
  }
}

Runtime

Task created event

SLA engine starts timer

Status tracked:

ON_TIME

WARNING

BREACHED

Step 6: Escalation Rules (Linked to SLA)

Admins think:

“If delayed, then what?”

Admin UI
On SLA Breach:
-------------
Action: Escalate
Escalate To: Reporting Manager
After: 2 Hours
Notify: Yes

What you store
{
  "taskKey": "creditReview",
  "escalation": {
    "on": "SLA_BREACH",
    "after": "PT2H",
    "action": {
      "type": "ESCALATE_TO_MANAGER",
      "level": 1
    }
  }
}

Runtime

SLA breached

Escalation engine fires

memo-service:

reassigns or adds approver

sends notifications

Final Runtime Architecture (Clean)
memo-ui
  ↓
memo-service
  ↓      ↓
form-service   workflow-service
  ↓              ↓
forms           flowable


cas-server is called only for resolution, never for logic.

Why this design works in banks

BPMN stays clean

Business can reconfigure anytime

Org changes don’t break workflows

Audit & compliance friendly

Scales across regions & products

1️⃣ How banks think about “if this then that”

In banks, nobody says:

“Add an if condition in code”

They say:

“If amount > 10M, send to committee”

“If rejected, send back to maker”

“If risk is high, add risk head”

“If deviation exists, escalate”

These are called:
👉 Decision rules, not flow logic.

2️⃣ Where the decision itself lives (IMPORTANT)
✅ BPMN decides WHERE branching happens
✅ Configuration decides WHICH path is taken

So BPMN has:

Exclusive Gateway

Named outgoing flows

NO expressions in BPMN

3️⃣ BPMN Design Pattern (Correct Way)
Example: Amount-based approval
          ┌─────────────┐
          │ Credit Review│
          └──────┬──────┘
                 ▼
           ┌────────────┐
           │  Gateway   │  (Approval Level?)
           └───┬────┬──┘
        <=10M   │    │   >10M
                ▼    ▼
        Branch Approval  Committee Approval

In BPMN

Gateway has multiple outgoing sequence flows

Each flow has a business name

LOW_AMOUNT

HIGH_AMOUNT

That’s it.

4️⃣ How Admin configures conditions (Your System)
Admin UI (Natural for banks)

For gateway: Approval Level?

Admin sees:

Decision Rules
--------------
If loanAmount <= 10,000,000
  → Go to: Branch Approval

If loanAmount > 10,000,000
  → Go to: Credit Committee


Admin never sees BPMN IDs.

5️⃣ How you store it (Very Important)
Decision Rule Model
{
  "gatewayKey": "approvalLevelGateway",
  "rules": [
    {
      "condition": "loanAmount <= 10000000",
      "goTo": "LOW_AMOUNT"
    },
    {
      "condition": "loanAmount > 10000000",
      "goTo": "HIGH_AMOUNT"
    }
  ],
  "default": "LOW_AMOUNT"
}


goTo = sequence flow name

Condition = evaluated by memo-service

6️⃣ Runtime Execution (Step-by-step)
At runtime

Workflow reaches gateway

Flowable pauses and asks:

“Which outgoing sequenceFlow?”

memo-service:

Loads decision rules

Evaluates conditions using memo data

memo-service tells Flowable:

Take flow: HIGH_AMOUNT

Flowable just obeys.

7️⃣ How to integrate with Flowable cleanly
Option A (Recommended)

Use exclusive gateway + execution listener

Listener calls memo-service

memo-service returns flow name

Listener sets process variable

Flowable routing is controlled by variable, not expression.

8️⃣ Real Banking Examples (Very Common)
1. Rejection flow
If decision = REJECT
 → Back to Maker

2. Risk-based escalation
If riskScore >= HIGH
 → Add Risk Head Review
Else
 → Skip Risk Head

3. Deviation present
If interestDeviation = true
 → Product Team Review

9️⃣ Parallel conditions (Advanced but real)
BPMN

Parallel Gateway

Config
{
  "parallelConditions": [
    {
      "if": "riskScore == HIGH",
      "addTask": "riskHeadReview"
    },
    {
      "if": "legalDeviation == true",
      "addTask": "legalReview"
    }
  ]
}


This is how real credit committees work.

🔟 Admin UX pattern (Very important)

Admin should configure conditions using:

Field

Operator

Value

Not raw expressions.

Example UI:

Field: Loan Amount
Operator: >
Value: 10,000,000


You convert this to expression internally.

11️⃣ Where each part lives
Concern	Location
Gateway placement	BPMN
Flow names	BPMN
Conditions	memo-service
Evaluation	memo-service
Routing	workflow-service
Final Rule (Golden Rule)

BPMN defines possibilities
Configuration decides reality

This is how banks keep workflows flexible for 10–15 years.

What I’d do next (Strong suggestion)

Next steps that make this rock-solid:

Define Decision Rule DSL

Define Condition Builder UI

Add simulation mode (test memo before go-live)



Let’s do one full, realistic Credit Approval Memo routing, exactly how a bank would run it, mapped cleanly to your services and config model.

I’ll break this into:

Business scenario

BPMN structure (what admin draws)

Routing rules (if-this-then-that)

Assignment, SLA, escalation at each step

End-to-end runtime flow

No fluff. This is production-grade.

1️⃣ Business Scenario (Real Banking Case)

Memo Topic: Credit Approval Memo
Use case: Retail / SME loan approval

Key business rules

If loan amount ≤ 5M → Branch-level approval

If loan amount > 5M → Credit Committee

If risk score = HIGH → Mandatory Risk Head review

If rejected anywhere → Back to Maker

SLA breach → Escalate to next hierarchy

2️⃣ BPMN Structure (What Admin Designs)

Admin uses bpmn.io and draws ONLY this structure:

Tasks
makerSubmit
branchManagerReview
creditAnalystReview
riskReview
committeeApproval
finalApproval

Gateways
gw_amountCheck
gw_riskCheck
gw_decision

High-level flow
Start
 → Maker Submit
 → Credit Analyst Review
 → [Amount Gateway]
     ├─ LOW_AMOUNT → Branch Manager Review
     └─ HIGH_AMOUNT → Committee Approval
 → [Risk Gateway]
     ├─ HIGH_RISK → Risk Head Review
     └─ NORMAL → Skip
 → Final Approval
 → [Decision Gateway]
     ├─ APPROVE → End
     └─ REJECT → Back to Maker

IMPORTANT BPMN RULE

Each outgoing flow has a business name, not logic:

Gateway	Sequence Flow Name
gw_amountCheck	LOW_AMOUNT
gw_amountCheck	HIGH_AMOUNT
gw_riskCheck	HIGH_RISK
gw_riskCheck	NORMAL
gw_decision	APPROVE
gw_decision	REJECT

No expressions. No scripts.

3️⃣ Routing Rules (Configured by Admin)

These live in memo-service, not BPMN.

A) Amount-based routing
{
  "gatewayKey": "gw_amountCheck",
  "rules": [
    {
      "if": {
        "field": "loanAmount",
        "operator": "<=",
        "value": 5000000
      },
      "goTo": "LOW_AMOUNT"
    },
    {
      "if": {
        "field": "loanAmount",
        "operator": ">",
        "value": 5000000
      },
      "goTo": "HIGH_AMOUNT"
    }
  ],
  "default": "LOW_AMOUNT"
}


Admin UI version:

If Loan Amount <= 5,000,000 → Branch Approval
If Loan Amount > 5,000,000 → Committee Approval

B) Risk-based routing
{
  "gatewayKey": "gw_riskCheck",
  "rules": [
    {
      "if": {
        "field": "riskScore",
        "operator": "==",
        "value": "HIGH"
      },
      "goTo": "HIGH_RISK"
    }
  ],
  "default": "NORMAL"
}

C) Decision routing (Approve / Reject)
{
  "gatewayKey": "gw_decision",
  "rules": [
    {
      "if": {
        "field": "decision",
        "operator": "==",
        "value": "APPROVE"
      },
      "goTo": "APPROVE"
    },
    {
      "if": {
        "field": "decision",
        "operator": "==",
        "value": "REJECT"
      },
      "goTo": "REJECT"
    }
  ]
}

4️⃣ Assignment Rules (Per Task)
1. Maker Submit
{
  "taskKey": "makerSubmit",
  "assignment": {
    "type": "INITIATOR"
  }
}

2. Credit Analyst Review
{
  "taskKey": "creditAnalystReview",
  "assignment": {
    "type": "ROLE",
    "role": "CREDIT_ANALYST",
    "scope": "INITIATOR_BRANCH"
  }
}

3. Branch Manager Review
{
  "taskKey": "branchManagerReview",
  "assignment": {
    "type": "ROLE",
    "role": "BRANCH_MANAGER",
    "scope": "INITIATOR_BRANCH"
  }
}

4. Committee Approval
{
  "taskKey": "committeeApproval",
  "assignment": {
    "type": "GROUP",
    "groupCode": "CREDIT_COMMITTEE"
  },
  "completionRule": "ANY_ONE"
}

5. Risk Head Review
{
  "taskKey": "riskReview",
  "assignment": {
    "type": "ROLE",
    "role": "RISK_HEAD",
    "scope": "REGION"
  }
}

6. Final Approval
{
  "taskKey": "finalApproval",
  "assignment": {
    "type": "ROLE",
    "role": "APPROVAL_AUTHORITY",
    "rule": "BASED_ON_AMOUNT"
  }
}

5️⃣ Forms Mapping
Maker

Editable: All fields

Mandatory: Loan amount, tenure, purpose

Reviewer tasks
{
  "taskKey": "creditAnalystReview",
  "form": {
    "formCode": "CREDIT_MEMO_FORM",
    "editableFields": ["riskScore", "comments"],
    "mandatoryFields": ["comments"]
  }
}


Branch / Committee:

Editable: decision, comments

Read-only: loan data

6️⃣ SLA Rules
[
  {
    "taskKey": "creditAnalystReview",
    "sla": { "duration": "P1D" }
  },
  {
    "taskKey": "branchManagerReview",
    "sla": { "duration": "P1D" }
  },
  {
    "taskKey": "committeeApproval",
    "sla": { "duration": "P2D" }
  }
]

7️⃣ Escalation Rules
Example: Credit Analyst SLA breach
{
  "taskKey": "creditAnalystReview",
  "on": "SLA_BREACH",
  "after": "PT2H",
  "action": {
    "type": "ESCALATE_TO_MANAGER"
  }
}

Committee SLA breach
{
  "taskKey": "committeeApproval",
  "on": "SLA_BREACH",
  "action": {
    "type": "NOTIFY",
    "roles": ["CREDIT_HEAD"]
  }
}

8️⃣ Visibility Rules
{
  "memoVisibility": [
    { "type": "INITIATOR" },
    { "type": "ROLE", "role": "BRANCH_MANAGER", "scope": "SAME_BRANCH" },
    { "type": "DEPARTMENT", "code": "AUDIT" }
  ],
  "fieldVisibility": {
    "riskScore": ["RISK_TEAM"]
  }
}

9️⃣ End-to-End Runtime Flow
User creates memo
 ↓
Form instance created
 ↓
Workflow started (processKey)
 ↓
Task: Credit Analyst
 ↓
Amount Gateway evaluated (memo-service)
 ↓
Branch OR Committee
 ↓
Risk Gateway evaluated
 ↓
Optional Risk Review
 ↓
Final Approval
 ↓
Approve → End
Reject → Back to Maker


Flowable executes.
memo-service decides.
cas-server resolves users.

🔑 Why this design is rock-solid

BPMN stays stable for years

Admin controls logic without redeploy

Works across branches, regions, products

Fully auditable

Matches how real banks operateLet’s do one full, realistic Credit Approval Memo routing, exactly how a bank would run it, mapped cleanly to your services and config model.

I’ll break this into:

Business scenario

BPMN structure (what admin draws)

Routing rules (if-this-then-that)

Assignment, SLA, escalation at each step

End-to-end runtime flow

No fluff. This is production-grade.

1️⃣ Business Scenario (Real Banking Case)

Memo Topic: Credit Approval Memo
Use case: Retail / SME loan approval

Key business rules

If loan amount ≤ 5M → Branch-level approval

If loan amount > 5M → Credit Committee

If risk score = HIGH → Mandatory Risk Head review

If rejected anywhere → Back to Maker

SLA breach → Escalate to next hierarchy

2️⃣ BPMN Structure (What Admin Designs)

Admin uses bpmn.io and draws ONLY this structure:

Tasks
makerSubmit
branchManagerReview
creditAnalystReview
riskReview
committeeApproval
finalApproval

Gateways
gw_amountCheck
gw_riskCheck
gw_decision

High-level flow
Start
 → Maker Submit
 → Credit Analyst Review
 → [Amount Gateway]
     ├─ LOW_AMOUNT → Branch Manager Review
     └─ HIGH_AMOUNT → Committee Approval
 → [Risk Gateway]
     ├─ HIGH_RISK → Risk Head Review
     └─ NORMAL → Skip
 → Final Approval
 → [Decision Gateway]
     ├─ APPROVE → End
     └─ REJECT → Back to Maker

IMPORTANT BPMN RULE

Each outgoing flow has a business name, not logic:

Gateway	Sequence Flow Name
gw_amountCheck	LOW_AMOUNT
gw_amountCheck	HIGH_AMOUNT
gw_riskCheck	HIGH_RISK
gw_riskCheck	NORMAL
gw_decision	APPROVE
gw_decision	REJECT

No expressions. No scripts.

3️⃣ Routing Rules (Configured by Admin)

These live in memo-service, not BPMN.

A) Amount-based routing
{
  "gatewayKey": "gw_amountCheck",
  "rules": [
    {
      "if": {
        "field": "loanAmount",
        "operator": "<=",
        "value": 5000000
      },
      "goTo": "LOW_AMOUNT"
    },
    {
      "if": {
        "field": "loanAmount",
        "operator": ">",
        "value": 5000000
      },
      "goTo": "HIGH_AMOUNT"
    }
  ],
  "default": "LOW_AMOUNT"
}


Admin UI version:

If Loan Amount <= 5,000,000 → Branch Approval
If Loan Amount > 5,000,000 → Committee Approval

B) Risk-based routing
{
  "gatewayKey": "gw_riskCheck",
  "rules": [
    {
      "if": {
        "field": "riskScore",
        "operator": "==",
        "value": "HIGH"
      },
      "goTo": "HIGH_RISK"
    }
  ],
  "default": "NORMAL"
}

C) Decision routing (Approve / Reject)
{
  "gatewayKey": "gw_decision",
  "rules": [
    {
      "if": {
        "field": "decision",
        "operator": "==",
        "value": "APPROVE"
      },
      "goTo": "APPROVE"
    },
    {
      "if": {
        "field": "decision",
        "operator": "==",
        "value": "REJECT"
      },
      "goTo": "REJECT"
    }
  ]
}

4️⃣ Assignment Rules (Per Task)
1. Maker Submit
{
  "taskKey": "makerSubmit",
  "assignment": {
    "type": "INITIATOR"
  }
}

2. Credit Analyst Review
{
  "taskKey": "creditAnalystReview",
  "assignment": {
    "type": "ROLE",
    "role": "CREDIT_ANALYST",
    "scope": "INITIATOR_BRANCH"
  }
}

3. Branch Manager Review
{
  "taskKey": "branchManagerReview",
  "assignment": {
    "type": "ROLE",
    "role": "BRANCH_MANAGER",
    "scope": "INITIATOR_BRANCH"
  }
}

4. Committee Approval
{
  "taskKey": "committeeApproval",
  "assignment": {
    "type": "GROUP",
    "groupCode": "CREDIT_COMMITTEE"
  },
  "completionRule": "ANY_ONE"
}

5. Risk Head Review
{
  "taskKey": "riskReview",
  "assignment": {
    "type": "ROLE",
    "role": "RISK_HEAD",
    "scope": "REGION"
  }
}

6. Final Approval
{
  "taskKey": "finalApproval",
  "assignment": {
    "type": "ROLE",
    "role": "APPROVAL_AUTHORITY",
    "rule": "BASED_ON_AMOUNT"
  }
}

5️⃣ Forms Mapping
Maker

Editable: All fields

Mandatory: Loan amount, tenure, purpose

Reviewer tasks
{
  "taskKey": "creditAnalystReview",
  "form": {
    "formCode": "CREDIT_MEMO_FORM",
    "editableFields": ["riskScore", "comments"],
    "mandatoryFields": ["comments"]
  }
}


Branch / Committee:

Editable: decision, comments

Read-only: loan data

6️⃣ SLA Rules
[
  {
    "taskKey": "creditAnalystReview",
    "sla": { "duration": "P1D" }
  },
  {
    "taskKey": "branchManagerReview",
    "sla": { "duration": "P1D" }
  },
  {
    "taskKey": "committeeApproval",
    "sla": { "duration": "P2D" }
  }
]

7️⃣ Escalation Rules
Example: Credit Analyst SLA breach
{
  "taskKey": "creditAnalystReview",
  "on": "SLA_BREACH",
  "after": "PT2H",
  "action": {
    "type": "ESCALATE_TO_MANAGER"
  }
}

Committee SLA breach
{
  "taskKey": "committeeApproval",
  "on": "SLA_BREACH",
  "action": {
    "type": "NOTIFY",
    "roles": ["CREDIT_HEAD"]
  }
}

8️⃣ Visibility Rules
{
  "memoVisibility": [
    { "type": "INITIATOR" },
    { "type": "ROLE", "role": "BRANCH_MANAGER", "scope": "SAME_BRANCH" },
    { "type": "DEPARTMENT", "code": "AUDIT" }
  ],
  "fieldVisibility": {
    "riskScore": ["RISK_TEAM"]
  }
}

9️⃣ End-to-End Runtime Flow
User creates memo
 ↓
Form instance created
 ↓
Workflow started (processKey)
 ↓
Task: Credit Analyst
 ↓
Amount Gateway evaluated (memo-service)
 ↓
Branch OR Committee
 ↓
Risk Gateway evaluated
 ↓
Optional Risk Review
 ↓
Final Approval
 ↓
Approve → End
Reject → Back to Maker


Flowable executes.
memo-service decides.
cas-server resolves users.

🔑 Why this design is rock-solid

BPMN stays stable for years

Admin controls logic without redeploy

Works across branches, regions, products

Fully auditable

Matches how real banks operate


1️⃣ Design Principles (Non-negotiable in Banks)

Admins in banks:

Are NOT developers

Think in policy language

Hate free-text expressions

Want predictability and auditability

So your UI must be:

Dropdown-driven

Explicit

Order-based

Testable before activation

Golden rule:

No raw expressions visible to admin

2️⃣ Decision Rule Builder – Where It Appears

Admin flow:

Memo Topic
  → Workflow
    → Select BPMN
      → Select Gateway
        → Configure Decision Rules


Admin never sees BPMN XML.
They see business names only.

3️⃣ Gateway Selection Screen
Screen: “Decision Points”
Decision Points in Workflow
---------------------------
• Approval Amount Check
• Risk Evaluation
• Final Decision


Each item maps to a BPMN gateway key internally:

approvalAmountGateway
riskCheckGateway
decisionGateway


Admin clicks one.

4️⃣ Decision Rule Builder – Core Screen
Example: Approval Amount Check
Header
Decision: Approval Amount Check
Based on: Credit Approval Memo

Rule List View (Very Important)
Rules (evaluated top to bottom)
--------------------------------
1. If Loan Amount <= 5,000,000 → Branch Approval
2. If Loan Amount > 5,000,000 → Credit Committee
Default → Branch Approval


Controls:

➕ Add Rule

⬆ Move Up

⬇ Move Down

✏ Edit

🗑 Delete

Admins understand ordering instinctively.

5️⃣ Add / Edit Rule – Detailed UI
Section 1: Condition Builder
IF
[ Loan Amount ▼ ] [ <= ▼ ] [ 5,000,000 ]

Field dropdown (from form metadata)

Loan Amount

Tenure

Risk Score

Interest Deviation

Branch Type

This comes from form-service schema.

Operator dropdown (type-aware)

Number: =, !=, >, <, >=, <=

String: is, is not

Boolean: true, false

Enum: dropdown options

Value input

Numeric input

Enum dropdown

Yes / No toggle

Section 2: Routing Action
THEN
Go To Step:
[ Branch Manager Review ▼ ]


This dropdown is populated from:

Outgoing sequence flow names of the gateway

Admin sees human-readable step names, not IDs.

Section 3: Rule Metadata (Optional but powerful)
Rule Name: Low Amount Routing
Priority: 1
Active: Yes


Priority is optional if you rely on ordering.

6️⃣ Default Rule (Mandatory)

Every gateway must have exactly one default.

UI enforces this.

Default Path
------------
If no rule matches → Branch Approval


This prevents broken workflows.

7️⃣ Advanced: Multiple Conditions (AND / OR)

Admins WILL ask for this.

UI pattern (very common in banks)
IF
[ Risk Score ▼ ] [ = ▼ ] [ HIGH ]
AND
[ Loan Amount ▼ ] [ > ▼ ] [ 10,000,000 ]


Controls:

➕ Add Condition

Toggle AND / OR at group level

Internally:

{
  "all": [
    { "field": "riskScore", "op": "=", "value": "HIGH" },
    { "field": "loanAmount", "op": ">", "value": 10000000 }
  ]
}


But admin never sees this JSON.

8️⃣ Advanced: Conditional Skip / Optional Steps

Example:

“If no deviation, skip Product Review”

UI:

IF Interest Deviation = No
→ Skip Product Review


Technically:

Route to NORMAL flow

Skip optional task

Still same gateway model.

9️⃣ Rule Simulation (VERY IMPORTANT)

Before activating rules:

“Test Rule” Panel
Test with Sample Data
--------------------
Loan Amount: 12,000,000
Risk Score: HIGH

Result:
✔ Route to Credit Committee
✔ Risk Review will be added


This alone will save you from endless support calls.

10️⃣ Activation & Versioning

Banks need safety.

UI:

Rule Version: v3
Status: Draft
[ Activate ]


Once activated:

Old running workflows use old version

New memos use new rules

11️⃣ Backend Mapping (Clean & Simple)

What UI produces:

{
  "gatewayKey": "approvalAmountGateway",
  "rules": [
    {
      "conditions": [
        { "field": "loanAmount", "operator": "<=", "value": 5000000 }
      ],
      "goTo": "LOW_AMOUNT"
    },
    {
      "conditions": [
        { "field": "loanAmount", "operator": ">", "value": 5000000 }
      ],
      "goTo": "HIGH_AMOUNT"
    }
  ],
  "default": "LOW_AMOUNT",
  "version": 3,
  "active": true
}


memo-service evaluates this at runtime.

12️⃣ Runtime Evaluation Flow (Quick Recap)
Gateway reached
 ↓
memo-service loads active rules
 ↓
Evaluate top to bottom
 ↓
First match wins
 ↓
Return sequence flow name

13️⃣ Common Mistakes to Avoid (Learned the hard way)

❌ Letting admin type expressions
❌ Putting conditions directly in BPMN
❌ No default rule
❌ No test/simulation
❌ Mixing assignment logic with routing logic

14️⃣ How This Fits Perfectly With Your Architecture
Concern	Service
Rule UI	memo-ui
Rule storage	memo-service
Field metadata	form-service
Execution	workflow-service
Org resolution	cas-server

Each service does one job only.


1️⃣ Assignment Rule Builder UI
1.1 Admin Mental Model (VERY important)

Admins think:

“Who should get this step?”

They do NOT think:

userId

expressions

technical scopes

So your UI must ask human questions.

1.2 Where this UI lives

Admin path:

Memo Topic
 → Workflow
   → Steps
     → Select Step
       → Assignment


Admin always configures one step at a time.

1.3 Assignment Overview Screen
Screen: “Assignment for Step”
Step: Credit Analyst Review
---------------------------
Assignment Type: Role-based
Resolved Scope: Initiator's Branch
Fallback: Regional Credit Head


Buttons:

✏ Edit Assignment

👁 Preview Assignees (VERY IMPORTANT)

📜 View History

1.4 Assignment Type Selection (Core UI)

Admin chooses ONE primary assignment type.

Options (radio buttons)
Assign task to:
◉ Initiator
○ Specific User
○ Role
○ Department
○ Group / Committee
○ Reporting Manager
○ Rule-based (Advanced)


Each option reveals a different configuration panel.

1.5 Role-based Assignment (Most common)
UI
Role Assignment
---------------
Role: [ CREDIT_ANALYST ▼ ]

Scope:
◉ Same Branch as Initiator
○ Same Department
○ Same Region
○ Same State
○ Head Office


Optional:

If no user found:
☑ Escalate to higher level
Fallback Role: [ REGIONAL_CREDIT_HEAD ▼ ]

Backend mapping
{
  "taskKey": "creditAnalystReview",
  "assignment": {
    "type": "ROLE",
    "role": "CREDIT_ANALYST",
    "scope": "INITIATOR_BRANCH",
    "fallback": {
      "role": "REGIONAL_CREDIT_HEAD"
    }
  }
}

1.6 Reporting Manager Assignment

Used a LOT in corporates.

UI
Assign to:
◉ Immediate Reporting Manager
○ Skip levels: [ 1 ▼ ]


Optional:

If manager not available:
☑ Assign to department head

Backend
{
  "type": "MANAGER",
  "level": 1
}

1.7 Group / Committee Assignment
UI
Committee:
[ CREDIT_COMMITTEE ▼ ]

Approval Mode:
◉ Any One Member
○ Majority
○ All Members

Quorum:
[ 3 ]

Backend
{
  "type": "GROUP",
  "groupCode": "CREDIT_COMMITTEE",
  "completionRule": "ANY_ONE",
  "quorum": 3
}

1.8 Rule-based Assignment (Advanced)

Used when:

Amount-based authority

Product-based roles

UI
IF Loan Amount > 10,000,000
 → Assign to REGIONAL_CREDIT_HEAD

ELSE
 → Assign to BRANCH_MANAGER


This UI internally reuses your Decision Rule Builder.

1.9 Preview Assignees (CRITICAL FEATURE)

Admin clicks:

[ Preview Assignees ]


System asks:

Sample Context:
Branch: Kathmandu
Department: Credit


Shows:

Resolved Users:
✔ Ram Shrestha
✔ Sita Rana


This single feature will save weeks of support pain.

2️⃣ Escalation Rule Builder UI
2.1 Admin Mental Model

Admins think:

“If this task is delayed or stuck, what should happen?”

They do NOT think:

timers

listeners

threads

2.2 Where this UI lives
Memo Topic
 → Workflow
   → Steps
     → Select Step
       → SLA & Escalation


Escalation is always attached to a step.

2.3 Escalation Overview Screen
Step: Credit Analyst Review
--------------------------
SLA: 1 Working Day
Escalation: Enabled


Buttons:

➕ Add Escalation Rule

✏ Edit

🗑 Remove

2.4 Add Escalation Rule – Core Screen
Section 1: Trigger
Trigger Escalation When:
◉ SLA Breached
○ No Action For: [ 4 ] Hours
○ Task Rejected

2.5 Section 2: Delay (Optional)
Execute escalation after:
[ 2 ] Hours from trigger


This allows grace periods.

2.6 Section 3: Escalation Action
Action options
Escalation Action:
◉ Reassign Task
○ Add Parallel Approver
○ Notify
○ Auto-Complete

A) Reassign Task
Reassign To:
◉ Reporting Manager
○ Role
○ Department Head

B) Add Parallel Approver
Add Approver:
Role: [ CREDIT_HEAD ▼ ]
Completion Rule:
◉ Any One
○ All

C) Notify Only
Notify:
☑ Email
☑ System Notification

Recipients:
☑ Current Assignee
☑ Manager
☑ Compliance Team

2.7 Multiple Escalation Levels (Very real)

Admins often want:

“First warn, then escalate harder”

UI
Escalation Levels
-----------------
Level 1: Notify Manager (After 2h)
Level 2: Reassign to Dept Head (After 6h)
Level 3: Notify Compliance (After 24h)


Reorderable.

Backend
{
  "taskKey": "creditAnalystReview",
  "escalations": [
    {
      "level": 1,
      "after": "PT2H",
      "action": { "type": "NOTIFY", "roles": ["MANAGER"] }
    },
    {
      "level": 2,
      "after": "PT6H",
      "action": { "type": "REASSIGN", "to": "DEPARTMENT_HEAD" }
    }
  ]
}

2.8 Simulation & Testing (MUST HAVE)
UI
Simulate Escalation
-------------------
If task not completed for 8 hours:
✔ Level 1 triggered
✔ Level 2 triggered

3️⃣ Backend Responsibilities (Clear Split)
Feature	Service
Assignment rules	memo-service
Escalation rules	memo-service
SLA timer	memo-service
User resolution	cas-server
Task execution	workflow-service
4️⃣ Common Mistakes (Avoid These)

❌ Hardcoding escalation in BPMN
❌ No preview of assignees
❌ Mixing assignment & escalation logic
❌ No multi-level escalation
❌ No audit trail

5️⃣ Why this design works long-term

Matches how banks think

Easy for non-tech admins

Extremely flexible

Safe (preview + simulation)

Scales across regions and org changes

What I’d suggest next

Next level polish:

Assignment fallback strategy

Escalation notification templates

Rule audit & version comparison