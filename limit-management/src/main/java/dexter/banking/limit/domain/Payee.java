package dexter.banking.limit.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payee {
    private String id;
    private String name;
    private String iban;
}