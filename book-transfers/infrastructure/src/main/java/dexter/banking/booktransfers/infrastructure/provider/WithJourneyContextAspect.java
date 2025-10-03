package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.context.*;
import dexter.banking.booktransfers.core.domain.shared.markers.WithJourneyContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Aspect
@Configurable
public class WithJourneyContextAspect {

    @Autowired
    private BlueprintProvider blueprintProvider;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


    @Around("@annotation(withJourneyContext)")
    public Object establishAdHocContext(ProceedingJoinPoint pjp, WithJourneyContext withJourneyContext) throws Throwable {
        String journeyName = resolveJourneyName(pjp, withJourneyContext.journeyIdentifier());

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

    private String resolveJourneyName(ProceedingJoinPoint pjp, String expression) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = pjp.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        String journeyName = expressionParser.parseExpression(expression).getValue(context, String.class);
        if (journeyName == null) {
            throw new IllegalStateException("SpEL expression '" + expression + "' evaluated to null. Cannot begin journey.");
        }
        return journeyName;
    }

    // Private wrapper exception for transparently handling Throwables in lambdas
    private static class AspectExecutionException extends RuntimeException {
        public AspectExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
