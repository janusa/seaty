package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * End-to-end test of the guest list endpoint: a real HTTP request through the full application stack
 * (controller → repository → seeded SQLite) using [org.springframework.test.web.servlet.client.RestTestClient].
 * Search matching lives in the browser, so this verifies only the roster the client fetches once.
 */
class GuestListIntegrationTest : AbstractWebIntegrationTest() {
    @Test
    fun `all seated guests are returned ordered by name`() {
        restClient
            .get()
            .uri("/api/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals("Cache-Control", "max-age=3600, private")
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(140)
            .jsonPath("$[0].name")
            .isEqualTo("Aaron")
    }

    @Test
    fun `accented guest names are served intact`() {
        val body =
            restClient
                .get()
                .uri("/api/guests")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

        assertThat(body).contains("Zoë")
    }
}
