package org.example.ecommerce.orders.repository.pagination;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.entity.Order_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Long> findPageIds(Specification<Order> specification, LocalDateTime createdAt, Long id, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Order> root = cq.from(Order.class);

        cq.select(root.get(Order_.id));

        Predicate predicate = cb.conjunction();

        if (specification != null) {
            Predicate specificationPredicate = specification.toPredicate(root, cq, cb);
            if (specificationPredicate != null) {
                predicate = cb.and(predicate, specificationPredicate);
            }
        }

        if (createdAt != null && id != null) {
            Predicate cursorPredicate = cb.and(
                cb.greaterThanOrEqualTo(root.get(Order_.createdAt), createdAt),
                cb.or(
                    cb.greaterThan(root.get(Order_.createdAt), createdAt),
                    cb.greaterThan(root.get(Order_.id), id)
                )
            );

            predicate = cb.and(predicate, cursorPredicate);
        }

        cq.where(predicate);
        cq.orderBy(
            cb.asc(root.get(Order_.createdAt)),
            cb.asc(root.get(Order_.id))
        );

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .getResultList();
    }

}
