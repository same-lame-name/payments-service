package dexter.banking.limit.web.dto;

import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;

@Data
public class AddressPatch {
    private JsonNullable<String> city = JsonNullable.undefined();
    private JsonNullable<String> zip = JsonNullable.undefined();
}
