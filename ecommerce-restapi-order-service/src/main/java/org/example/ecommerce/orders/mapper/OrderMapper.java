package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = CentralMapperConfig.class,
    uses = OrderItemMapper.class
)
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "deleted", source = "order.deleted")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "updatedAt", source = "order.updatedAt")
    @Mapping(target = "orderItems", source = "order.orderItems")
    OrderResponse toResponse(Order order, UserResponse user);

}
