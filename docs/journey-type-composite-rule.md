# The Journey-Type Composite Key: A Rule for Standardized Orchestration Identity

## 1. The Problem: The Need for Unambiguous Orchestration Identity

Modern payment systems must handle a vast and growing portfolio of payment journeys. The business logic, validation rules, and required infrastructure (e.g., clearing networks, FX engines, account adapters) for these journeys are highly context-dependent. A simple business-facing journey name (e.g., "cross-border-transfer") is insufficient for the system to reliably determine the correct processing steps.

This ambiguity forces developers to write complex, brittle `if/else` or `switch` statements deep within the application logic to figure out which rules to apply or which downstream systems to call. This approach violates the Single Responsibility Principle (SRP), making the system difficult to test, maintain, and scale. It creates "God Handlers" that know too much about too many different types of payments, leading to a fragile architecture.

## 2. The Goal: A Declarative System for Handler Selection

To solve this, we must establish a formal, unambiguous **Orchestration Identity** for every class of payment journey. This identity, which we call the `journey-type`, acts as a stable, machine-readable key.

The sole purpose of the `journey-type` is to allow the system to select the **one, correct `CommandHandler`** responsible for orchestrating a specific class of payment, without ambiguity.

This enables a clean, declarative architecture where handlers announce the types of journeys they are responsible for, rather than containing complex internal dispatching logic. This makes the system transparent, scalable, and easy to reason about.

## 3. The Solution: The Six-Part Composite Key

The `journey-type` is a string composed of six orthogonal (independent) components, separated by underscores. This structure is not arbitrary; it is the minimal, maximal set of components required to unambiguously identify a payment orchestration contract.

The defined structure is:
**`<country-code>_<business-process>_<fx-dimension>_<clearing-network>_<source-instrument>_<destination-instrument>`**

## 4. The Principle of the Hierarchy: Decreasing Stability

The components are ordered from the most stable, highest-level context to the most volatile, specific implementation detail. This ensures the key is logical, readable, and optimized for pattern-matching and future evolution.

The hierarchy is:
1.  **Jurisdiction (`<country-code>`)**: The highest-level context.
2.  **Intent (`<business-process>`)**: The fundamental user goal.
3.  **Currency Logic (`<fx-dimension>`)**: A primary fork in orchestration logic.
4.  **Settlement Rails (`<clearing-network>`)**: The implementation of how money moves.
5.  **Endpoints (`<source-instrument>`, `<destination-instrument>`)**: The specific debit/credit contracts, which are the most likely to change or expand.

## 5. Defense of Each Component

Each component is non-negotiable. Removing or merging any one of them introduces critical ambiguity, leading to incorrect orchestrations and runtime failures.

#### `<country-code>`
*   **Why it is Essential:** It defines the **regulatory and infrastructural jurisdiction**.
*   **Litmus Test:** A high-value transfer in the US (`US_..._RTGS_...`) requires an adapter for the **FedWire** network. An identical transfer in the UK (`GB_..._RTGS_...`) requires an adapter for the **CHAPS** network. Without the `<country-code>`, the system cannot select the correct infrastructure port, guaranteeing a runtime failure.

#### `<business-process>`
*   **Why it is Essential:** It defines the **core business intent** and the primary domain logic sequence.
*   **Litmus Test:** A `TRANSFER` involves a simple debit and credit. A `BILL_PAYMENT` requires an additional, mandatory orchestration step: **biller directory validation**. A handler dispatched for a `TRANSFER` that receives a `BILL_PAYMENT` will fail to execute the necessary validation, leading to failed payments.

#### `<fx-dimension>`
*   **Why it is Essential:** It defines the **foreign exchange requirement**, which fundamentally alters the orchestration graph.
*   **Litmus Test:** A domestic payment (`..._LCY_LCY_...`) is a simple orchestration. An FX payment (`..._LCY_FCY_...`) requires several new, mandatory states in its orchestration: **Get FX Quote, Reserve FX Rate, Execute Trade**. A handler built for the former cannot execute the latter. They are fundamentally different standard journeys.

#### `<clearing-network>`
*   **Why it is Essential:** It defines the **external settlement infrastructure** and the finality characteristics of the payment.
*   **Litmus Test:** A batch payment in India (`IN_..._ACH_...`) uses an orchestration that submits the payment and terminates, expecting deferred settlement. A real-time payment (`IN_..._INSTANT_PAY_...`) requires an orchestration that holds, polls for a final status, and handles real-time timeouts. They are different state machines.

