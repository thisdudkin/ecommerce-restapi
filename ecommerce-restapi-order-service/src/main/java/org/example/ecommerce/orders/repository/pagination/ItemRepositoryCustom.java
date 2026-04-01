package org.example.ecommerce.orders.repository.pagination;

import org.example.ecommerce.orders.entity.Item;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemRepositoryCustom {

    List<Item> findPage(LocalDateTime createdAt, Long id, int size);

}
