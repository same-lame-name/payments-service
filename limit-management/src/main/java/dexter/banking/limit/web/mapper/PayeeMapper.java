package dexter.banking.limit.web.mapper;

import dexter.banking.limit.domain.Address;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.stereotype.Component;

@Component
public class PayeeMapper {

    public PayeeDto toDto(Payee entity) {
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
        return dto;
    }

    public Payee toDomain(PayeeDto dto) {
        Address address = new Address(dto.getCity(), dto.getZip());
        return new Payee(dto.getId(), dto.getName(), dto.getIban(), dto.getDob(), dto.getCreatedAt(), address);
    }
}