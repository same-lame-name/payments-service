package dexter.banking.limit.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;
import dexter.banking.limit.pipeline.core.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonApiTypeForClass("payees")
public class PayeeDto extends BaseRequest<PayeeDto> {
    @JsonApiId
    private String id;
    private String name;
    private String iban;
    private String city;
    private String zip;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private LocalDate dob;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private OffsetDateTime createdAt;

    @JsonIgnore
    private String derivedScheme;

    @JsonIgnore
    private BankDetails enrichedBankDetails;

    @JsonIgnore
    private String idempotencyKey;

    @JsonIgnore
    @Override
    public String getJourneyIdentifier() {
         return derivedScheme;
     }
}