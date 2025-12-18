package dexter.banking.limit.web.mapper;

import dexter.banking.limit.domain.Address;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.dto.AddressPatch;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import org.mapstruct.*;
import org.openapitools.jackson.nullable.JsonNullable;

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

    @Mapping(target = "city", ignore = true)
    @Mapping(target = "zip", ignore = true)
    void updateDtoFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeDto dto);

    @AfterMapping
    default void afterUpdateDtoFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeDto dto) {
        if (patch.getAddress() == null || !patch.getAddress().isPresent()) {
            return; // address field not provided in patch, do nothing.
        }

        AddressPatch addressPatch = patch.getAddress().get();

        // If "address": null, clear the address fields in the DTO
        if (addressPatch == null) {
            dto.setCity(null);
            dto.setZip(null);
            return;
        }

        // If "address": { ... }, update fields that are present in the address patch.
        JsonNullable<String> city = addressPatch.getCity();
        if (city != null && city.isPresent()) {
            dto.setCity(city.get()); // city.get() can be null if "city": null
        }

        JsonNullable<String> zip = addressPatch.getZip();
        if (zip != null && zip.isPresent()) {
            dto.setZip(zip.get()); // zip.get() can be null if "zip": null
        }
    }


    @Mapping(target = "address", source = "address")
    void updateEntityFromPatch(UpdatePayeePatch patch, @MappingTarget Payee entity);

    void updateAddressFromPatch(AddressPatch patch, @MappingTarget Address address);
}
