package org.example.ecommerce.users.mapper;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.example.ecommerce.users.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardMapperTests {

    private final PaymentCardMapper paymentCardMapper = Mappers.getMapper(PaymentCardMapper.class);

    @Test
    void toEntityShouldMapRequestToEntity() {
        // Arrange
        PaymentCardRequest request = new PaymentCardRequest(
            "4111 1111 1111 1111",
            "JOHN DOE",
            LocalDate.of(2030, 12, 31)
        );

        // Act
        PaymentCard entity = paymentCardMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getNumber()).isEqualTo(request.number());
        assertThat(entity.getHolder()).isEqualTo(request.holder());
        assertThat(entity.getExpirationDate()).isEqualTo(request.expirationDate());

        assertThat(entity.getId()).isNull();
        assertThat(entity.getUser()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();

        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void updateEntityShouldUpdateExistingEntity() {
        // Arrange
        PaymentCardRequest request = new PaymentCardRequest(
            "5555 5555 5555 5555",
            "JANE DOE",
            LocalDate.of(2031, 1, 31)
        );

        User user = new User();
        user.setId(100L);

        PaymentCard entity = new PaymentCard();
        entity.setId(10L);
        entity.setUser(user);
        entity.setNumber("old-number");
        entity.setHolder("old-holder");
        entity.setExpirationDate(LocalDate.of(2028, 5, 20));
        entity.setActive(false);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 10, 0));

        // Act
        paymentCardMapper.updateEntity(request, entity);

        // Assert
        assertThat(entity.getNumber()).isEqualTo(request.number());
        assertThat(entity.getHolder()).isEqualTo(request.holder());
        assertThat(entity.getExpirationDate()).isEqualTo(request.expirationDate());

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getActive()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 0));
    }

    @Test
    void toResponseShouldMapEntityToResponse() {
        // Arrange
        PaymentCard entity = new PaymentCard();
        entity.setId(11L);
        entity.setNumber("4000 0000 0000 0002");
        entity.setHolder("ALEX DUDKIN");
        entity.setExpirationDate(LocalDate.of(2032, 6, 30));
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.of(2024, 3, 10, 12, 15));
        entity.setUpdatedAt(LocalDateTime.of(2024, 3, 11, 13, 20));

        // Act
        PaymentCardResponse response = paymentCardMapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.number()).isEqualTo(entity.getNumber());
        assertThat(response.holder()).isEqualTo(entity.getHolder());
        assertThat(response.expirationDate()).isEqualTo(entity.getExpirationDate());
        assertThat(response.active()).isEqualTo(entity.getActive());
        assertThat(response.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(entity.getUpdatedAt());
    }
}
