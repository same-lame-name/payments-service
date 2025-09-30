package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.context.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import java.lang.reflect.Method;

@Aspect
class WithJourneyContextAspect {

    private final BlueprintProvider blueprintProvider;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    WithJourneyContextAspect(BlueprintProvider blueprintProvider) {
        this.blueprintProvider = blueprintProvider;
    }

    @Around("@annotation(withJourneyContext)")
    public Object establishAdHocContext(ProceedingJoinPoint pjp, WithJourneyContext withJourneyContext) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), parameterNameDiscoverer);
        String journeyName = (String) expressionParser.parseExpression(withJourneyContext.journeyIdentifier()).getValue(evaluationContext);

        if (journeyName == null) {
            throw new IllegalStateException("SpEL expression for @WithJourneyContext resolved to null.");
        }

        JourneySpecification spec = blueprintProvider.findByName(journeyName)
            .orElseThrow(() -> new IllegalStateException("No journey specification found for identifier: " + journeyName));

        var context = new JourneyContext(spec);
        try {
            // The aspect calls the clean, application-level manager.
            return JourneyContextManager.runWithContext(context, () -> {
                // 1. THE WRAP: The aspect handles its own dirty work.
                // It must catch Throwable to create a valid Callable.
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    // Wrap throwable to escape the lambda
                    throw new AspectExecutionException(t);
                }
            });
        } catch (AspectExecutionException e) {
            // Unwrap and rethrow the original throwable to be fully transparent
            throw e.getCause();
        }
    }

    // Private wrapper exception for transparently handling Throwables in lambdas
    private static class AspectExecutionException extends RuntimeException {
        public AspectExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
