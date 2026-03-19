package org.example.ecommerce.users.mapper;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface PaymentCardMapper {

    PaymentCard toEntity(PaymentCardRequest request);

    void updateEntity(PaymentCardRequest request, @MappingTarget PaymentCard entity);

    PaymentCardResponse toResponse(PaymentCard card);

}
