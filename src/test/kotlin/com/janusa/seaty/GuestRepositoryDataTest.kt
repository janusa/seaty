package com.janusa.seaty

import com.janusa.seaty.support.AbstractDatabaseTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Data-layer test of [GuestRepository] against a real (seeded, temp-file) SQLite database via a
 * [org.springframework.jdbc.core.simple.JdbcClient] wired directly onto the seeded database.
 * Exercises the actual SQL: prefix matching, ordering and row mapping.
 */
class GuestRepositoryDataTest : AbstractDatabaseTest() {
    private val guestRepository = GuestRepository(jdbcClient)

    @Test
    fun `prefix match returns the matching guest`() {
        assertThat(guestRepository.findGuests("Alic")).containsExactly(Guest(7, "Alice", 7, 1))
    }

    @Test
    fun `non-matching prefix returns an empty list`() {
        assertThat(guestRepository.findGuests("Zz")).isEmpty()
    }

    @Test
    fun `results are ordered by name`() {
        // Empty prefix becomes the LIKE pattern "%", matching every guest.
        val names = guestRepository.findGuests("").map { it.name }
        assertThat(names).hasSize(146)
        assertThat(names).isSorted()
    }

    @Test
    fun `maps id, name, seat and table`() {
        assertThat(guestRepository.findGuests("Bobby")).containsExactly(Guest(14, "Bobby", 6, 2))
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
