package dexter.banking.booktransfers.infrastructure.aspects;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Parameter;

@Aspect
public class InJourneyParameterAspect {

    @Around("execution(* *(.., @dexter.banking.booktransfers.core.domain.shared.context.InJourney (*), ..))")
    public Object injectJourneyBlueprint(ProceedingJoinPoint pjp) throws Throwable {
        JourneyBlueprint prebuiltBlueprint = JourneyContextManager.getContext().getSpecification().getBlueprint();

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        int injectionIndex = -1;
        Class<?> requestedBlueprintType = null;
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(InJourney.class)) {
                injectionIndex = i;
                requestedBlueprintType = parameters[i].getType();
                break;
            }
        }

        if (requestedBlueprintType == null || !requestedBlueprintType.isInstance(prebuiltBlueprint)) {
            throw new ClassCastException(String.format(
                "Cannot inject blueprint of type '%s' into parameter of type '%s' for method '%s'.",
                prebuiltBlueprint.getClass().getInterfaces()[0].getSimpleName(),
                requestedBlueprintType != null ? requestedBlueprintType.getSimpleName() : "unknown",
                signature.getMethod().getName()
            ));
        }

        Object[] args = pjp.getArgs();
        args[injectionIndex] = prebuiltBlueprint;
        return pjp.proceed(args);
    }
}
