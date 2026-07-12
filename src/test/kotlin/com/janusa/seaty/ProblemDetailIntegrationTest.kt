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
            .uri("/api/guests?name=")
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
            .isEqualTo("/api/guests")
    }

    @Test
    fun `missing required parameter is also a problem detail`() {
        restClient
            .get()
            .uri("/api/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
