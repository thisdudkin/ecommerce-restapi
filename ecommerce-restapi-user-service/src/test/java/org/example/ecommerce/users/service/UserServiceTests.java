package org.example.ecommerce.users.service;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.dto.response.UserScrollResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.exception.custom.PaymentCardNumberAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.UserAlreadyActiveException;
import org.example.ecommerce.users.exception.custom.UserAlreadyInactiveException;
import org.example.ecommerce.users.exception.custom.UserEmailAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.UserNotFoundException;
import org.example.ecommerce.users.exception.custom.UserPaymentCardsLimitExceededException;
import org.example.ecommerce.users.mapper.PaymentCardMapper;
import org.example.ecommerce.users.mapper.UserMapper;
import org.example.ecommerce.users.repository.PaymentCardRepository;
import org.example.ecommerce.users.repository.UserRepository;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.utils.UserCursorCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.example.ecommerce.users.utils.TestDataGenerator.datetime;
import static org.example.ecommerce.users.utils.TestDataGenerator.id;
import static org.example.ecommerce.users.utils.TestDataGenerator.name;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCard;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardRequests;
import static org.example.ecommerce.users.utils.TestDataGenerator.surname;
import static org.example.ecommerce.users.utils.TestDataGenerator.user;
import static org.example.ecommerce.users.utils.TestDataGenerator.userListResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.userRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.userResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.userUpdateRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserCursorCodec userCursorCodec;

    @InjectMocks
    private UserService userService;

    @Test
    void getByIdShouldReturnUserResponse() {
        Long userId = id();
        User user = user(userId);
        UserResponse expected = userResponse();

        when(userRepository.findByIdWithCards(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        UserResponse actual = userService.getById(userId);

        assertEquals(expected, actual);
        verify(userRepository).findByIdWithCards(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    void getByIdShouldThrowWhenUserNotFound() {
        Long userId = id();
        when(userRepository.findByIdWithCards(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(userId));
        verify(userRepository).findByIdWithCards(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnFirstWindowWhenCursorIsNull() {
        String name = name();
        String surname = surname();
        int size = 2;
        SortDirection direction = SortDirection.DESC;

        User user1 = user(1L);
        User user2 = user(2L);

        UserListResponse response1 = userListResponse(user1);
        UserListResponse response2 = userListResponse(user2);

        Window<User> window = mock(Window.class);
        when(window.stream()).thenReturn(Stream.of(user1, user2));
        when(window.hasNext()).thenReturn(false);

        when(userRepository.findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        )).thenReturn(window);

        when(userMapper.toListResponse(user1)).thenReturn(response1);
        when(userMapper.toListResponse(user2)).thenReturn(response2);

        UserScrollResponse actual = userService.getAll(name, surname, size, direction, null);

        assertEquals(List.of(response1, response2), actual.items());
        assertFalse(actual.hasNext());
        assertNull(actual.nextCursor());

        verify(userRepository).findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        );
        verify(userMapper).toListResponse(user1);
        verify(userMapper).toListResponse(user2);
        verify(userCursorCodec, never()).decode(anyString(), any());
        verify(userCursorCodec, never()).encode(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldUseKeysetWhenCursorIsBlank() {
        int size = 1;
        SortDirection direction = SortDirection.DESC;
        User user = user(1L);
        UserListResponse response = userListResponse(user);

        Window<User> window = mock(Window.class);
        when(window.stream()).thenReturn(Stream.of(user));
        when(window.hasNext()).thenReturn(false);

        when(userRepository.findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        )).thenReturn(window);

        when(userMapper.toListResponse(user)).thenReturn(response);

        UserScrollResponse actual = userService.getAll(null, null, size, direction, "   ");

        assertEquals(List.of(response), actual.items());
        assertFalse(actual.hasNext());
        assertNull(actual.nextCursor());

        verify(userRepository).findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        );
        verify(userCursorCodec, never()).decode(anyString(), any());
        verify(userCursorCodec, never()).encode(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldDecodeCursorWhenCursorProvided() {
        int size = 1;
        String cursor = "encoded-cursor";
        SortDirection direction = SortDirection.ASC;

        ScrollPosition decodedPosition = ScrollPosition.forward(
            Map.of(
                "createdAt", datetime(),
                "id", 10L
            )
        );

        User user = user(11L);
        UserListResponse response = userListResponse(user);

        Window<User> window = mock(Window.class);
        when(window.stream()).thenReturn(Stream.of(user));
        when(window.hasNext()).thenReturn(false);

        when(userCursorCodec.decode(cursor, direction)).thenReturn(decodedPosition);
        when(userRepository.findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(decodedPosition)
        )).thenReturn(window);
        when(userMapper.toListResponse(user)).thenReturn(response);

        UserScrollResponse actual = userService.getAll(null, null, size, direction, cursor);

        assertEquals(List.of(response), actual.items());
        assertFalse(actual.hasNext());
        assertNull(actual.nextCursor());

        verify(userCursorCodec).decode(cursor, direction);
        verify(userRepository).findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(decodedPosition)
        );
        verify(userMapper).toListResponse(user);
        verify(userCursorCodec, never()).encode(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnNextCursorWhenWindowHasNext() {
        int size = 2;
        SortDirection direction = SortDirection.DESC;

        User user1 = user(1L);
        User user2 = user(2L);

        UserListResponse response1 = userListResponse(user1);
        UserListResponse response2 = userListResponse(user2);

        Window<User> window = mock(Window.class);
        when(window.stream()).thenReturn(Stream.of(user1, user2));
        when(window.hasNext()).thenReturn(true);

        when(userRepository.findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        )).thenReturn(window);

        when(userMapper.toListResponse(user1)).thenReturn(response1);
        when(userMapper.toListResponse(user2)).thenReturn(response2);
        when(userCursorCodec.encode(response2.createdAt(), response2.id(), direction))
            .thenReturn("next-cursor");

        UserScrollResponse actual = userService.getAll(null, null, size, direction, null);

        assertEquals(List.of(response1, response2), actual.items());
        assertTrue(actual.hasNext());
        assertEquals("next-cursor", actual.nextCursor());

        verify(userCursorCodec).encode(response2.createdAt(), response2.id(), direction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnEmptyResultWhenWindowIsEmpty() {
        int size = 3;
        SortDirection direction = SortDirection.DESC;

        Window<User> window = mock(Window.class);
        when(window.stream()).thenReturn(Stream.of());
        when(window.hasNext()).thenReturn(false);

        when(userRepository.findWindow(
            any(Specification.class),
            eq(PageRequest.of(0, size, UserRepository.keysetSort(direction))),
            eq(ScrollPosition.keyset())
        )).thenReturn(window);

        UserScrollResponse actual = userService.getAll(null, null, size, direction, null);

        assertEquals(List.of(), actual.items());
        assertFalse(actual.hasNext());
        assertNull(actual.nextCursor());

        verify(userMapper, never()).toListResponse(any());
        verify(userCursorCodec, never()).encode(any(), any(), any());
    }

    @Test
    void createShouldCreateUserWithoutCards() {
        UserRequest request = userRequest();
        User toSave = user();
        User saved = user();
        UserResponse expected = userResponse();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(toSave);
        when(userRepository.save(toSave)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse actual = userService.create(request);

        assertEquals(expected, actual);
        verify(userRepository).existsByEmail(request.email());
        verify(userMapper).toEntity(request);
        verify(userRepository).save(toSave);
        verify(userMapper).toResponse(saved);
        verifyNoInteractions(cardMapper);
        verify(cardRepository, never()).existsByNumber(anyString());
    }

    @Test
    void createShouldCreateUserWithCards() {
        PaymentCardRequest cardRequest1 = paymentCardRequest();
        PaymentCardRequest cardRequest2 = paymentCardRequest();
        UserRequest request = userRequest(List.of(cardRequest1, cardRequest2));

        User toSave = user();
        User saved = user();
        UserResponse expected = userResponse();

        PaymentCard card1 = paymentCard();
        PaymentCard card2 = paymentCard();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(cardRepository.findExistingNumbers(List.of(cardRequest1.number(), cardRequest2.number())))
            .thenReturn(Set.of());
        when(userMapper.toEntity(request)).thenReturn(toSave);
        when(cardMapper.toEntity(cardRequest1)).thenReturn(card1);
        when(cardMapper.toEntity(cardRequest2)).thenReturn(card2);
        when(userRepository.save(toSave)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse actual = userService.create(request);

        assertEquals(expected, actual);
        assertEquals(2, toSave.getPaymentCards().size());

        verify(userRepository).existsByEmail(request.email());
        verify(cardRepository).findExistingNumbers(List.of(cardRequest1.number(), cardRequest2.number()));
        verify(userMapper).toEntity(request);
        verify(cardMapper).toEntity(cardRequest1);
        verify(cardMapper).toEntity(cardRequest2);
        verify(userRepository).save(toSave);
        verify(userMapper).toResponse(saved);
    }

    @Test
    void createShouldThrowWhenCardsLimitExceeded() {
        List<PaymentCardRequest> cards = paymentCardRequests(6);
        UserRequest request = userRequest(cards);

        assertThrows(UserPaymentCardsLimitExceededException.class, () -> userService.create(request));

        verifyNoInteractions(userRepository, userMapper, cardMapper, cardRepository);
    }

    @Test
    void createShouldThrowWhenEmailAlreadyExists() {
        UserRequest request = userRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserEmailAlreadyExistsException.class, () -> userService.create(request));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper, cardMapper, cardRepository);
    }

    @Test
    void createShouldThrowWhenCardNumberAlreadyExists() {
        PaymentCardRequest cardRequest = paymentCardRequest();
        UserRequest request = userRequest(List.of(cardRequest));

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(cardRepository.findExistingNumbers(List.of(cardRequest.number())))
            .thenReturn(Set.of(cardRequest.number()));

        assertThrows(PaymentCardNumberAlreadyExistsException.class, () -> userService.create(request));

        verify(userRepository).existsByEmail(request.email());
        verify(cardRepository).findExistingNumbers(List.of(cardRequest.number()));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper, cardMapper);
    }

    @Test
    void updateShouldUpdateUser() {
        Long userId = id();
        User user = user(userId);
        UserUpdateRequest request = userUpdateRequest();
        UserResponse expected = userResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(request.email(), userId)).thenReturn(false);
        when(userMapper.toResponse(user)).thenReturn(expected);

        UserResponse actual = userService.update(userId, request);

        assertEquals(expected, actual);
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailAndIdNot(request.email(), userId);
        verify(userMapper).updateEntity(request, user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateShouldThrowWhenUserNotFound() {
        Long userId = id();
        UserUpdateRequest request = userUpdateRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.update(userId, request));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), any());
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateShouldThrowWhenEmailAlreadyExists() {
        Long userId = id();
        User user = user(userId);
        UserUpdateRequest request = userUpdateRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(request.email(), userId)).thenReturn(true);

        assertThrows(UserEmailAlreadyExistsException.class, () -> userService.update(userId, request));

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailAndIdNot(request.email(), userId);
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void deleteShouldDeleteUser() {
        Long userId = id();
        User user = user(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.delete(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteShouldThrowWhenUserNotFound() {
        Long userId = id();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.delete(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void activateShouldSetUserActive() {
        Long userId = id();
        User user = user(userId);
        user.setActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.activate(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).updateActiveStatus(userId, true);
    }

    @Test
    void activateShouldThrowWhenUserNotFound() {
        Long userId = id();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.activate(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void activateShouldThrowWhenUserAlreadyActive() {
        Long userId = id();
        User user = user(userId);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyActiveException.class, () -> userService.activate(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void deactivateShouldSetUserInactive() {
        Long userId = id();
        User user = user(userId);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivate(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).updateActiveStatus(userId, false);
    }

    @Test
    void deactivateShouldThrowWhenUserNotFound() {
        Long userId = id();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deactivate(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateActiveStatus(any(), any());
    }

    @Test
    void deactivateShouldThrowWhenUserAlreadyInactive() {
        Long userId = id();
        User user = user(userId);
        user.setActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyInactiveException.class, () -> userService.deactivate(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateActiveStatus(any(), any());
    }
}
