package dexter.banking.booktransfers.core.application.middleware;

import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.BaseJourneyBlueprint;
import dexter.banking.commandbus.EnrichableCommand;
import dexter.banking.commandbus.EnrichmentFragment;
import dexter.banking.booktransfers.core.port.in.enrichment.DataCollector;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Order(5) // Runs after IdempotencyMiddleware
@Component
@RequiredArgsConstructor
public class EnrichmentMiddleware implements Middleware {

    private final ApplicationContext applicationContext;
    private final Executor dataCollectorExecutor; // A dedicated thread pool for I/O

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        // 1. Check if the command has opted into enrichment.
        if (!(command instanceof EnrichableCommand<?> enrichableCmd)) {
            return next.invoke(); // Pass through if not enrichable.
        }

        // 2. Get blueprint directly from the command object.
        var blueprint = (BaseJourneyBlueprint) enrichableCmd.getBlueprint();
        if (blueprint == null) {
            return next.invoke(); // Pass through if no blueprint is attached.
        }

        List<String> collectorNames = blueprint.getDataCollectors();
        if (collectorNames == null || collectorNames.isEmpty()) {
            return next.invoke(); // Pass through if no collectors are defined.
        }

        // 3. Fetch all collectors and execute them in parallel.
        List<CompletableFuture<EnrichmentFragment>> futures = collectorNames.stream()
                .map(name -> applicationContext.getBean(name, DataCollector.class))
                .map(collector -> CompletableFuture.supplyAsync(() -> collector.collect(command), dataCollectorExecutor))
                .toList();

        // 4. Wait for all parallel collections to complete.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 5. Collect results into a type-safe map.
        Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(EnrichmentFragment::getClass, Function.identity(), (a, b) -> b)); // In case of duplicate fragment types, last one wins

        // 6. Mutate the command object in place.
        enrichableCmd.enrich(fragments);

        // 7. Pass the SAME command object reference down the chain.
        return next.invoke();
    }
}
