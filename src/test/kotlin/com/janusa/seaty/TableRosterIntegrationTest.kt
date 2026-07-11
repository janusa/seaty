package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode

/**
 * End-to-end test of the table roster: real HTTP request through the full application stack
 * (controller → repository → seeded SQLite) using [org.springframework.test.web.servlet.client.RestTestClient].
 */
class TableRosterIntegrationTest : AbstractWebIntegrationTest() {
    @Test
    fun `table roster returns the guests ordered by seat number`() {
        restClient
            .get()
            .uri("/api/tables/4/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(8)
            .jsonPath("$[0].seatNumber")
            .isEqualTo(1)
            .jsonPath("$[0].name")
            .isEqualTo("Charlie")
            .jsonPath("$[1].name")
            .isEqualTo("Charlotte")
            .jsonPath("$[7].name")
            .isEqualTo("Dennis")
    }

    @Test
    fun `roster for an unknown table returns an empty array`() {
        restClient
            .get()
            .uri("/api/tables/999/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("[]", JsonCompareMode.LENIENT)
    }

    @Test
    fun `zero table number is a problem detail`() {
        restClient
            .get()
            .uri("/api/tables/0/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(400)
            .jsonPath("$.detail")
            .value(String::class.java) { detail ->
                assertThat(detail).contains("Invalid request parameters")
            }.jsonPath("$.instance")
            .isEqualTo("/api/tables/0/guests")
    }

    @Test
    fun `negative table number is a problem detail`() {
        restClient
            .get()
            .uri("/api/tables/-1/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun `non-numeric table number is rejected`() {
        restClient
            .get()
            .uri("/api/tables/abc/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
