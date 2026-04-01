package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemCreateRequest;

@SpringJUnitConfig(ItemMapperTests.MapperTestConfig.class)
class ItemMapperTests {

    @Configuration
    @ComponentScan(basePackageClasses = ItemMapperImpl.class)
    static class MapperTestConfig {
    }

    @Autowired
    private ItemMapper mapper;

    @Test
    void toEntityMapsCreateRequestToItem() {
        ItemCreateRequest request = itemCreateRequest();

        Item result = mapper.toEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getName()).isEqualTo(request.name());
        assertThat(result.getPrice()).isEqualByComparingTo(request.price());
        assertThat(result.getArchived()).isFalse();
    }

    @Test
    void toResponseMapsItemToResponse() {
        Item source = item(100L);

        ItemResponse result = mapper.toResponse(source);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(source.getId());
        assertThat(result.name()).isEqualTo(source.getName());
        assertThat(result.price()).isEqualByComparingTo(source.getPrice());
        assertThat(result.createdAt()).isEqualTo(source.getCreatedAt());
        assertThat(result.updatedAt()).isEqualTo(source.getUpdatedAt());
    }

    @Test
    void updateOverwritesNonNullFields() {
        Item target = item(100L, "Old item", BigDecimal.valueOf(10.00));
        ItemUpdateRequest request = new ItemUpdateRequest("New item", BigDecimal.valueOf(25.50));

        mapper.update(request, target);

        assertThat(target.getName()).isEqualTo("New item");
        assertThat(target.getPrice()).isEqualByComparingTo("25.50");
    }

    @Test
    void updateIgnoresNullFields() {
        Item target = item(100L, "Old item", new BigDecimal("10.00"));
        ItemUpdateRequest request = new ItemUpdateRequest(null, null);

        mapper.update(request, target);

        assertThat(target.getName()).isEqualTo("Old item");
        assertThat(target.getPrice()).isEqualByComparingTo("10.00");
    }

}
