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
        assertThat(repository.findGuests("Al")).containsExactly(Guest(1, "Alice"))
    }

    @Test
    fun `non-matching prefix returns an empty list`() {
        assertThat(repository.findGuests("Zz")).isEmpty()
    }

    @Test
    fun `results are ordered by name`() {
        // Empty prefix becomes the LIKE pattern "%", matching every guest.
        assertThat(repository.findGuests("").map { it.name })
            .containsExactly("Alice", "Bob", "Charlie")
    }

    @Test
    fun `maps both id and name`() {
        assertThat(repository.findGuests("Bob")).containsExactly(Guest(2, "Bob"))
    }
}
