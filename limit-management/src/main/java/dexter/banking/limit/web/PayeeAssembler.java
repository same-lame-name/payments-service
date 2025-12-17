package dexter.banking.limit.web;

import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PayeeAssembler extends BaseJsonApiAssembler<PayeeDto> {

    public PayeeAssembler(PagedResourcesAssembler<PayeeDto> pagedResourcesAssembler) {
        super(PayeeController.class, PayeeDto.class, pagedResourcesAssembler);
    }

    @Override
    protected Link getSelfLink(PayeeDto dto) {
        return linkTo(methodOn(PayeeController.class).getOne(dto.getId())).withSelfRel();
    }
}