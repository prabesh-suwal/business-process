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
    public static String enrichBpmn(String bpmnXml) {
        try {
            log.debug("Enriching BPMN XML with task listeners");

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            // Find all userTask elements
            NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
            int enrichedCount = 0;

            for (int i = 0; i < userTasks.getLength(); i++) {
                Element userTask = (Element) userTasks.item(i);
                if (injectTaskListener(doc, userTask)) {
                    enrichedCount++;
                }
            }

            log.info("Enriched {} user tasks with assignment listeners", enrichedCount);

            // Convert back to XML string
            return documentToString(doc);

        } catch (Exception e) {
            log.error("Failed to enrich BPMN XML: {}", e.getMessage(), e);
            // Return original XML if enrichment fails (fail-safe)
            return bpmnXml;
        }
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
}
