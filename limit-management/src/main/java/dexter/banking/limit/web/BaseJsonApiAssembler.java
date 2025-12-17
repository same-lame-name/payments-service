package dexter.banking.limit.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base assembler for converting DTOs into HATEOAS EntityModels.
 * This class focuses purely on Hypermedia (links) and Pagination.
 * It assumes the Data Transformation (Entity -> DTO) has already happened.
 *
 * @param <D> The DTO type (must extend RepresentationModel)
 */
public abstract class BaseJsonApiAssembler<D extends RepresentationModel<D>>
        extends RepresentationModelAssemblerSupport<D, EntityModel<D>> {

    @Value("${jsonapi.links.enabled:true}")
    private boolean linksEnabled;

    private final PagedResourcesAssembler<D> pagedResourcesAssembler;

    public BaseJsonApiAssembler(Class<?> controllerClass,
                                Class<D> resourceType,
                                PagedResourcesAssembler<D> pagedResourcesAssembler) {
        super(controllerClass, (Class<EntityModel<D>>) (Class<?>) EntityModel.class);
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    /**
     * Generates the 'self' link for the given DTO.
     * To be implemented by the concrete assembler using type-safe linkTo/methodOn.
     */
    protected abstract Link getSelfLink(D dto);

    /**
     * Converts a single DTO to an EntityModel, conditionally adding the self link.
     */
    @Override
    public final EntityModel<D> toModel(D dto) {
        EntityModel<D> model = EntityModel.of(dto);
        if (linksEnabled) {
            model.add(getSelfLink(dto));
        }
        return model;
    }

    /**
     * Converts a Page of DTOs to a PagedModel, conditionally adding pagination links.
     */
    public PagedModel<EntityModel<D>> toPagedModel(Page<D> page) {
        if (linksEnabled) {
            return pagedResourcesAssembler.toModel(page, this);
        } else {
            // Manual construction without any links
            List<EntityModel<D>> content = page.getContent().stream()
                    .map(this::toModel)
                    .collect(Collectors.toList());

            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                    page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());

            return PagedModel.of(content, metadata);
        }
    }
}