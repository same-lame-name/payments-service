package dexter.banking.commandbus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A highly configurable, performance-oriented implementation of the {@link CommandBus}.
 * Use the static {@link #builder()} method to construct an instance.
 */
public final class CommandDispatcher implements CommandBus {

    private final Map<Class<?>, List<CommandHandler<?, ?>>> commandHandlers;
    private final List<Middleware> middlewares;

    private CommandDispatcher(Builder builder) {
        this.middlewares = List.copyOf(builder.middlewares); // Immutable copy
        this.commandHandlers = builder.commandHandlers.stream()
            .collect(Collectors.groupingBy(
                CommandHandler::getCommandType,
                Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)
            ));
    }

    /**
     * Creates a new fluent builder for configuring a CommandDispatcher.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <R, C extends Command<R>> R send(C command) {
        Objects.requireNonNull(command, "Command must not be null");

        // 1. The final action is to find the handler and execute the command.
        Middleware.Next<R> finalAction = () -> {
            CommandHandler<C, R> handler = route(command);
            return handler.handle(command);
        };

        // 2. CORRECTED: Build the middleware chain using a reverse loop.
        // This ensures the first middleware in the list is the first one executed.
        Middleware.Next<R> chain = finalAction;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            // Capture the current 'next' in the chain for the lambda
            Middleware.Next<R> next = chain;
            chain = () -> middleware.invoke(command, next);
        }


        return chain.invoke();
    }

    @SuppressWarnings("unchecked")
    private <R, C extends Command<R>> CommandHandler<C, R> route(C command) {
        List<CommandHandler<?, ?>> candidateHandlers = commandHandlers.getOrDefault(command.getClass(), Collections.emptyList());

        if (candidateHandlers.isEmpty()) {
            throw new CommandHandlerNotFoundException(command);
        }

        List<CommandHandler<C, R>> matchingHandlers = candidateHandlers.stream()
            .map(h -> (CommandHandler<C, R>) h)
            .filter(h -> h.matches(command))
            .toList();

        if (matchingHandlers.isEmpty()) {
            throw new CommandHandlerNotFoundException(command);
        }

        if (matchingHandlers.size() > 1) {
           throw new CommandHasMultipleHandlersException(command, (Collection<CommandHandler<?, ?>>) (Collection<?>) matchingHandlers);
        }

        return matchingHandlers.get(0);
    }

    /**
     * Fluent builder for creating {@link CommandDispatcher} instances.
     */
    public static class Builder {
        private final List<CommandHandler<?, ?>> commandHandlers = new ArrayList<>();
        private final List<Middleware> middlewares = new ArrayList<>();

        private Builder() {}

        /**
         * Registers a collection of command handlers.
         *
         * @param handlers The handlers to register.
         * @return This builder instance for chaining.
         */
        public Builder withCommandHandlers(List<CommandHandler<?, ?>> handlers) {
            this.commandHandlers.addAll(handlers);
            return this;
        }

        /**
         * Registers a collection of middlewares. Middlewares will be executed in the order they are provided.
         *
         * @param middlewares The middlewares to register.
         * @return This builder instance for chaining.
         */
        public Builder withMiddlewares(List<Middleware> middlewares) {
            this.middlewares.addAll(middlewares);
            return this;
        }

        /**
         * Builds the configured {@link CommandDispatcher}.
         *
         * @return A new, immutable CommandDispatcher instance.
         */
        public CommandDispatcher build() {
            return new CommandDispatcher(this);
        }
    }
}
