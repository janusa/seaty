package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * Verifies HTTP errors are returned as RFC 9457 problem details (`application/problem+json`). Uses the
 * full context so the framework's error handling is exercised end to end.
 */
class ProblemDetailIntegrationTest : AbstractWebIntegrationTest() {
    @Test
    fun `an unmapped path is returned as a problem detail`() {
        restClient
            .get()
            .uri("/api/does-not-exist")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            // RFC 9457 members. ("type" defaults to "about:blank", which Spring omits.)
            .jsonPath("$.title")
            .exists()
            .jsonPath("$.status")
            .isEqualTo(404)
    }
}
