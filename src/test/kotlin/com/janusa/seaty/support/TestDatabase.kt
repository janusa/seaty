package com.janusa.seaty.support

import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.DynamicPropertyRegistry
import org.sqlite.JDBC
import java.nio.file.Files

/**
 * Builds embedded SQLite databases for tests from the canonical [db/schema.sql] and
 * [db/seed.sql] (mapped onto the test classpath in pom.xml - single source of truth).
 *
 * No Testcontainers: SQLite is a file, so each database is just a temp file.
 */
object TestDatabase {
    /**
     * Secret every test that boots the app authenticates with: registered as `auth.secret`
     * on the context (see [registerProperties]) and presented back as the `session` cookie by
     * the test clients that call the app.
     */
    const val TEST_SECRET: String = "123"

    /**
     * Path to a single shared, seeded database, built once on first access. Integration
     * tests read from it through a read-only datasource, so it never gets mutated and can
     * safely be reused across the whole test run.
     */
    val path: String by lazy { createSeeded() }

    /** Read-only JDBC URL onto [path], matching production's datasource config exactly. */
    val readOnlyUrl: String
        get() = "jdbc:sqlite:file:$path?mode=ro&foreign_keys=true"

    /**
     * Points a full-context Spring test at the seeded, read-only temp database ([readOnlyUrl])
     * and sets the [TEST_SECRET]. Shared by [AbstractIntegrationTest] (for its whole subtree)
     * and the standalone RANDOM_PORT `PlaywrightSmokeTest`, which needs the same wiring but
     * cannot share that base class.
     */
    fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { readOnlyUrl }
        registry.add("auth.secret") { TEST_SECRET }
    }

    /**
     * Builds a fresh, independently-seeded, *writable* database and returns its path. Used
     * by the read-only test to prove that read-only behaviour comes from configuration
     * (mode=ro + PRAGMA query_only) rather than from the file being unwritable.
     */
    fun createWritable(): String = createSeeded()

    private fun createSeeded(): String {
        val file = Files.createTempFile("seaty-test-", ".db").toFile().apply { deleteOnExit() }
        // Plain writable URL (no mode=ro / PRAGMA) so the populator can create + seed.
        val dataSource = SimpleDriverDataSource(JDBC(), "jdbc:sqlite:${file.absolutePath}")
        ResourceDatabasePopulator(
            ClassPathResource("db/schema.sql"),
            ClassPathResource("db/seed.sql"),
        ).execute(dataSource)
        return file.absolutePath
    }
}
