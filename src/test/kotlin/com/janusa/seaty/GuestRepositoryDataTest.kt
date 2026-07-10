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
}
