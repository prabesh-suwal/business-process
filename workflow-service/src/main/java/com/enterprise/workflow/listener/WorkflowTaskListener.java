package com.enterprise.workflow.listener;

import com.enterprise.workflow.service.WorkflowCallbackService;
import com.enterprise.workflow.service.WorkflowCallbackService.TaskStatusEvent;
import com.enterprise.workflow.service.WorkflowCallbackService.ProcessStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Flowable task listener that sends callbacks when tasks are created, assigned,
 * or completed.
 */
@Slf4j
@Component("workflowTaskListener")
@RequiredArgsConstructor
public class WorkflowTaskListener implements TaskListener {

    private final WorkflowCallbackService callbackService;
    private final RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        log.info("Task listener fired: {} for task {} ({})",
                eventName, delegateTask.getName(), delegateTask.getId());

        try {
            String callbackUrl = getCallbackUrl(delegateTask);
            if (callbackUrl == null) {
                log.debug("No callback URL for process, skipping notification");
                return;
            }

            TaskStatusEvent event = buildTaskEvent(delegateTask);

            switch (eventName) {
                case EVENTNAME_CREATE:
                    callbackService.notifyTaskCreated(callbackUrl, event);
                    break;
                case EVENTNAME_ASSIGNMENT:
                    callbackService.notifyTaskClaimed(callbackUrl, event);
                    break;
                case EVENTNAME_COMPLETE:
                    callbackService.notifyTaskCompleted(callbackUrl, event);
                    break;
                default:
                    log.debug("Ignoring task event: {}", eventName);
            }
        } catch (Exception e) {
            log.error("Error in task listener: {}", e.getMessage(), e);
        }
    }

    private String getCallbackUrl(DelegateTask delegateTask) {
        // Get callback URL from process variable
        Object callbackUrl = delegateTask.getVariable("callbackUrl");
        if (callbackUrl != null) {
            return callbackUrl.toString();
        }

        // Or from process instance
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(delegateTask.getProcessInstanceId())
                    .singleResult();
            if (processInstance != null) {
                Object url = runtimeService.getVariable(processInstance.getId(), "callbackUrl");
                return url != null ? url.toString() : null;
            }
        } catch (Exception e) {
            log.debug("Could not get callback URL from process instance");
        }
        return null;
    }

    private TaskStatusEvent buildTaskEvent(DelegateTask delegateTask) {
        // Get outcome variable if set
        String outcome = null;
        Object outcomeVar = delegateTask.getVariable("outcome");
        if (outcomeVar != null) {
            outcome = outcomeVar.toString();
        }

        return TaskStatusEvent.builder()
                .processInstanceId(delegateTask.getProcessInstanceId())
                .businessKey(getBusinessKey(delegateTask))
                .taskId(delegateTask.getId())
                .taskKey(delegateTask.getTaskDefinitionKey())
                .taskName(delegateTask.getName())
                .assignee(delegateTask.getAssignee())
                .outcome(outcome)
                .build();
    }

    private String getBusinessKey(DelegateTask delegateTask) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(delegateTask.getProcessInstanceId())
                    .singleResult();
            return processInstance != null ? processInstance.getBusinessKey() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
