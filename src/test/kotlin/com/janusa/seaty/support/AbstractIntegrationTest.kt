package com.janusa.seaty.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base class for full-context integration tests.
 *
 * Boots the real application but points the datasource at a seeded temp SQLite file
 * ([TestDatabase]) instead of `./data/app.db` (the overridden properties live in
 * [TestDatabase.registerProperties]). Everything else (the `PRAGMA query_only=ON` Hikari
 * init, problemdetails, messages) is inherited from the real `application.properties`, so
 * the datasource is read-only exactly like production.
 *
 * [DynamicPropertySource] runs before the context refreshes, so the file is built and seeded
 * before Hikari opens the `mode=ro` connection (which could not open a missing file).
 *
 * For pure data-layer tests that only need SQL against the seeded database - no HTTP, no
 * Spring beans - prefer [AbstractDatabaseTest], which skips the context entirely.
 */
@Suppress("UtilityClassWithPublicConstructor")
@SpringBootTest
abstract class AbstractIntegrationTest {
    private companion object {
        @JvmStatic
        @DynamicPropertySource
        fun springProperties(registry: DynamicPropertyRegistry) {
            TestDatabase.registerProperties(registry)
        }
    }
}