#### `<source-instrument>` & `<destination-instrument>`
*   **Why they are Essential:** They define the **contract for the debit and credit legs** of the transaction. They must be separate.
*   **Litmus Test:** Debiting a `CASA` account is a standard operation. "Debiting" a `CREDIT_CARD` to fund a transfer is a **Cash Advance**, which requires checking a different limit and applying a different fee structure. A handler built for CASA debits cannot handle a cash advance. Likewise, crediting a `CASA` account requires a different adapter than crediting a `QR_CODE`, which must be resolved first.

## 6. Exhaustive Classification Tables

These tables provide a comprehensive, though not exhaustive, list of potential values for each component, based on a modern global banking portfolio.

#### 1. Country Code (`<country-code>`)
| Code | Description |
| :--- | :--- |
| `SG` | Singapore |
| `HK` | Hong Kong |
| `CN` | China |
| `IN` | India |
| `AE` | United Arab Emirates |
| `NG` | Nigeria |
| `GH` | Ghana |
| `*` | Wildcard for global journeys |

#### 2. Business Process (`<business-process>`)
| Code               | Description |
|:-------------------| :--- |
| `TRANSFER`         | Movement of funds between two parties. |
| `BILLPAY`      | Payment to a registered biller or utility. |
| `MERCHPAY` | Payment to a commercial entity for goods/services. |
| `TOPUP`           | Adding funds to a stored-value facility. |
| `CARDPAY`     | Payment specifically to a credit card account. |
| `CASHADV`     | Withdrawing cash from a credit facility. |
| `BALREF`   | Refunding excess balance from a credit facility. |
| `LOANPAY`     | Servicing a loan (mortgage, auto, personal). |

#### 3. FX Dimension (`<fx-dimension>`)
| Code   | Description |
|:-------| :--- |
| `DOM`  | Domestic: Local Currency to Local Currency. |
| `FCYP` | FX Purchase: Local Currency to Foreign Currency. |
| `FCYS` | FX Settlement: Foreign Currency to Local Currency. |
| `FCYX` | Cross-Currency: Foreign Currency to another Foreign Currency. |
| `SFCY` | Foreign Pass-Through: Foreign Currency to same Foreign Currency. |

#### 4. Clearing Network (`<clearing-network>`)
| Code        | Description |
|:------------| :--- |
| `ONUS`      | On-us, internal ledger settlement. |
| `ACH`       | Automated Clearing House (Generic Batch). |
| `RTGS`      | Real-Time Gross Settlement (Generic High-Value). |
| `FAST`      | Generic Real-time low-value network. |
| `UPI`       | India's Unified Payments Interface. |
| `SWIFT`     | International SWIFT network for cross-border payments. |
| `CARDRAILS` | Card networks (Visa, Mastercard, Amex) for settlement. |

#### 5. & 6. Source / Destination Instrument (`<source-instrument>`, `<destination-instrument>`)
| Code          | Description |
|:--------------| :--- |
| `ACC`         | Current Account / Savings Account. |
| `CREDITCARD`  | Credit Card Account. |
| `DEBITCARD`   | Debit Card Number. |
| `WALLET`      | 3rd Party Digital Wallet (e.g., PayPal, Alipay). |
| `MOBILEMONEY` | Telco-provided mobile wallet (e.g., M-Pesa). |
| `IBAN`        | International Bank Account Number. |
| `QRCODE`      | A static or dynamic QR code representing an account. |
| `LOANACCT`    | A loan account. |

## 7. How It Works in Practice

1.  **API Request:** An incoming API request contains a business-friendly `journeyName` (e.g., `"journeyName": "ghana-onetime-transfer"`).
2.  **Configuration Mapping:** A central configuration file (e.g., `application.yml`) maps this business name to its formal, canonical `journey-type`.
    ```yaml
    journeys:
      ghana-onetime-transfer:
        journey-type: "GH_TRANSFER_DOM_ONUS_ACC_ACC"
        # ... other configuration
    ```
3.  **Enrichment:** A middleware component reads this configuration based on the `journeyName` and attaches the formal `journey-type` string to the internal `Command` object being processed.
4.  **Handler Dispatch:** The Command Bus iterates through all available `CommandHandler` beans. Each handler declaratively specifies the `journey-type` patterns it is responsible for (e.g., via an annotation `@HandlesJourneyType("*-TRANSFER-LCY_LCY-INTERNAL-CASA-CASA")`). The first handler that provides a positive match for the command's `journey-type` is selected.
5.  **Execution:** The selected handler executes its orchestration logic, confident that it has the correct contract and that all necessary infrastructure is available for this specific class of payment.

## 8. Conclusion

This six-part composite key provides a robust, scalable, and unambiguous foundation for our payment architecture. It enforces a clean separation of concerns, allowing the system to be extended with new payment types by adding new data and handlers, rather than by modifying complex, brittle code. It is the definitive standard for identifying payment orchestrations.
