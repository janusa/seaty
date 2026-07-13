package com.janusa.seaty

import com.janusa.seaty.support.AbstractDatabaseTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Data-layer test of [GuestRepository] against a real (seeded, temp-file) SQLite database via a
 * [org.springframework.jdbc.core.simple.JdbcClient] wired directly onto the seeded database.
 * Exercises the actual SQL: the full guest list and the per-table roster (ordering and row mapping).
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

    @Test
    fun `table roster is returned ordered by seat number`() {
        assertThat(guestRepository.findGuestsByTable(4)).containsExactly(
            Guest(27, "Charlie", 1, 4),
            Guest(28, "Charlotte", 2, 4),
            Guest(29, "Chase", 3, 4),
            Guest(30, "Clara", 4, 4),
            Guest(31, "Colin", 5, 4),
            Guest(32, "Daniel", 6, 4),
            Guest(33, "David", 7, 4),
            Guest(34, "Dennis", 8, 4),
        )
    }

    @Test
    fun `head table roster has six guests in seat order`() {
        val roster = guestRepository.findGuestsByTable(18)
        assertThat(roster).hasSize(6)
        assertThat(roster.map { it.seatNumber }).containsExactly(1L, 2L, 3L, 4L, 5L, 6L)
        assertThat(roster).contains(Guest(143, "Zara", 3, 18))
    }

    @Test
    fun `ten-seat table roster has ten guests`() {
        val roster = guestRepository.findGuestsByTable(2)
        assertThat(roster).hasSize(10)
        assertThat(roster).contains(Guest(14, "Bobby", 6, 2))
    }

    @Test
    fun `duplicate first names are returned as distinct rows`() {
        // "Charlie" is seated at two different tables; each is its own row keyed by id.
        assertThat(guestRepository.findGuestsByTable(3)).contains(Guest(26, "Charlie", 8, 3))
        assertThat(guestRepository.findGuestsByTable(4)).startsWith(Guest(27, "Charlie", 1, 4))
    }

    @Test
    fun `roster for an unknown table is empty`() {
        assertThat(guestRepository.findGuestsByTable(999)).isEmpty()
    }
}
