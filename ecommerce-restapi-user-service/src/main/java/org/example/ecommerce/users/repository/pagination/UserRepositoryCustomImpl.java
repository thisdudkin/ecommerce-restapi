package org.example.ecommerce.users.repository.pagination;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.entity.User_;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private static final String PAYMENT_CARDS = "paymentCards";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<User> findPageWithCards(Specification<User> specification,
                                        int size,
                                        SortDirection direction,
                                        UserCursorPayload cursor) {
        List<Long> pageIds = findPageIds(specification, size, direction, cursor);

        if (pageIds.isEmpty()) {
            return List.of();
        }

        return findUsersWithCards(pageIds, direction);
    }

    private List<Long> findPageIds(Specification<User> specification,
                                   int size,
                                   SortDirection direction,
                                   UserCursorPayload cursor) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<User> root = query.from(User.class);

        Predicate specificationPredicate = specification == null
            ? criteriaBuilder.conjunction()
            : specification.toPredicate(root, query, criteriaBuilder);

        if (specificationPredicate == null) {
            specificationPredicate = criteriaBuilder.conjunction();
        }

        Predicate cursorPredicate = buildCursorPredicate(criteriaBuilder, root, direction, cursor);

        query.select(root.get(User_.ID));
        query.where(criteriaBuilder.and(specificationPredicate, cursorPredicate));
        query.orderBy(
            direction == SortDirection.ASC
                ? criteriaBuilder.asc(root.get(User_.CREATED_AT))
                : criteriaBuilder.desc(root.get(User_.CREATED_AT)),
            direction == SortDirection.ASC
                ? criteriaBuilder.asc(root.get(User_.ID))
                : criteriaBuilder.desc(root.get(User_.ID))
        );

        return entityManager.createQuery(query)
            .setMaxResults(size + 1)
            .getResultList();
    }

    private List<User> findUsersWithCards(List<Long> ids, SortDirection direction) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
        Root<User> root = query.from(User.class);

        root.fetch(PAYMENT_CARDS, JoinType.LEFT);

        query.select(root).distinct(true);
        query.where(root.get(User_.ID).in(ids));
        query.orderBy(
            direction == SortDirection.ASC
                ? criteriaBuilder.asc(root.get(User_.CREATED_AT))
                : criteriaBuilder.desc(root.get(User_.CREATED_AT)),
            direction == SortDirection.ASC
                ? criteriaBuilder.asc(root.get(User_.ID))
                : criteriaBuilder.desc(root.get(User_.ID))
        );

        return entityManager.createQuery(query)
            .getResultList();
    }

    private Predicate buildCursorPredicate(CriteriaBuilder cb,
                                           Root<User> root,
                                           SortDirection direction,
                                           UserCursorPayload cursorPayload) {
        if (cursorPayload == null) {
            return cb.conjunction();
        }

        LocalDateTime createdAt = cursorPayload.createdAt();
        Long id = cursorPayload.id();

        return switch (direction) {
            case ASC -> cb.and(
                cb.greaterThanOrEqualTo(root.get(User_.CREATED_AT), createdAt),
                cb.or(
                    cb.greaterThan(root.get(User_.CREATED_AT), createdAt),
                    cb.greaterThan(root.get(User_.ID), id)
                )
            );
            case DESC -> cb.and(
                cb.lessThanOrEqualTo(root.get(User_.CREATED_AT), createdAt),
                cb.or(
                    cb.lessThan(root.get(User_.CREATED_AT), createdAt),
                    cb.lessThan(root.get(User_.ID), id)
                )
            );
        };
    }

}
