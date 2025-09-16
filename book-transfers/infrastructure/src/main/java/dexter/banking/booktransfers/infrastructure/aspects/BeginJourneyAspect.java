package dexter.banking.booktransfers.infrastructure.aspects;

import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.context.BeginJourney;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BeginJourneyAspect {

    private final ConfigurationPort configurationPort;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public BeginJourneyAspect(ConfigurationPort configurationPort) {
        this.configurationPort = configurationPort;
    }

    @Around("@annotation(beginJourneyAnnotation)")
    public Object initializeContext(ProceedingJoinPoint pjp, BeginJourney beginJourneyAnnotation) throws Throwable {
        String journeyName = resolveJourneyName(pjp, beginJourneyAnnotation.value());
        JourneySpecification spec = configurationPort.findForJourney(journeyName)
                .orElseThrow(() -> new IllegalArgumentException("JourneySpecification not found for name: " + journeyName));
        JourneyContext context = new JourneyContext(spec);

        try {
            // The aspect calls the clean, application-level manager.
            return JourneyContextManager.runWithContext(context, () -> {
                // 1. THE WRAP: The aspect handles its own dirty work.
                // It must catch Throwable to create a valid Callable.
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    // Wrap the original Throwable to transport it through the 'Callable' interface.
                    throw new Exception(t);
                }
            });
        } catch (Exception e) {
            // 2. THE UNWRAP: The aspect unwraps the exception to restore the original Throwable.
            // This is the aspect "swinging the sword" to guarantee its own transparency.
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

        String journeyName = parser.parseExpression(expression).getValue(context, String.class);
        if (journeyName == null) {
            throw new IllegalStateException("SpEL expression '" + expression + "' evaluated to null. Cannot begin journey.");
        }
        return journeyName;
    }
}
