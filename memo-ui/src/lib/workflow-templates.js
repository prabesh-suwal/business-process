// Sample parallel workflow templates for testing parallel gateway functionality
// These can be loaded into BpmnDesigner for testing

export const VENDOR_ONBOARDING_WORKFLOW = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" 
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" 
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_VendorOnboarding" 
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="vendor_onboarding" name="Vendor Onboarding" isExecutable="true">
    <bpmn:startEvent id="start" name="Start">
      <bpmn:outgoing>flow_to_init</bpmn:outgoing>
    </bpmn:startEvent>
    
    <bpmn:userTask id="business_initiation" name="Business Initiation" flowable:candidateGroups="ROLE_BUSINESS_TEAM">
      <bpmn:incoming>flow_to_init</bpmn:incoming>
      <bpmn:outgoing>flow_to_fork</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:parallelGateway id="fork" name="Parallel Review Fork">
      <bpmn:incoming>flow_to_fork</bpmn:incoming>
      <bpmn:outgoing>flow_to_procurement</bpmn:outgoing>
      <bpmn:outgoing>flow_to_finance</bpmn:outgoing>
      <bpmn:outgoing>flow_to_legal</bpmn:outgoing>
    </bpmn:parallelGateway>
    
    <bpmn:userTask id="procurement_review" name="Procurement Review" flowable:candidateGroups="ROLE_PROCUREMENT">
      <bpmn:incoming>flow_to_procurement</bpmn:incoming>
      <bpmn:outgoing>flow_from_procurement</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="finance_due_diligence" name="Finance Due Diligence" flowable:candidateGroups="ROLE_FINANCE">
      <bpmn:incoming>flow_to_finance</bpmn:incoming>
      <bpmn:outgoing>flow_from_finance</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="legal_review" name="Legal Review" flowable:candidateGroups="ROLE_LEGAL">
      <bpmn:incoming>flow_to_legal</bpmn:incoming>
      <bpmn:outgoing>flow_from_legal</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:parallelGateway id="join" name="Parallel Review Join">
      <bpmn:incoming>flow_from_procurement</bpmn:incoming>
      <bpmn:incoming>flow_from_finance</bpmn:incoming>
      <bpmn:incoming>flow_from_legal</bpmn:incoming>
      <bpmn:outgoing>flow_to_approval</bpmn:outgoing>
    </bpmn:parallelGateway>
    
    <bpmn:userTask id="final_approval" name="Final Approval" flowable:candidateGroups="ROLE_APPROVER">
      <bpmn:incoming>flow_to_approval</bpmn:incoming>
      <bpmn:outgoing>flow_to_end</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:endEvent id="end" name="Vendor Approved">
      <bpmn:incoming>flow_to_end</bpmn:incoming>
    </bpmn:endEvent>
    
    <bpmn:sequenceFlow id="flow_to_init" sourceRef="start" targetRef="business_initiation" />
    <bpmn:sequenceFlow id="flow_to_fork" sourceRef="business_initiation" targetRef="fork" />
    <bpmn:sequenceFlow id="flow_to_procurement" sourceRef="fork" targetRef="procurement_review" />
    <bpmn:sequenceFlow id="flow_to_finance" sourceRef="fork" targetRef="finance_due_diligence" />
    <bpmn:sequenceFlow id="flow_to_legal" sourceRef="fork" targetRef="legal_review" />
    <bpmn:sequenceFlow id="flow_from_procurement" sourceRef="procurement_review" targetRef="join" />
    <bpmn:sequenceFlow id="flow_from_finance" sourceRef="finance_due_diligence" targetRef="join" />
    <bpmn:sequenceFlow id="flow_from_legal" sourceRef="legal_review" targetRef="join" />
    <bpmn:sequenceFlow id="flow_to_approval" sourceRef="join" targetRef="final_approval" />
    <bpmn:sequenceFlow id="flow_to_end" sourceRef="final_approval" targetRef="end" />
  </bpmn:process>
  
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="vendor_onboarding">
      <bpmndi:BPMNShape id="start_di" bpmnElement="start">
        <dc:Bounds x="100" y="200" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="business_initiation_di" bpmnElement="business_initiation">
        <dc:Bounds x="180" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="fork_di" bpmnElement="fork">
        <dc:Bounds x="330" y="193" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="procurement_review_di" bpmnElement="procurement_review">
        <dc:Bounds x="430" y="78" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="finance_due_diligence_di" bpmnElement="finance_due_diligence">
        <dc:Bounds x="430" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="legal_review_di" bpmnElement="legal_review">
        <dc:Bounds x="430" y="278" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="join_di" bpmnElement="join">
        <dc:Bounds x="580" y="193" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="final_approval_di" bpmnElement="final_approval">
        <dc:Bounds x="680" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_di" bpmnElement="end">
        <dc:Bounds x="830" y="200" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

