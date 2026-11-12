package com.expensetracker.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Base class for integration tests: full application context, MockMvc and a real PostgreSQL.
 *
 * <p>All subclasses share the same configuration, so Spring caches one context and Testcontainers
 * starts a single PostgreSQL container for the whole test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvcTester mvc;
}
