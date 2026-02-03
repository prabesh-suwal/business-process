package com.enterprise.memo.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Utility to enrich BPMN XML with task listeners and other runtime
 * configurations.
 * This ensures that all user tasks have proper assignment listeners configured.
 */
@Slf4j
public class BpmnEnricher {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    /**
     * Enriches BPMN XML by injecting task listeners on all userTask elements.
     * 
     * @param bpmnXml Original BPMN XML
     * @return Enriched BPMN XML with task listeners
     */
    /**
     * Enriches BPMN XML by injecting task listeners and condition expressions.
     * 
     * @param bpmnXml     Original BPMN XML
     * @param stepConfigs Workflow step configurations containing condition logic
     * @return Enriched BPMN XML
     */
    public static String enrichBpmn(String bpmnXml,
            java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs) {
        try {
            log.debug("Enriching BPMN XML with task listeners and conditions");

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            // 1. Inject Task Listeners (Assignment)
            NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
            int enrichedCount = 0;
            for (int i = 0; i < userTasks.getLength(); i++) {
                Element userTask = (Element) userTasks.item(i);
                if (injectTaskListener(doc, userTask)) {
                    enrichedCount++;
                }
            }
            log.info("Enriched {} user tasks with assignment listeners", enrichedCount);

            // 2. Inject Conditions (Branching)
            if (stepConfigs != null && !stepConfigs.isEmpty()) {
                injectConditions(doc, stepConfigs);
            }

            // Convert back to XML string
            return documentToString(doc);

        } catch (Exception e) {
            log.error("Failed to enrich BPMN XML: {}", e.getMessage(), e);
            return bpmnXml;
        }
    }

    /**
     * Compatibility method for existing callers (if any)
     */
    public static String enrichBpmn(String bpmnXml) {
        return enrichBpmn(bpmnXml, null);
    }

    private static void injectConditions(Document doc,
            java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs) {
        // Map configs by Task Key (ID)
        java.util.Map<String, com.enterprise.memo.entity.WorkflowStepConfig> configMap = stepConfigs.stream()
                .collect(java.util.stream.Collectors.toMap(com.enterprise.memo.entity.WorkflowStepConfig::getTaskKey,
                        c -> c, (a, b) -> a));

        // Collect all valid element IDs from BPMN (for validation)
        java.util.Set<String> validElementIds = collectAllElementIds(doc);
        log.debug("Valid BPMN element IDs: {}", validElementIds);

        // Validate conditions before injection
        for (com.enterprise.memo.entity.WorkflowStepConfig config : stepConfigs) {
            if (config.getConditionConfig() == null)
                continue;

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> conditions = (java.util.List<java.util.Map<String, Object>>) config
                    .getConditionConfig().get("conditions");

            if (conditions == null)
                continue;

            for (java.util.Map<String, Object> cond : conditions) {
                String targetStep = (String) cond.get("targetStep");
                if (targetStep != null && !targetStep.isEmpty() && !validElementIds.contains(targetStep)) {
                    log.warn(
                            "⚠️ STALE CONDITION: Task '{}' has condition targeting '{}' which does NOT exist in BPMN! "
                                    +
                                    "The condition will be ignored. Please reconfigure the condition in the designer.",
                            config.getTaskKey(), targetStep);
                }
            }
        }

        // Index all Sequence Flows by Source Ref
        NodeList sequenceFlows = doc.getElementsByTagNameNS(BPMN_NS, "sequenceFlow");
        java.util.Map<String, java.util.List<Element>> flowsBySource = new java.util.HashMap<>();

        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element flow = (Element) sequenceFlows.item(i);
            String sourceRef = flow.getAttribute("sourceRef");
            flowsBySource.computeIfAbsent(sourceRef, k -> new java.util.ArrayList<>()).add(flow);
        }

