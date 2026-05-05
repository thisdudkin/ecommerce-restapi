package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.response.OrderItemResponse;
import org.example.ecommerce.orders.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(
    config = CentralMapperConfig.class
)
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemPrice", source = "item.price")
    @Mapping(target = "subtotal", source = ".", qualifiedByName = "toSubtotal")
    OrderItemResponse toResponse(OrderItem orderItem);

    @Named("toSubtotal")
    default BigDecimal toSubtotal(OrderItem orderItem) {
        if (orderItem == null || orderItem.getItem() == null || orderItem.getItem().getPrice() == null)
            return BigDecimal.ZERO;

        return orderItem.getItem().getPrice()
            .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }

}
