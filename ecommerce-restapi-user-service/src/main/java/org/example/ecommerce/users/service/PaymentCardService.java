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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.example.ecommerce.users.config.RedisCacheConfig.USER_WITH_CARDS;

@Service
public class PaymentCardService {

    private final PaymentCardMapper cardMapper;
    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;

    public PaymentCardService(PaymentCardMapper cardMapper,
                              PaymentCardRepository cardRepository,
                              UserRepository userRepository) {
        this.cardMapper = cardMapper;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PaymentCardResponse getById(Long userId, Long cardId) {
        return cardMapper.toResponse(getOwnedCardOrThrow(userId, cardId));
    }

    @Transactional(readOnly = true)
    public List<PaymentCardResponse> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId))
            throw new UserNotFoundException(userId);

        return cardRepository.findAllByUserId(userId).stream()
            .map(cardMapper::toResponse)
            .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#userId")
    public PaymentCardResponse create(Long userId, PaymentCardRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getPaymentCards().size() >= 5)
            throw new UserPaymentCardsLimitExceededException();
        if (cardRepository.existsByNumber(request.number()))
            throw new PaymentCardNumberAlreadyExistsException(request.number());

        PaymentCard card = cardMapper.toEntity(request);
        user.addCard(card);

        PaymentCard saved = cardRepository.save(card);
        return cardMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#userId")
    public PaymentCardResponse update(Long userId, Long cardId, PaymentCardRequest request) {
        PaymentCard existing = getOwnedCardOrThrow(userId, cardId);

        if (!existing.getNumber().equals(request.number()) &&
            cardRepository.existsByNumber(request.number())) {
            throw new PaymentCardNumberAlreadyExistsException(request.number());
        }

        cardMapper.updateEntity(request, existing);

        return cardMapper.toResponse(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#userId")
    public void activate(Long userId, Long cardId) {
        PaymentCard existing = getOwnedCardOrThrow(userId, cardId);

        if (Boolean.TRUE.equals(existing.getActive()))
            throw new PaymentCardAlreadyActiveException(cardId);

        cardRepository.updateActiveStatus(cardId, true);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#userId")
    public void deactivate(Long userId, Long cardId) {
        PaymentCard existing = getOwnedCardOrThrow(userId, cardId);

        if (Boolean.FALSE.equals(existing.getActive()))
            throw new PaymentCardAlreadyInactiveException(cardId);

        cardRepository.updateActiveStatus(cardId, false);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_WITH_CARDS, key = "#userId")
    public void delete(Long userId, Long cardId) {
        cardRepository.delete(getOwnedCardOrThrow(userId, cardId));
    }

    private PaymentCard getOwnedCardOrThrow(Long userId, Long cardId) {
        PaymentCard card = cardRepository.findByIdWithUser(cardId)
            .orElseThrow(() -> new PaymentCardNotFoundException(cardId));

        if (!card.getUser().getId().equals(userId)) {
            throw new PaymentCardOwnershipException(cardId, userId);
        }

        return card;
    }

}
