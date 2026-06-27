package com.janusa.seaty

import com.janusa.seaty.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Data-layer test of [Repository] against a real (seeded, temp-file) SQLite database via the
 * application's [org.springframework.jdbc.core.simple.JdbcClient]. Exercises the actual SQL:
 * prefix matching, ordering and row mapping.
 */
class GuestRepositoryDataTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var repository: Repository

    @Test
    fun `prefix match returns the matching guest`() {
        assertThat(repository.findGuests("Alic")).containsExactly(Guest(1, "Alice", 3, 2))
    }

    @Test
    fun `non-matching prefix returns an empty list`() {
        assertThat(repository.findGuests("Zz")).isEmpty()
    }

    @Test
    fun `results are ordered by name`() {
        // Empty prefix becomes the LIKE pattern "%", matching every guest.
        assertThat(repository.findGuests("").map { it.name })
            .containsExactly("Ali", "Alice", "Bob", "Bobby", "Charles", "Charlie", "Charlotte", "Eve")
    }

    @Test
    fun `maps id, name, seat and table`() {
        assertThat(repository.findGuests("Bobby")).containsExactly(Guest(5, "Bobby", 5, 5))
    }
}
