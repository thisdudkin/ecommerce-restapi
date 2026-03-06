package org.example.ecommerce.users.service;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.exception.custom.PaymentCardAlreadyActiveException;
import org.example.ecommerce.users.exception.custom.PaymentCardAlreadyInactiveException;
import org.example.ecommerce.users.exception.custom.PaymentCardNotFoundException;
import org.example.ecommerce.users.exception.custom.PaymentCardNumberAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.PaymentCardOwnershipException;
import org.example.ecommerce.users.exception.custom.UserNotFoundException;
import org.example.ecommerce.users.exception.custom.UserPaymentCardsLimitExceededException;
import org.example.ecommerce.users.mapper.PaymentCardMapper;
import org.example.ecommerce.users.repository.PaymentCardRepository;
import org.example.ecommerce.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.example.ecommerce.users.utils.TestDataGenerator.id;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCard;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTests {

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentCardService cardService;

    @Test
    void getByIdShouldReturnCardResponse() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);

        PaymentCardResponse expected = paymentCardResponse();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(expected);

        PaymentCardResponse actual = cardService.getById(userId, cardId);

        assertEquals(expected, actual);
        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardMapper).toResponse(card);
    }

    @Test
    void getByIdShouldThrowWhenCardNotFound() {
        Long userId = id();
        Long cardId = id();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        assertThrows(PaymentCardNotFoundException.class, () -> cardService.getById(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
    }

    @Test
    void getByIdShouldThrowWhenCardDoesNotBelongToUser() {
        Long userId = id();
        Long anotherUserId = id();
        Long cardId = id();

        User owner = user(anotherUserId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        assertThrows(PaymentCardOwnershipException.class, () -> cardService.getById(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
    }

    @Test
    void getAllByUserIdShouldReturnCardResponses() {
        Long userId = id();

        PaymentCard card1 = paymentCard();
        PaymentCard card2 = paymentCard();

        PaymentCardResponse response1 = paymentCardResponse();
        PaymentCardResponse response2 = paymentCardResponse();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findAllByUserId(userId)).thenReturn(List.of(card1, card2));
        when(cardMapper.toResponse(card1)).thenReturn(response1);
        when(cardMapper.toResponse(card2)).thenReturn(response2);

        List<PaymentCardResponse> actual = cardService.getAllByUserId(userId);

        assertEquals(2, actual.size());
        assertEquals(response1, actual.get(0));
        assertEquals(response2, actual.get(1));

        verify(userRepository).existsById(userId);
        verify(cardRepository).findAllByUserId(userId);
        verify(cardMapper).toResponse(card1);
        verify(cardMapper).toResponse(card2);
    }

    @Test
    void updateShouldUpdateCardWhenNumberNotChanged() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard existing = paymentCard(cardId);
        existing.setUser(owner);

        PaymentCardRequest request = new PaymentCardRequest(
            existing.getNumber(),
            existing.getHolder(),
            existing.getExpirationDate()
        );

        PaymentCardResponse expected = paymentCardResponse();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(existing));
        when(cardMapper.toResponse(existing)).thenReturn(expected);

        PaymentCardResponse actual = cardService.update(userId, cardId, request);

        assertEquals(expected, actual);
        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cardMapper).updateEntity(request, existing);
        verify(cardMapper).toResponse(existing);
    }

    @Test
    void updateShouldUpdateCardWhenNumberChangedAndUnique() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard existing = paymentCard(cardId);
        existing.setUser(owner);

        PaymentCardRequest request = paymentCardRequest();
        PaymentCardResponse expected = paymentCardResponse();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(existing));
        when(cardRepository.existsByNumber(request.number())).thenReturn(false);
        when(cardMapper.toResponse(existing)).thenReturn(expected);

        PaymentCardResponse actual = cardService.update(userId, cardId, request);

        assertEquals(expected, actual);
        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository).existsByNumber(request.number());
        verify(cardMapper).updateEntity(request, existing);
        verify(cardMapper).toResponse(existing);
    }

    @Test
    void updateShouldThrowWhenCardNotFound() {
        Long userId = id();
        Long cardId = id();
        PaymentCardRequest request = paymentCardRequest();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        assertThrows(PaymentCardNotFoundException.class, () -> cardService.update(userId, cardId, request));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cardMapper, never()).updateEntity(any(), any());
    }

    @Test
    void updateShouldThrowWhenCardDoesNotBelongToUser() {
        Long userId = id();
        Long anotherUserId = id();
        Long cardId = id();

        User owner = user(anotherUserId);
        PaymentCard existing = paymentCard(cardId);
        existing.setUser(owner);

        PaymentCardRequest request = paymentCardRequest();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(existing));

        assertThrows(PaymentCardOwnershipException.class,
            () -> cardService.update(userId, cardId, request));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cardMapper, never()).updateEntity(any(), any());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    void updateShouldThrowWhenNewNumberAlreadyExists() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard existing = paymentCard(cardId);
        existing.setUser(owner);

        PaymentCardRequest request = paymentCardRequest();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(existing));
        when(cardRepository.existsByNumber(request.number())).thenReturn(true);

        assertThrows(PaymentCardNumberAlreadyExistsException.class,
            () -> cardService.update(userId, cardId, request));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository).existsByNumber(request.number());
        verify(cardMapper, never()).updateEntity(any(), any());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    void activateShouldSetCardActive() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(false);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        cardService.activate(userId, cardId);

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository).updateActiveStatus(cardId, true);
    }

    @Test
    void activateShouldThrowWhenCardNotFound() {
        Long userId = id();
        Long cardId = id();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        assertThrows(PaymentCardNotFoundException.class, () -> cardService.activate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void activateShouldThrowWhenCardDoesNotBelongToUser() {
        Long userId = id();
        Long anotherUserId = id();
        Long cardId = id();

        User owner = user(anotherUserId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(false);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        assertThrows(PaymentCardOwnershipException.class, () -> cardService.activate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void activateShouldThrowWhenCardAlreadyActive() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(true);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        assertThrows(PaymentCardAlreadyActiveException.class, () -> cardService.activate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void deactivateShouldSetCardInactive() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(true);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        cardService.deactivate(userId, cardId);

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository).updateActiveStatus(cardId, false);
    }

    @Test
    void deactivateShouldThrowWhenCardNotFound() {
        Long userId = id();
        Long cardId = id();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        assertThrows(PaymentCardNotFoundException.class, () -> cardService.deactivate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void deactivateShouldThrowWhenCardDoesNotBelongToUser() {
        Long userId = id();
        Long anotherUserId = id();
        Long cardId = id();

        User owner = user(anotherUserId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(true);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        assertThrows(PaymentCardOwnershipException.class, () -> cardService.deactivate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void deactivateShouldThrowWhenCardAlreadyInactive() {
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);
        card.setActive(false);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        assertThrows(PaymentCardAlreadyInactiveException.class, () -> cardService.deactivate(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void createShouldCreateCardWhenUserExistsAndNumberUnique() {
        // Arrange
        Long userId = id();

        User owner = user(userId);
        PaymentCardRequest request = paymentCardRequest();
        PaymentCard card = paymentCard();
        PaymentCard savedCard = paymentCard();
        PaymentCardResponse expected = paymentCardResponse();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(owner));
        when(cardRepository.existsByNumber(request.number())).thenReturn(false);
        when(cardMapper.toEntity(request)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(savedCard);
        when(cardMapper.toResponse(savedCard)).thenReturn(expected);

        // Act
        PaymentCardResponse actual = cardService.create(userId, request);

        // Assert
        assertEquals(expected, actual);
        verify(userRepository).findByIdForUpdate(userId);
        verify(cardRepository).existsByNumber(request.number());
        verify(cardMapper).toEntity(request);
        verify(cardRepository).save(card);
        verify(cardMapper).toResponse(savedCard);
    }

    @Test
    void createShouldThrowWhenUserNotFound() {
        // Arrange
        Long userId = id();
        PaymentCardRequest request = paymentCardRequest();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> cardService.create(userId, request));

        verify(userRepository).findByIdForUpdate(userId);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cardRepository, never()).save(any());
        verify(cardMapper, never()).toEntity(any());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    void createShouldThrowWhenUserCardsLimitExceeded() {
        // Arrange
        Long userId = id();

        User owner = user(userId);
        for (int i = 0; i < 5; i++) {
            owner.addCard(paymentCard());
        }

        PaymentCardRequest request = paymentCardRequest();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThrows(UserPaymentCardsLimitExceededException.class, () -> cardService.create(userId, request));

        verify(userRepository).findByIdForUpdate(userId);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cardRepository, never()).save(any());
        verify(cardMapper, never()).toEntity(any());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    void createShouldThrowWhenCardNumberAlreadyExists() {
        // Arrange
        Long userId = id();

        User owner = user(userId);
        PaymentCardRequest request = paymentCardRequest();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(owner));
        when(cardRepository.existsByNumber(request.number())).thenReturn(true);

        // Act & Assert
        assertThrows(PaymentCardNumberAlreadyExistsException.class, () -> cardService.create(userId, request));

        verify(userRepository).findByIdForUpdate(userId);
        verify(cardRepository).existsByNumber(request.number());
        verify(cardRepository, never()).save(any());
        verify(cardMapper, never()).toEntity(any());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    void deleteShouldDeleteCardWhenOwnedByUser() {
        // Arrange
        Long userId = id();
        Long cardId = id();

        User owner = user(userId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        // Act
        cardService.delete(userId, cardId);

        // Assert
        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteShouldThrowWhenCardNotFound() {
        // Arrange
        Long userId = id();
        Long cardId = id();

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentCardNotFoundException.class, () -> cardService.delete(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }

    @Test
    void deleteShouldThrowWhenCardDoesNotBelongToUser() {
        // Arrange
        Long userId = id();
        Long anotherUserId = id();
        Long cardId = id();

        User owner = user(anotherUserId);
        PaymentCard card = paymentCard(cardId);
        card.setUser(owner);

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));

        // Act & Assert
        assertThrows(PaymentCardOwnershipException.class, () -> cardService.delete(userId, cardId));

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }

}
