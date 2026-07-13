package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * Verifies HTTP errors are returned as RFC 9457 problem details (`application/problem+json`),
 * including the custom detail message from messages.properties. Uses the full context so the
 * MessageSource is guaranteed to be wired.
 */
class ProblemDetailIntegrationTest : AbstractWebIntegrationTest() {
    @Test
    fun `validation failure is a problem detail with the custom message`() {
        restClient
            .get()
            .uri("/api/tables/0/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            // RFC 9457 members. ("type" defaults to "about:blank", which Spring omits.)
            .jsonPath("$.title")
            .exists()
            .jsonPath("$.status")
            .isEqualTo(400)
            .jsonPath("$.detail")
            .value(String::class.java) { detail ->
                assertThat(detail).contains("Invalid request parameters")
            }.jsonPath("$.instance")
            .isEqualTo("/api/tables/0/guests")
    }

    @Test
    fun `a non-numeric path variable is also a problem detail`() {
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
