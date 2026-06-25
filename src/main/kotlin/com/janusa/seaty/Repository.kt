package com.janusa.seaty

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class Repository(
    private val jdbcClient: JdbcClient,
) {
    fun findGuests(name: String): List<Guest> =
        jdbcClient.sql(
            """
                SELECT guest.id, guest.name FROM guest where name like :name order by name
            """.trimIndent(),
        )
            .param("name", "$name%")
            .query(Guest::class.java)
            .list()
            .filterNotNull()
}