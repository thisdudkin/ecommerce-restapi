package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.entity.Item;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    config = CentralMapperConfig.class
)
public interface ItemMapper {

    Item toEntity(ItemCreateRequest request);

    ItemResponse toResponse(Item item);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ItemUpdateRequest request, @MappingTarget Item item);

}
