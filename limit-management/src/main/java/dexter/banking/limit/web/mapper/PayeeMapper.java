package dexter.banking.limit.web.mapper;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    uses = JsonNullableMapper.class,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface PayeeMapper {

    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "zip", source = "address.zip")
    PayeeDto toDto(Payee entity);

    @Mapping(target = "address.city", source = "city")
    @Mapping(target = "address.zip", source = "zip")
    Payee toDomain(PayeeDto dto);

    void updateDtoFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeDto dto);

    @Mapping(target = "address.city", source = "city")
    @Mapping(target = "address.zip", source = "zip")
    void updateEntityFromPatch(UpdatePayeePatch patch, @MappingTarget Payee entity);
}