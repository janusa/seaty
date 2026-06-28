package com.janusa.seaty.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base class for full-context integration tests.
 *
 * Boots the real application but points the datasource at a seeded temp SQLite file
 * ([TestDatabase]) instead of `./data/app.db`. Only `spring.datasource.url` is overridden -
 * everything else (the `PRAGMA query_only=ON` Hikari init, problemdetails, messages) is
 * inherited from the real `application.properties`, so the datasource is read-only exactly
 * like production.
 *
 * [DynamicPropertySource] runs before the context refreshes, so the file is built and seeded
 * before Hikari opens the `mode=ro` connection (which could not open a missing file).
 */
@SpringBootTest
abstract class AbstractIntegrationTest {
    protected companion object {
        const val TEST_SECRET = "123"

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { TestDatabase.readOnlyUrl }
            registry.add("auth.secret") { TEST_SECRET }
        }
    }
}
