package org.example.ecommerce.orders.repository.pagination;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Item_;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Item> findPage(LocalDateTime createdAt, Long id, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Item> cq = cb.createQuery(Item.class);
        Root<Item> root = cq.from(Item.class);

        Predicate predicate = cb.isFalse(root.get(Item_.archived));

        if (createdAt != null && id != null) {
            predicate = cb.and(
                predicate,
                cb.greaterThanOrEqualTo(root.get(Item_.createdAt), createdAt),
                cb.or(
                    cb.greaterThan(root.get(Item_.createdAt), createdAt),
                    cb.greaterThan(root.get(Item_.id), id)
                )
            );
        }

        cq.where(predicate);
        cq.orderBy(
            cb.asc(root.get(Item_.createdAt)),
            cb.asc(root.get(Item_.id))
        );

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .getResultList();
    }

}
