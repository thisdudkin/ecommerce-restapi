package org.example.ecommerce.orders.repository;

import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.support.AbstractPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
    scripts = {
        "/sql/cleanup.sql",
        "/sql/item-test-data.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ItemRepositoryIntegrationTests extends AbstractPostgresContainer {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findActiveReturnsOnlyNotArchivedItem() {
        assertThat(itemRepository.findActive(100L)).isPresent();
        assertThat(itemRepository.findActive(103L)).isEmpty();
    }

    @Test
    void findArchivedReturnsOnlyArchivedItem() {
        assertThat(itemRepository.findArchived(103L)).isPresent();
        assertThat(itemRepository.findArchived(100L)).isEmpty();
    }

    @Test
    void findPageReturnsFirstSliceOrderedByCreatedAtAndId() {
        List<Item> page = itemRepository.findPage(null, null, 2);

        assertThat(page)
            .extracting(Item::getId)
            .containsExactly(100L, 101L);
    }

    @Test
    void findPageReturnsNextSliceByCursor() {
        List<Item> page = itemRepository.findPage(
            LocalDateTime.of(2026, 1, 1, 10, 0),
            100L,
            10
        );

        assertThat(page)
            .extracting(Item::getId)
            .containsExactly(101L, 102L);
    }

}
