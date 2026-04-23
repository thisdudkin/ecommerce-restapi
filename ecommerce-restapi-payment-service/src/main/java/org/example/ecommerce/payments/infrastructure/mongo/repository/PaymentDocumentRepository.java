package org.example.ecommerce.payments.infrastructure.mongo.repository;

import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface PaymentDocumentRepository extends MongoRepository<PaymentDocument, String> {

    List<PaymentDocument> findAllByUserIdOrderByTimestampDesc(Long userId);

    List<PaymentDocument> findAllByOrderIdOrderByTimestampDesc(Long orderId);

    List<PaymentDocument> findAllByStatusOrderByTimestampDesc(PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    @Query(value = "{ 'published_at': null }", sort = "{ 'timestamp': 1 }")
    List<PaymentDocument> findPendingPublicationBatch(Pageable pageable);

    List<PaymentDocument> findAllByIdInAndPublishedAtIsNull(Collection<String> ids);

    interface SumView {
        BigDecimal getTotal();
    }

    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, timestamp: { $gte: ?1, $lt: ?2 }, status: { $ne: 'FAILED' } } }",
        "{ $group: { _id: null, total: { $sum: '$amount' } } }",
        "{ $project: { _id: 0, total: 1 } }"
    })
    SumView sumByUserIdAndTimestampBetween(Long userId, Instant from, Instant to);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0, $lt: ?1 }, status: { $ne: 'FAILED' } } }",
        "{ $group: { _id: null, total: { $sum: '$amount' } } }",
        "{ $project: { _id: 0, total: 1 } }"
    })
    SumView sumAllByTimestampBetween(Instant from, Instant to);

}
