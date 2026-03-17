package org.example.ecommerce.orders.repository;

import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.repository.pagination.ItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

    @Query("""
        select i
          from Item i
          where i.id = :itemId
            and i.archived = false
        """)
    Optional<Item> findActive(Long itemId);

    @Query("""
        select i
          from Item i
          where i.id = :itemId
            and i.archived = true
        """)
    Optional<Item> findArchived(Long itemId);

}
