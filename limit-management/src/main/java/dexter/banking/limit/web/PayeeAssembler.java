package dexter.banking.limit.web;

import dexter.banking.limit.domain.Address;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.link.LinkToggleService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PayeeAssembler extends RepresentationModelAssemblerSupport<Payee, EntityModel<PayeeDto>> {

    private final LinkToggleService linkToggleService;

    public PayeeAssembler(LinkToggleService linkToggleService) {
        super(PayeeController.class, (Class<EntityModel<PayeeDto>>) (Class<?>) EntityModel.class);
        this.linkToggleService = linkToggleService;
    }

    @Override
    public EntityModel<PayeeDto> toModel(Payee entity) {
        PayeeDto dto = new PayeeDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIban(entity.getIban());
        dto.setDob(entity.getDob());
        dto.setCreatedAt(entity.getCreatedAt());
        if (entity.getAddress() != null) {
            dto.setCity(entity.getAddress().getCity());
            dto.setZip(entity.getAddress().getZip());
        }
        
        EntityModel<PayeeDto> model = EntityModel.of(dto);

        if (linkToggleService.isLinksEnabled()) {
            model.add(linkTo(methodOn(PayeeController.class).getOne(entity.getId())).withSelfRel());
        }

        return model;
    }

    public Payee toDomain(PayeeDto dto) {
        Address address = new Address(dto.getCity(), dto.getZip());
        return new Payee(dto.getId(), dto.getName(), dto.getIban(), dto.getDob(), dto.getCreatedAt(), address);
    }
}