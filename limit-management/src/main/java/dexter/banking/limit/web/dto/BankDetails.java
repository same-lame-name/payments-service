package dexter.banking.limit.web.dto;

public record BankDetails(
    String bic,
    String bankName
) {}