package com.janusa.seaty

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class GuestRepository(
    private val jdbcClient: JdbcClient,
) {
    fun findGuests(name: String): List<Guest> {
        val likePattern = "$name%"
        log.debug("Querying guests with name like '{}'", likePattern)
        val result =
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
                ).param("name", likePattern)
                .query(Guest::class.java)
                .list()
                .filterNotNull()
        log.debug("Query for name like '{}' returned {} row(s)", likePattern, result.size)
        return result
    }

    fun findGuestsByTable(tableNumber: Long): List<Guest> {
        log.debug("Querying roster for table {}", tableNumber)
        val result =
            jdbcClient
                .sql(
                    """
                    SELECT g.id, g.name, s.seat_number, dt.table_number
                    FROM guest g
                    JOIN seating_assignment sa on sa.guest_id = g.id
                    JOIN seat s on s.id = sa.seat_id
                    JOIN dining_table dt on dt.id = s.table_id
                    WHERE dt.table_number = :tableNumber ORDER BY s.seat_number
                    """.trimIndent(),
                ).param("tableNumber", tableNumber)
                .query(Guest::class.java)
                .list()
                .filterNotNull()
        log.debug("Table {} returned {} row(s)", tableNumber, result.size)
        return result
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuestRepository::class.java)
    }
}
