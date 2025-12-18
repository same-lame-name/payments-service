package dexter.banking.limit.web.dto;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;
import dexter.banking.limit.pipeline.core.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonApiTypeForClass("payees")
public class UpdatePayeePatch extends BaseRequest<UpdatePayeePatch> {

    @JsonApiId
    private String id;

    private JsonNullable<String> name = JsonNullable.undefined();
    private JsonNullable<String> iban = JsonNullable.undefined();
    private JsonNullable<String> city = JsonNullable.undefined();
    private JsonNullable<String> zip = JsonNullable.undefined();
    private JsonNullable<LocalDate> dob = JsonNullable.undefined();

    @Override
    public String getJourneyIdentifier() {
        return "PAYEE-UPDATE";
    }
}