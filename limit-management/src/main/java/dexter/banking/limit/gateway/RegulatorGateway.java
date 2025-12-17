package dexter.banking.limit.gateway;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.PayeeAssembler;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.mapper.PayeeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RegulatorGateway {

    private final RestTemplate restTemplate;
    private final PayeeMapper payeeMapper;
    private final String regulatorUrl;

    public RegulatorGateway(@Qualifier("jsonApiRestTemplate") RestTemplate restTemplate,
                            PayeeAssembler assembler, PayeeMapper payeeMapper,
                            @Value("${regulator.api.url:http://localhost:8082/mock/regulator/payees}") String regulatorUrl) {
        this.restTemplate = restTemplate;
        this.payeeMapper = payeeMapper;
        this.regulatorUrl = regulatorUrl;
    }

    public List<Payee> fetchPayees() {
        ParameterizedTypeReference<CollectionModel<EntityModel<PayeeDto>>> responseType =
                new ParameterizedTypeReference<>() {};

        try {
            ResponseEntity<CollectionModel<EntityModel<PayeeDto>>> responseEntity =
                    restTemplate.exchange(regulatorUrl, HttpMethod.GET, null, responseType);

            CollectionModel<EntityModel<PayeeDto>> resources = responseEntity.getBody();

            if (resources == null || resources.getContent() == null) {
                return Collections.emptyList();
            }

            return resources.getContent().stream()
                    .map(EntityModel::getContent)
                    .map(payeeMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching regulator payees: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}