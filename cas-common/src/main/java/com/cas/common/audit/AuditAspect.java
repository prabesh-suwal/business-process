package com.cas.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect that intercepts methods annotated with @Auditable
 * and publishes audit events automatically.
 * 
 * This bean is registered by {@link AuditServletAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditEventPublisher publisher;
    private final AuditProperties properties;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                publishAuditEvent(joinPoint, auditable, result, exception, startTime);
            } catch (Exception e) {
                log.warn("Failed to publish audit event for {}: {}",
                        joinPoint.getSignature().getName(), e.getMessage());
            }
        }
    }

    private void publishAuditEvent(ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object result,
            Exception exception,
            long startTime) {

        // Build evaluation context for SpEL
        EvaluationContext context = buildEvaluationContext(joinPoint, result);

        // Extract resource ID using SpEL
        String resourceId = evaluateExpression(auditable.resourceIdExpression(), context, String.class);

        // Build description
        String description = auditable.description();
        if (description.isEmpty()) {
            description = String.format("%s %s", auditable.action(), auditable.resourceType());
            if (resourceId != null) {
                description += " [" + resourceId + "]";
            }
        }

        // Determine product code
        String productCode = auditable.productCode().isEmpty()
                ? properties.getDefaultProductCode()
                : auditable.productCode();

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", joinPoint.getSignature().toShortString());
        metadata.put("executionTimeMs", System.currentTimeMillis() - startTime);

        // Build and publish the event
        AuditEvent event = AuditEvent.builder()
                .serviceName(properties.getServiceName())
                .productCode(productCode)
                .actorId(AuditContext.getActorId())
                .actorName(AuditContext.getActorName())
                .actorEmail(AuditContext.getActorEmail())
                .actorType(AuditContext.getActorType())
                .actorRoles(AuditContext.getActorRoles())
                .ipAddress(AuditContext.getIpAddress())
                .action(auditable.action())
                .category(auditable.category())
                .resourceType(auditable.resourceType())
                .resourceId(resourceId)
                .description(description)
                .metadata(metadata)
                .result(exception == null ? AuditResult.SUCCESS : AuditResult.FAILURE)
                .failureReason(exception != null ? exception.getMessage() : null)
                .build();

        publisher.publish(event);
    }

    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add return value
        context.setVariable("result", result);

        // Add method arguments
        Object[] args = joinPoint.getArgs();
        context.setVariable("args", args);

        // Add named arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                context.setVariable("arg" + i, args[i]);
                context.setVariable("p" + i, args[i]);
            }
        }

        return context;
    }

    private <T> T evaluateExpression(String expression, EvaluationContext context, Class<T> type) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            return parser.parseExpression(expression).getValue(context, type);
        } catch (Exception e) {
            log.debug("Failed to evaluate SpEL expression '{}': {}", expression, e.getMessage());
            return null;
        }
    }
}
