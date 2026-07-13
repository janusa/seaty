package com.janusa.seaty

import com.janusa.seaty.support.AbstractDatabaseTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Data-layer test of [GuestRepository] against a real (seeded, temp-file) SQLite database via a
 * [org.springframework.jdbc.core.simple.JdbcClient] wired directly onto the seeded database.
 * Exercises the actual SQL for the full guest list (ordering and row mapping).
 */
class GuestRepositoryDataTest : AbstractDatabaseTest() {
    private val guestRepository = GuestRepository(jdbcClient)

    @Test
    fun `findAllGuests returns every seated guest ordered by name`() {
        val guests = guestRepository.findAllGuests()
        assertThat(guests).hasSize(146)
        assertThat(guests.map { it.name }).isSorted()
        assertThat(guests.first().name).isEqualTo("Aaron")
    }
}
