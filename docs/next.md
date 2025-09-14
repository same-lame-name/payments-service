Architectural Implementation Specification: The Pluggable Port Dispatcher1. Grand Design & MotivationOur objective is to create a scalable, type-safe, and maintainable mechanism for selecting the correct adapter implementation for a given port at runtime. This is critical for supporting multiple payment journeys (e.g., by country, by flavor) where each journey might require a different external integration.After extensive discussion, we rejected simpler but flawed designs:•A resolver in the core: This would pollute the core with infrastructural concerns, violating the Hexagonal Architecture boundary.•A single generic dispatcher object: This would destroy compile-time type safety, forcing unsafe casting in the core.•Reliance on ThreadLocal context: This is not compatible with asynchronous workflows and introduces hidden, "magical" dependencies.The approved Grand Design is a pluggable, type-safe dispatcher pattern that embodies the following principles:•The Core is Pristine: The application core remains completely ignorant of adapter selection. It depends only on a type-safe Port.Dispatcher contract.•Infrastructure is Intelligent but Contained: The complexity of routing is handled entirely within the infrastructure layer by a dedicated dispatcher component for each port.•No Code Duplication: Common dispatcher logic is encapsulated in a generic, reusable PortDispatcher interface with default methods, making the pattern "library-esque."•Explicit Dependencies: The JourneySpecification, which contains the routing rules, is passed explicitly as a method argument, ensuring the design is robust and async-safe.This plan will implement the pattern for the DepositPort as the first, foundational example.2. The Implementation PlanThis is a complete, step-by-step guide to implementing the design.Sub-stage 1: Establish the Core ContractsMotivation: We must first modify the core contracts to support the new design. This involves adding the Dispatcher marker interface for type-safe injection and updating the port methods to accept the JourneySpecification explicitly.File to be modified: book-transfers/core/src/main/java/dexter/banking/booktransfers/core/port/out/DepositPort.javaDepositPort.java+12-3 package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;

import java.util.UUID;

/**
* Driven Port for interacting with the external Deposit Banking service.
* The contract is defined in terms of pure domain objects.
* The contract is defined in terms of pure domain objects and explicitly requires
* the JourneySpecification to guide its infrastructure implementation.
  */
  public interface DepositPort {
  DebitLegResult submitDeposit(SubmitDepositRequest request);
  DebitLegResult submitDepositReversal(SubmitDepositReversalRequest request);
  DebitLegResult submitDeposit(SubmitDepositRequest request, JourneySpecification journeySpecification);
  DebitLegResult submitDepositReversal(SubmitDepositReversalRequest request, JourneySpecification journeySpecification);

  record SubmitDepositRequest(UUID transactionId, String accountNumber) {}
  record SubmitDepositReversalRequest(UUID transactionId, UUID reservationId) {}

  /**
    * A marker interface for the dispatcher implementation of this port.
    * The application core should ALWAYS be injected with this specific type
    * to ensure it receives the dispatcher, not a specific adapter.
      */
      interface Dispatcher extends DepositPort {}
      }Sub-stage 2: Enhance the Journey Specification and ConfigurationMotivation: The JourneySpecification must be the single source of truth for a journey's dynamic behavior. We will add a structure to it to hold the routing keys and update the application.yml to provide the concrete values.File to be modified: book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/config/JourneySpecification.javaJourneySpecification.java+12-3  */
      public record JourneySpecification(
      boolean isIdempotencyEnabled,
      List<String> policies,
      Optional<BigDecimal> complianceThreshold
      ) implements ValueObject {
      List<String> policies,
      Optional<BigDecimal> complianceThreshold,
      AdapterRouting adapterRouting
      ) implements ValueObject {

  /**
    * A nested record to hold the routing configuration for all ports.
    * This keeps the main JourneySpecification record clean.
      */
      public record AdapterRouting(
      String depositPort
      ) implements ValueObject {}
      }File to be modified: book-transfers/app/src/main/resources/application.ymlapplication.yml+8   journeys:
      PAYMENT_SUBMIT_V1:
      idempotencyEnabled: true
      policies:
        - "stateTransitionPolicy"
          adapterRouting:
          depositPort: "DEPOSIT_PORT_REST" # Example
          PAYMENT_SUBMIT_V2_SYNC:
          idempotencyEnabled: true
          policies:
        - "stateTransitionPolicy"
          adapterRouting:
          depositPort: "DEPOSIT_PORT_REST" # Example
          PAYMENT_SUBMIT_V2_ASYNC:
          idempotencyEnabled: false
          policies:
        - "stateTransitionPolicy"
          PAYMENT_SUBMIT_V3:
          idempotencyEnabled: true
          policies:
        - "stateTransitionPolicy"
          complianceThreshold: 10000.00

