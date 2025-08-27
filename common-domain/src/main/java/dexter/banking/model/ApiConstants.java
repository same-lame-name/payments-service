package dexter.banking.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiConstants {

    public static final String API_CREDIT_CARD_BANKING = "api/v1/credit-card-banking";
    public static final String API_DEPOSIT_BANKING = "api/v1/deposit-banking";
    public static final String API_LIMIT_MANAGEMENT = "api/v1/limit-management";

    public static final String GET_TRANSACTION_INFO = "api/v1/book-transfers/payment/{id}";
    public static final String SUBMIT_TRANSACTION = "api/v1/book-transfers/payment";

    public static final String GET_TRANSACTION_INFO_V2 = "api/v2/book-transfers/payment/{id}";
    public static final String SUBMIT_TRANSACTION_V2 = "api/v2/book-transfers/payment";
}
