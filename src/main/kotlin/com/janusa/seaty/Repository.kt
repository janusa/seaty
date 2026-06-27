package com.janusa.seaty

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class Repository(
    private val jdbcClient: JdbcClient,
) {
    fun findGuests(name: String): List<Guest> =
        jdbcClient
            .sql(
                """
                SELECT g.id, g.name, s.seat_number, dt.table_number
                FROM guest g
                JOIN seating_assignment sa on sa.guest_id = g.id
                JOIN seat s on s.id = sa.seat_id
                JOIN dining_table dt on dt.id = s.table_id
                WHERE name like :name ORDER BY name
                """.trimIndent(),
            ).param("name", "$name%")
            .query(Guest::class.java)
            .list()
            .filterNotNull()
}