logging:
level:Sub-stage 3: Create the Generic PortDispatcher InfrastructureMotivation: To avoid code duplication across multiple dispatchers, we will create a generic, reusable interface with default methods. This provides a "library-esque" pluggable behavior for any port to opt into.File to be created: C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/out/shared/PortDispatcher.javaPortDispatcher.java+53 package dexter.banking.booktransfers.infrastructure.adapter.out.shared;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* A generic, reusable dispatcher interface that provides default implementations
* for the common logic of routing to port adapters.
*
* @param <P> The type of the Port interface this dispatcher serves (e.g., DepositPort).
  */
  public interface PortDispatcher<P> {

  /**
    * Builds a map of bean names to adapter instances.
    * This is a default method providing a reusable implementation.
    *
    * @param allAdapters The list of all beans implementing the port interface.
    * @param context     The Spring ApplicationContext to look up bean names.
    * @return A map of bean name to adapter instance.
      */
      default Map<String, P> buildAdapterMap(List<P> allAdapters, ApplicationContext context) {
      return allAdapters.stream()
      // The dispatcher itself must be filtered out to prevent recursion.
      // We assume the concrete dispatcher class will implement this interface.
      .filter(adapter -> !(this.getClass().isInstance(adapter)))
      .collect(Collectors.toMap(
      adapter -> getBeanName(context, adapter),
      Function.identity()
      ));
      }

  private String getBeanName(ApplicationContext context, Object bean) {
  try {
  String[] beanNames = context.getBeanNamesForType(bean.getClass());
  if (beanNames.length == 1) {
  return beanNames[0];
  }
  if (beanNames.length > 1) {
  throw new IllegalStateException("Found multiple bean names for adapter class: " + bean.getClass().getName());
  }
  } catch (NoSuchBeanDefinitionException e) {
  // This should not happen if the bean was injected into the list.
  }
  throw new IllegalStateException("Could not determine bean name for adapter: " + bean.getClass().getName());
  }
  }Sub-stage 4: Implement the DepositPort Dispatcher and WorkerMotivation: We will now create the concrete dispatcher for DepositPort. It will be a thin class that "opts-in" to the generic PortDispatcher behavior. We will also create a sample "worker" adapter (JmsDepositPortAdapter) that the dispatcher can route to.File to be created: C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/out/deposit/DepositPortDispatcherPort.javaDepositPortDispatcherPort.java+42 package dexter.banking.booktransfers.infrastructure.adapter.out.deposit;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.shared.PortDispatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DepositPortDispatcherPort implements DepositPort.Dispatcher, PortDispatcher<DepositPort> {

     private final Map<String, DepositPort> adapterMap;
 
     public DepositPortDispatcherPort(List<DepositPort> allAdapters, ApplicationContext context) {
         // The complex logic is now delegated to the default method in the PortDispatcher interface.
         this.adapterMap = buildAdapterMap(allAdapters, context);
     }
 
     @Override
     public DebitLegResult submitDeposit(SubmitDepositRequest request, JourneySpecification journeySpecification) {
         return getAdapter(journeySpecification).submitDeposit(request, journeySpecification);
     }
 
     @Override
     public DebitLegResult submitDepositReversal(SubmitDepositReversalRequest request, JourneySpecification journeySpecification) {
         return getAdapter(journeySpecification).submitDepositReversal(request, journeySpecification);
     }
 
     private DepositPort getAdapter(JourneySpecification journeySpecification) {
         String adapterName = Optional.ofNullable(journeySpecification.adapterRouting())
                 .map(JourneySpecification.AdapterRouting::depositPort)
                 .orElseThrow(() -> new IllegalStateException("Adapter routing configuration is missing for depositPort."));
 
         return Optional.ofNullable(adapterMap.get(adapterName))
                 .orElseThrow(() -> new IllegalStateException("DepositPort bean not found for name: '" + adapterName + "'. Check service-config and adapter @Component name."));
     }
}File to be modified: C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/out/deposit/JmsDepositPortAdapter.javaJmsDepositPortAdapter.java+28 package dexter.banking.booktransfers.infrastructure.adapter.out.deposit;

/** imports **/

@Component("DEPOSIT_PORT_REST")
@Primary
class DepositAdapter implements DepositPort {

    private final RawDepositClient client;
    private final HttpAdapterMapper mapper;
    @Autowired
    public DepositAdapter(RawDepositClient client, HttpAdapterMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public DebitLegResult submitDeposit(SubmitDepositRequest command) {
        DepositBankingRequest request = mapper.toDepositBankingRequest(command);
        DepositBankingResponse responseDto = client.submitDeposit(request);
        return mapper.toDomain(responseDto);
    }

    @Override
    public DebitLegResult submitDepositReversal(SubmitDepositReversalRequest command) {
        DepositBankingReversalRequest request = mapper.toDepositReversalRequest(command);
        DepositBankingResponse responseDto = client.submitDepositReversal(command.reservationId(), request);
        return mapper.toReversalDomain(responseDto);
    }


    @FeignClient(value = "deposit-banking-service")
    interface RawDepositClient {

        @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_DEPOSIT_BANKING)
        DepositBankingResponse submitDeposit(@RequestBody DepositBankingRequest reservationRequest);
        @PutMapping(
                produces = MediaType.APPLICATION_JSON_VALUE,
                value = ApiConstants.API_DEPOSIT_BANKING + "/{depositRequestId}/cancelled")
        DepositBankingResponse submitDepositReversal(@PathVariable("depositRequestId") UUID depositRequestId,
                                                     @RequestBody DepositBankingReversalRequest depositBankingReversalRequest);
    }
}This concludes the architectural implementation plan. It is complete, robust, and ready for execution.