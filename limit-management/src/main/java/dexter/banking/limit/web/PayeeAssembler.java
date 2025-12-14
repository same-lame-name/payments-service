package dexter.banking.limit.web;

import dexter.banking.limit.domain.Address;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PayeeAssembler extends RepresentationModelAssemblerSupport<Payee, EntityModel<PayeeDto>> {

    public PayeeAssembler() {
        super(PayeeController.class, (Class<EntityModel<PayeeDto>>) (Class<?>) EntityModel.class);
    }

    @Override
    public EntityModel<PayeeDto> toModel(Payee entity) {
        PayeeDto dto = new PayeeDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIban(entity.getIban());
        if (entity.getAddress() != null) {
            dto.setCity(entity.getAddress().getCity());
            dto.setZip(entity.getAddress().getZip());
        }
        
        return EntityModel.of(dto,
                linkTo(methodOn(PayeeController.class).getOne(entity.getId())).withSelfRel());
    }

    public Payee toDomain(PayeeDto dto) {
        Address address = new Address(dto.getCity(), dto.getZip());
        return new Payee(dto.getId(), dto.getName(), dto.getIban(), address);
    }
}