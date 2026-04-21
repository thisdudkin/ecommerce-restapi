package org.example.ecommerce.users.service;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.dto.response.UserScrollResponse;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.exception.custom.DuplicatePaymentCardNumbersException;
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
import org.example.ecommerce.users.repository.specifications.UserSpecifications;
import org.example.ecommerce.users.utils.UserCursorCodec;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.example.ecommerce.users.config.RedisCacheConfig.USER_WITH_CARDS;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PaymentCardMapper cardMapper;
    private final UserRepository userRepository;
    private final UserCursorCodec userCursorCodec;
    private final PaymentCardRepository cardRepository;

    public UserService(UserMapper userMapper,
                       UserRepository userRepository,
                       PaymentCardMapper cardMapper,
                       UserCursorCodec userCursorCodec,
                       PaymentCardRepository cardRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.userCursorCodec = userCursorCodec;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = USER_WITH_CARDS, key = "#id")
    public UserResponse getById(Long id) {
        User user = userRepository.findByIdWithCards(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getByIds(Set<Long> ids) {
        return userRepository.findAllByIdIn(ids).stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserScrollResponse getAll(String name,
                                     String surname,
                                     int size,
                                     SortDirection direction,
                                     String cursor) {
        UserCursorPayload cursorPayload = (cursor == null || cursor.isBlank())
            ? null
            : userCursorCodec.decode(cursor, direction);

        List<User> users = userRepository.findPageWithCards(
            UserSpecifications.withFilters(name, surname),
            size,
            direction,
            cursorPayload
        );

        boolean hasNext = users.size() > size;
        if (hasNext) {
            users = List.copyOf(users.subList(0, size));
        }

        List<UserResponse> items = users.stream()
            .map(userMapper::toResponse)
            .toList();

        String nextCursor = null;
        if (!items.isEmpty() && hasNext) {
            UserResponse last = items.getLast();
            nextCursor = userCursorCodec.encode(last.createdAt(), last.id(), direction);
        }

        return new UserScrollResponse(items, hasNext, nextCursor);
    }

    @Transactional
    public UserResponse create(UserRequest request) throws DataIntegrityViolationException {
        List<PaymentCardRequest> cards = request.paymentCards() == null
            ? List.of()
            : request.paymentCards();

        if (cards.size() > 5)
            throw new UserPaymentCardsLimitExceededException();
        Set<String> uniqueNumbers = cards.stream()
            .map(PaymentCardRequest::number)
            .collect(java.util.stream.Collectors.toSet());

        if (uniqueNumbers.size() != cards.size())
            throw new DuplicatePaymentCardNumbersException();

        if (userRepository.existsByEmail(request.email()))
            throw new UserEmailAlreadyExistsException(request.email());

        List<String> numbers = cards.stream()
            .map(PaymentCardRequest::number)
            .toList();

        if (!numbers.isEmpty()) {
            Set<String> existing = cardRepository.findExistingNumbers(numbers);
            if (!existing.isEmpty()) {
                throw new PaymentCardNumberAlreadyExistsException(existing);
            }
        }

        User toSave = userMapper.toEntity(request);
        cards.stream()
            .map(cardMapper::toEntity)
            .forEach(toSave::addCard);

        User saved = userRepository.save(toSave);
        return userMapper.toResponse(saved);
    }

    @Transactional
    @CachePut(cacheNames = USER_WITH_CARDS, key = "#id")
    public UserResponse update(Long id, UserUpdateRequest request) {
        User existing = getUserOrThrow(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id))
            throw new UserEmailAlreadyExistsException(request.email());

        userMapper.updateEntity(request, existing);
        return userMapper.toResponse(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#id")
    public void delete(Long id) {
        userRepository.delete(getUserOrThrow(id));
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#id")
    public void activate(Long id) {
        User user = getUserOrThrow(id);

        if (Boolean.TRUE.equals(user.getActive()))
            throw new UserAlreadyActiveException(id);

        userRepository.updateActiveStatus(id, true);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#id")
    public void deactivate(Long id) {
        User user = getUserOrThrow(id);

        if (Boolean.FALSE.equals(user.getActive()))
            throw new UserAlreadyInactiveException(id);

        userRepository.updateActiveStatus(id, false);
        cardRepository.deactivateAll(id);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

}