export const CREDIT_APPROVAL_WORKFLOW = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" 
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" 
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  id="Definitions_CreditApproval" 
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="credit_approval" name="Credit Approval Memo" isExecutable="true">
    <bpmn:startEvent id="start" name="Start">
      <bpmn:outgoing>flow_to_rm</bpmn:outgoing>
    </bpmn:startEvent>
    
    <bpmn:userTask id="rm_initiation" name="RM Initiation" flowable:candidateGroups="ROLE_RELATIONSHIP_MANAGER">
      <bpmn:incoming>flow_to_rm</bpmn:incoming>
      <bpmn:outgoing>flow_to_bm</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="branch_manager_review" name="Branch Manager Review" flowable:candidateGroups="ROLE_BRANCH_MANAGER">
      <bpmn:incoming>flow_to_bm</bpmn:incoming>
      <bpmn:outgoing>flow_to_analyst</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="credit_analyst_review" name="Credit Analyst Review" flowable:candidateGroups="ROLE_CREDIT_ANALYST">
      <bpmn:incoming>flow_to_analyst</bpmn:incoming>
      <bpmn:outgoing>flow_to_risk</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="risk_review" name="Risk Department Review" flowable:candidateGroups="ROLE_RISK">
      <bpmn:incoming>flow_to_risk</bpmn:incoming>
      <bpmn:outgoing>flow_to_gateway</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:exclusiveGateway id="amount_gateway" name="Amount Check">
      <bpmn:incoming>flow_to_gateway</bpmn:incoming>
      <bpmn:outgoing>flow_low_amount</bpmn:outgoing>
      <bpmn:outgoing>flow_high_amount</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    
    <bpmn:userTask id="approver" name="Approval Authority" flowable:candidateGroups="ROLE_APPROVER">
      <bpmn:incoming>flow_low_amount</bpmn:incoming>
      <bpmn:outgoing>flow_approver_end</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:userTask id="credit_committee" name="Credit Committee" flowable:candidateGroups="COMMITTEE_CREDIT">
      <bpmn:incoming>flow_high_amount</bpmn:incoming>
      <bpmn:outgoing>flow_committee_end</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:endEvent id="end" name="Approved">
      <bpmn:incoming>flow_approver_end</bpmn:incoming>
      <bpmn:incoming>flow_committee_end</bpmn:incoming>
    </bpmn:endEvent>
    
    <bpmn:sequenceFlow id="flow_to_rm" sourceRef="start" targetRef="rm_initiation" />
    <bpmn:sequenceFlow id="flow_to_bm" sourceRef="rm_initiation" targetRef="branch_manager_review" />
    <bpmn:sequenceFlow id="flow_to_analyst" sourceRef="branch_manager_review" targetRef="credit_analyst_review" />
    <bpmn:sequenceFlow id="flow_to_risk" sourceRef="credit_analyst_review" targetRef="risk_review" />
    <bpmn:sequenceFlow id="flow_to_gateway" sourceRef="risk_review" targetRef="amount_gateway" />
    <bpmn:sequenceFlow id="flow_low_amount" name="Amount &lt; 50L" sourceRef="amount_gateway" targetRef="approver">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${amount &lt; 5000000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="flow_high_amount" name="Amount >= 50L" sourceRef="amount_gateway" targetRef="credit_committee">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${amount >= 5000000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="flow_approver_end" sourceRef="approver" targetRef="end" />
    <bpmn:sequenceFlow id="flow_committee_end" sourceRef="credit_committee" targetRef="end" />
  </bpmn:process>
</bpmn:definitions>`;
