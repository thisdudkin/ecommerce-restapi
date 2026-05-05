package org.example.ecommerce.payments.infrastructure.mongo.repository;

import lombok.RequiredArgsConstructor;
import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.repository.PaymentRepository;
import org.example.ecommerce.payments.infrastructure.mapper.PaymentMapper;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MongoPaymentRepository implements PaymentRepository {

    private final PaymentMapper mapper;
    private final PaymentDocumentRepository paymentRepository;

    @Override
    public Payment create(NewPayment newPayment) {
        PaymentDocument entity = new PaymentDocument(
            null,
            newPayment.orderId(),
            newPayment.userId(),
            newPayment.status(),
            newPayment.timestamp(),
            newPayment.amount(),
            UUID.randomUUID().toString(),
            null
        );

        PaymentDocument savedEntity = paymentRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findAllByUserIdOrderByTimestampDesc(userId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findAllByOrderIdOrderByTimestampDesc(orderId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentRepository.findAllByStatusOrderByTimestampDesc(status).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsSuccessfulByOrderId(Long orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS);
    }

    @Override
    public BigDecimal getTotalSumByDateRange(Long userId, Instant from, Instant to) {
        var result = paymentRepository.sumByUserIdAndTimestampBetween(userId, from, to);

        return result != null && result.getTotal() != null
            ? result.getTotal()
            : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalSumByDateRange(Instant from, Instant to) {
        var result = paymentRepository.sumAllByTimestampBetween(from, to);

        return result != null && result.getTotal() != null
            ? result.getTotal()
            : BigDecimal.ZERO;
    }

    @Override
    public List<PaymentOutboxRecord> findPendingPublicationBatch(int limit) {
        return paymentRepository.findPendingPublicationBatch(PageRequest.of(0, limit)).stream()
            .map(mapper::toOutboxRecord)
            .toList();
    }

    @Override
    public void markPublished(Collection<String> paymentIds, Instant publishedAt) {
        if (paymentIds == null || paymentIds.isEmpty())
            return;

        var documentsToUpdate = paymentRepository.findAllByIdInAndPublishedAtIsNull(paymentIds);

        if (documentsToUpdate.isEmpty())
            return;

        List<PaymentDocument> publishedDocuments = documentsToUpdate.stream()
            .map(document -> new PaymentDocument(
                document.id(),
                document.orderId(),
                document.userId(),
                document.status(),
                document.timestamp(),
                document.amount(),
                document.eventId(),
                publishedAt
            ))
            .toList();

        paymentRepository.saveAll(publishedDocuments);
    }

}
