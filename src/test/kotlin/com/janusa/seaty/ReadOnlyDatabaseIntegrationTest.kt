package com.janusa.seaty

import com.janusa.seaty.support.AbstractIntegrationTest
import com.janusa.seaty.support.TestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.sqlite.JDBC

/**
 * Verifies the server treats the database as read-only. The application's own
 * [JdbcClient] (configured with `mode=ro` + `PRAGMA query_only=ON`, inherited from the real
 * application.properties) must reject writes.
 */
class ReadOnlyDatabaseIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `writes through the application datasource are rejected`() {
        assertThatThrownBy {
            jdbcClient.sql("INSERT INTO guest(name) VALUES ('Mallory')").update()
        }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("readonly")
    }

    @Test
    fun `read-only is enforced by config, not file permissions`() {
        // The same kind of SQLite file, opened with a plain writable URL (no mode=ro, no
        // PRAGMA query_only), accepts writes — so the rejection above is configuration.
        val writable = JdbcClient.create(
            SimpleDriverDataSource(JDBC(), "jdbc:sqlite:${TestDatabase.createWritable()}"),
        )

        assertThatCode {
            writable.sql("INSERT INTO guest(name) VALUES ('Mallory')").update()
        }.doesNotThrowAnyException()

        val count = writable.sql("SELECT count(*) FROM guest WHERE name = 'Mallory'")
            .query(Int::class.javaObjectType)
            .single()
        assertThat(count).isEqualTo(1)
    }
}
