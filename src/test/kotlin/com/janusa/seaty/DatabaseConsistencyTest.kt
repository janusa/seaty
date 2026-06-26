package com.janusa.seaty

import com.janusa.seaty.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient

/**
 * Guards the committed `db/seed.sql` against `db/schema.sql`. Because the application has no write
 * path, these database-level invariants are the only thing keeping the hand-written seed (and any
 * future manual inserts) consistent, so this test fails the build if the data ever drifts from the
 * schema. Both pragmas are read-only, so they run fine through the read-only datasource.
 */
class DatabaseConsistencyTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `the seeded database has no foreign key violations`() {
        val violations = jdbcClient.sql("PRAGMA foreign_key_check").query().listOfRows()
        assertThat(violations).isEmpty()
    }

    @Test
    fun `the seeded database passes the integrity check`() {
        val result = jdbcClient.sql("PRAGMA integrity_check").query(String::class.java).list()
        assertThat(result).containsExactly("ok")
    }
}
