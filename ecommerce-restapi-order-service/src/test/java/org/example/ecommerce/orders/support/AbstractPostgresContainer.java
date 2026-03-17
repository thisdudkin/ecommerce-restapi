package org.example.ecommerce.orders.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresContainer {

    private static final String POSTGRES_VERSION = "postgres:17.0";

    @Container
    @ServiceConnection
    protected static final JdbcDatabaseContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_VERSION);

}
