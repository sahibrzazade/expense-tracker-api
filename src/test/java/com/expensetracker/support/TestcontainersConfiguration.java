package com.expensetracker.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL container for tests. {@link ServiceConnection} wires its JDBC URL,
 * username and password into the Spring datasource, so no profile or env vars are needed.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // Same major version as compose.yaml, so tests and local dev run the same database
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