        // Iterate user tasks to find their branching logic
        NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
        for (int i = 0; i < userTasks.getLength(); i++) {
            Element userTask = (Element) userTasks.item(i);
            String taskId = userTask.getAttribute("id");
            com.enterprise.memo.entity.WorkflowStepConfig config = configMap.get(taskId);

            if (config == null || config.getConditionConfig() == null)
                continue;

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> conditions = (java.util.List<java.util.Map<String, Object>>) config
                    .getConditionConfig().get("conditions");

            if (conditions == null || conditions.isEmpty())
                continue;

            // Find outgoing flows
            java.util.List<Element> outgoingFlows = flowsBySource.get(taskId);
            if (outgoingFlows == null)
                continue;

            for (Element flow : outgoingFlows) {
                String targetRef = flow.getAttribute("targetRef");

                // Check if target is a Gateway (typical pattern)
                Element gateway = findElementById(doc, targetRef);
                if (gateway != null && (gateway.getNodeName().contains("Gateway")
                        || gateway.getNodeName().endsWith(":exclusiveGateway"))) {
                    // Start injecting conditions on Gateway's outgoing flows
                    java.util.List<Element> gatewayFlows = flowsBySource.get(targetRef);
                    if (gatewayFlows != null) {
                        for (Element gwFlow : gatewayFlows) {
                            applyConditionToFlow(doc, gwFlow, conditions);
                        }
                    }
                } else {
                    // Direct connection (less common for branching, but possible)
                    applyConditionToFlow(doc, flow, conditions);
                }
            }
        }
    }

    private static void applyConditionToFlow(Document doc, Element flow,
            java.util.List<java.util.Map<String, Object>> conditions) {
        String flowId = flow.getAttribute("id");
        String targetStep = flow.getAttribute("targetRef");

        log.info("Checking flow {} -> {} against {} conditions", flowId, targetStep, conditions.size());

        // Log all condition targets for debugging
        for (java.util.Map<String, Object> cond : conditions) {
            String condTarget = (String) cond.get("targetStep");
            log.info("  Condition targetStep='{}' vs flow targetRef='{}' match={}",
                    condTarget, targetStep, targetStep.equals(condTarget));
        }

        // Find matching condition for this target
        for (java.util.Map<String, Object> cond : conditions) {
            String condTarget = (String) cond.get("targetStep");
            if (targetStep.equals(condTarget)) {
                // Generate Expression
                String field = (String) cond.get("field");
                String operator = (String) cond.get("operator");
                Object value = cond.get("value");

                String expression = buildExpression(field, operator, value);
                log.info("*** INJECTING condition on flow {} -> {}: {}", flowId, targetStep, expression);

                // Add conditionExpression element
                Element condExpr = doc.createElementNS(BPMN_NS, "conditionExpression");
                condExpr.setAttribute("xsi:type", "tFormalExpression");
                condExpr.setTextContent(expression);

                // Remove existing if any
                NodeList existing = flow.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
                while (existing.getLength() > 0) {
                    flow.removeChild(existing.item(0));
                }

                flow.appendChild(condExpr);
                break; // Only one condition per flow
            }
        }
    }

    private static String buildExpression(String field, String operator, Object value) {
        String op = switch (operator) {
            case "EQUALS" -> "==";
            case "NOT_EQUALS" -> "!=";
            case "GREATER_THAN" -> ">";
            case "LESS_THAN" -> "<";
            case "GREATER_THAN_OR_EQUALS" -> ">=";
            case "LESS_THAN_OR_EQUALS" -> "<=";
            default -> "==";
        };

        String valStr;
        if (value instanceof String) {
            valStr = "'" + value + "'";
        } else {
            valStr = String.valueOf(value);
        }

        return "${" + field + " " + op + " " + valStr + "}";
    }

    private static Element findElementById(Document doc, String id) {
        // Simple search (XPath would be better but DOM traversal is fine for small
        // graphs)
        // Check tasks, gateways, events
        String[] tags = { "userTask", "exclusiveGateway", "parallelGateway", "inclusiveGateway", "task", "serviceTask",
                "sendTask", "endEvent" };
        for (String tag : tags) {
            NodeList list = doc.getElementsByTagNameNS(BPMN_NS, tag);
            for (int i = 0; i < list.getLength(); i++) {
                Element el = (Element) list.item(i);
                if (id.equals(el.getAttribute("id"))) {
                    return el;
                }
            }
        }
        return null;
    }

    /**
     * Collect all element IDs from BPMN document (for validation).
     */
    private static java.util.Set<String> collectAllElementIds(Document doc) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        String[] tags = { "userTask", "exclusiveGateway", "parallelGateway", "inclusiveGateway",
                "task", "serviceTask", "sendTask", "startEvent", "endEvent", "intermediateCatchEvent" };
        for (String tag : tags) {
            NodeList list = doc.getElementsByTagNameNS(BPMN_NS, tag);
            for (int i = 0; i < list.getLength(); i++) {
                Element el = (Element) list.item(i);
                String id = el.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * Injects task listener into a userTask element.
     * Returns true if listener was added, false if it already existed.
     */
    private static boolean injectTaskListener(Document doc, Element userTask) {
        String taskId = userTask.getAttribute("id");
        String taskName = userTask.getAttribute("name");

        // Check if extensionElements already exists
        NodeList extensionElementsList = userTask.getElementsByTagNameNS(BPMN_NS, "extensionElements");
        Element extensionElements;

        if (extensionElementsList.getLength() > 0) {
            extensionElements = (Element) extensionElementsList.item(0);

            // Check if task listener already exists
            NodeList taskListeners = extensionElements.getElementsByTagNameNS(FLOWABLE_NS, "taskListener");
            for (int i = 0; i < taskListeners.getLength(); i++) {
                Element listener = (Element) taskListeners.item(i);
                String delegateExpr = listener.getAttribute("delegateExpression");
                if ("${assignmentTaskListener}".equals(delegateExpr)) {
                    log.debug("Task listener already exists on task: {}", taskId);
                    return false; // Already has the listener
                }
            }
        } else {
            // Create extensionElements
            extensionElements = doc.createElementNS(BPMN_NS, "extensionElements");
            userTask.insertBefore(extensionElements, userTask.getFirstChild());
        }

        // Create and add task listener
        Element taskListener = doc.createElementNS(FLOWABLE_NS, "flowable:taskListener");
        taskListener.setAttribute("event", "create");
        taskListener.setAttribute("delegateExpression", "${assignmentTaskListener}");

        extensionElements.appendChild(taskListener);

        log.debug("Injected task listener on task: {} ({})", taskName, taskId);
        return true;
    }

    /**
     * Converts DOM Document to XML string.
     */
    private static String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    // ==================== PARALLEL GATEWAY SUPPORT ====================

    /**
     * Info about a detected gateway in the BPMN.
     */
    public static class GatewayInfo {
        public String id;
        public String name;
        public String type; // parallelGateway, inclusiveGateway, exclusiveGateway
        public int incomingFlows;
        public int outgoingFlows;
        public boolean isJoin;
        public boolean isFork;

        public GatewayInfo(String id, String name, String type, int incoming, int outgoing) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.incomingFlows = incoming;
            this.outgoingFlows = outgoing;
            this.isJoin = incoming > 1;
            this.isFork = outgoing > 1;
        }
    }

    /**
     * Detects all parallel and inclusive gateways in a BPMN document.
     * Returns info about each gateway for UI display/configuration.
     */
    public static java.util.List<GatewayInfo> detectGateways(String bpmnXml) {
        java.util.List<GatewayInfo> gateways = new java.util.ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            // Find all gateways
            String[] gatewayTypes = { "parallelGateway", "inclusiveGateway", "exclusiveGateway" };

            for (String gatewayType : gatewayTypes) {
                NodeList nodes = doc.getElementsByTagNameNS(BPMN_NS, gatewayType);
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element gateway = (Element) nodes.item(i);
                    String id = gateway.getAttribute("id");
                    String name = gateway.getAttribute("name");

                    // Count incoming/outgoing flows
                    int incoming = countFlowsToElement(doc, id, true);
                    int outgoing = countFlowsToElement(doc, id, false);

                    gateways.add(new GatewayInfo(id, name, gatewayType, incoming, outgoing));
                }
            }

            log.debug("Detected {} gateways in BPMN", gateways.size());

        } catch (Exception e) {
            log.error("Error detecting gateways: {}", e.getMessage());
        }

        return gateways;
    }

    /**
     * Counts sequence flows going to (incoming=true) or from (incoming=false) an
     * element.
     */
    private static int countFlowsToElement(Document doc, String elementId, boolean incoming) {
        int count = 0;
        NodeList flows = doc.getElementsByTagNameNS(BPMN_NS, "sequenceFlow");

        for (int i = 0; i < flows.getLength(); i++) {
            Element flow = (Element) flows.item(i);
            String targetAttr = incoming ? "targetRef" : "sourceRef";
            if (elementId.equals(flow.getAttribute(targetAttr))) {
                count++;
            }
        }

        return count;
    }

    /**
     * Enriches BPMN with gateway completion conditions based on configuration.
     * 
     * @param bpmnXml        Original BPMN XML
     * @param stepConfigs    Step configurations (existing functionality)
     * @param gatewayConfigs Gateway configurations for completion modes
     * @return Enriched BPMN XML
     */
    public static String enrichBpmn(String bpmnXml,
            java.util.List<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigs,
            java.util.List<com.enterprise.memo.entity.WorkflowGatewayConfig> gatewayConfigs) {
        try {
            log.debug("Enriching BPMN with task listeners, conditions, and gateway configs");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            // 1. Inject Task Listeners
            NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
            int enrichedCount = 0;
            for (int i = 0; i < userTasks.getLength(); i++) {
                Element userTask = (Element) userTasks.item(i);
                if (injectTaskListener(doc, userTask)) {
                    enrichedCount++;
                }
            }
            log.info("Enriched {} user tasks with assignment listeners", enrichedCount);

            // 2. Inject Conditions
            if (stepConfigs != null && !stepConfigs.isEmpty()) {
                injectConditions(doc, stepConfigs);
            }

            // 3. Inject Gateway Completion Conditions
            if (gatewayConfigs != null && !gatewayConfigs.isEmpty()) {
                injectGatewayCompletionConditions(doc, gatewayConfigs);
            }

            return documentToString(doc);

        } catch (Exception e) {
            log.error("Failed to enrich BPMN XML: {}", e.getMessage(), e);
            return bpmnXml;
        }
    }

    /**
     * Injects completion conditions for gateways with ANY or N_OF_M mode.
     * Uses Flowable's multi-instance and completion condition expressions.
     */
    private static void injectGatewayCompletionConditions(Document doc,
            java.util.List<com.enterprise.memo.entity.WorkflowGatewayConfig> configs) {

        for (var config : configs) {
            if (!config.requiresCompletionCondition()) {
                continue;
            }

            Element gateway = findElementById(doc, config.getGatewayId());
            if (gateway == null) {
                log.warn("Gateway not found in BPMN: {}", config.getGatewayId());
                continue;
            }

            // Add Flowable extension elements for completion condition
            Element extensionElements = getOrCreateExtensionElements(doc, gateway);

            // Add completion condition as execution listener attribute
            // For parallel gateways, we use the completionCondition attribute
            String conditionExpression = buildCompletionCondition(config);

            // Set flowable:completionCondition attribute on the gateway
            gateway.setAttributeNS(FLOWABLE_NS, "flowable:completionCondition", conditionExpression);

            log.info("Injected completion condition on gateway {}: mode={}, expression={}",
                    config.getGatewayId(), config.getCompletionMode(), conditionExpression);
        }
    }

    /**
     * Builds a Flowable completion condition expression based on the config.
     */
    private static String buildCompletionCondition(com.enterprise.memo.entity.WorkflowGatewayConfig config) {
        switch (config.getCompletionMode()) {
            case ANY:
                // Complete when any 1 branch finishes
                return "${nrOfCompletedInstances >= 1}";

            case N_OF_M:
                // Complete when N branches finish
                int required = config.getMinimumRequired() != null ? config.getMinimumRequired() : 1;
                return "${nrOfCompletedInstances >= " + required + "}";

            case ALL:
            default:
                // Default: wait for all (no condition needed)
                return "${nrOfCompletedInstances >= nrOfInstances}";
        }
    }

    /**
     * Gets or creates extensionElements child for an element.
     */
    private static Element getOrCreateExtensionElements(Document doc, Element parent) {
        // Check for existing
        NodeList existing = parent.getElementsByTagNameNS(BPMN_NS, "extensionElements");
        if (existing.getLength() > 0) {
            return (Element) existing.item(0);
        }

        // Create new
        Element extensionElements = doc.createElementNS(BPMN_NS, "bpmn:extensionElements");
        parent.insertBefore(extensionElements, parent.getFirstChild());
        return extensionElements;
    }
}
