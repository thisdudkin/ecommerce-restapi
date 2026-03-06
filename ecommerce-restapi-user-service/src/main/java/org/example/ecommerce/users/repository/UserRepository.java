package org.example.ecommerce.users.repository;

import jakarta.persistence.LockModeType;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

public interface UserRepository extends JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(value = "userWithCards", type = LOAD)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    @EntityGraph(value = "userWithCards", type = LOAD)
    Optional<User> findByIdWithCards(Long id);

    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    int updateActiveStatus(Long id, Boolean active);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    default Window<User> findWindow(Specification<User> spec,
                                    Pageable pageable,
                                    ScrollPosition position) {
        return findBy(
            spec,
            query -> query
                .sortBy(pageable.getSort())
                .limit(pageable.getPageSize())
                .scroll(position)
        );
    }

    static Sort keysetSort(SortDirection direction) {
        return switch (direction) {
            case ASC -> Sort.by(
                asc("createdAt"),
                asc("id")
            );
            case DESC -> Sort.by(
                desc("createdAt"),
                desc("id")
            );
        };
    }

}
