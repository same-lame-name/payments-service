package dexter.banking.limit.web.dto;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;
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
public class PayeeDto extends RepresentationModel<PayeeDto> {
    @JsonApiId
    private String id;
    private String name;
    private String iban;
    private String city;
    private String zip;
    private LocalDate dob;
    private OffsetDateTime createdAt;
}