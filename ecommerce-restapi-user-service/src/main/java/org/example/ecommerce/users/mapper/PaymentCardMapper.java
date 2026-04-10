package org.example.ecommerce.users.mapper;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING
)
public interface PaymentCardMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    PaymentCard toEntity(PaymentCardRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(PaymentCardRequest request, @MappingTarget PaymentCard entity);

    PaymentCardResponse toResponse(PaymentCard card);

}
