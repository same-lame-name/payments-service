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

    // 1. Entity <-> DTO Mappings
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
    default void flattenAddress(UpdatePayeePatch patch, @MappingTarget PayeeDto dto) {
        // Container Logic: Handle the Tri-State of the Address Object itself
        JsonNullable<AddressPatch> addressNullable = patch.getAddress();

        if (addressNullable != null && addressNullable.isPresent()) {
            AddressPatch address = addressNullable.get();

            if (address == null) {
                // Case 1: "address": null -> Explicitly delete flattened fields
                dto.setCity(null);
                dto.setZip(null);
            } else {
                // Case 2: "address": { ... } -> Delegate to MapStruct to map inner fields
                updateDtoFromAddress(address, dto);
            }
        }
    }

    @Mapping(target = "city", source = "city")
    @Mapping(target = "zip", source = "zip")
    void updateDtoFromAddress(AddressPatch address, @MappingTarget PayeeDto dto);


    // 4. Patch -> Entity Mappings
    @Mapping(target = "address", source = "address")
    void updateEntityFromPatch(UpdatePayeePatch patch, @MappingTarget Payee entity);

    void updateAddressFromPatch(AddressPatch patch, @MappingTarget Address address);
}