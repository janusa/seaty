package com.janusa.seaty.support

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.sqlite.JDBC

/**
 * Base class for data-layer tests that run real SQL against the seeded, temp-file SQLite
 * database without booting a Spring application context.
 *
 * Wires a [JdbcClient] directly onto [TestDatabase]'s read-only datasource - the same
 * JdbcClient type the application's repositories use, but constructed by hand instead of by
 * Boot autoconfiguration. There is no context refresh, and the shared seeded database
 * ([TestDatabase.path]) is built once and reused (these tests only read).
 *
 * For tests that need the running application - HTTP endpoints, the app's own datasource
 * configuration, or any Spring bean - use [AbstractIntegrationTest] or
 * [AbstractWebIntegrationTest] instead; those boot the context.
 */
abstract class AbstractDatabaseTest {
    protected val jdbcClient: JdbcClient =
        JdbcClient.create(SimpleDriverDataSource(JDBC(), TestDatabase.readOnlyUrl))
}
