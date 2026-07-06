package com.janusa.seaty

import com.janusa.seaty.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Data-layer test of [GuestRepository] against a real (seeded, temp-file) SQLite database via the
 * application's [org.springframework.jdbc.core.simple.JdbcClient]. Exercises the actual SQL:
 * prefix matching, ordering and row mapping.
 */
class GuestRepositoryDataTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var guestRepository: GuestRepository

    @Test
    fun `prefix match returns the matching guest`() {
        assertThat(guestRepository.findGuests("Alic")).containsExactly(Guest(1, "Alice", 3, 2))
    }

    @Test
    fun `non-matching prefix returns an empty list`() {
        assertThat(guestRepository.findGuests("Zz")).isEmpty()
    }

    @Test
    fun `results are ordered by name`() {
        // Empty prefix becomes the LIKE pattern "%", matching every guest.
        assertThat(guestRepository.findGuests("").map { it.name })
            .containsExactly(
                "Ali",
                "Alice",
                "Bob",
                "Bobby",
                "Chad",
                "Chandler",
                "Chanel",
                "Chantelle",
                "Charlene",
                "Charles",
                "Charlie",
                "Charlie",
                "Charlotte",
                "Chase",
                "Eve",
            )
    }

    @Test
    fun `maps id, name, seat and table`() {
        assertThat(guestRepository.findGuests("Bobby")).containsExactly(Guest(5, "Bobby", 5, 2))
    }
}
