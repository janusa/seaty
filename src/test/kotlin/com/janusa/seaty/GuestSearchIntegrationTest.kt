package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.json.JsonCompareMode

/**
 * End-to-end test of the guest search: real HTTP request through the full application stack
 * (controller → repository → seeded SQLite) using [org.springframework.test.web.servlet.client.RestTestClient].
 */
class GuestSearchIntegrationTest : AbstractWebIntegrationTest() {
    @Test
    fun `searching by prefix returns the matching guests`() {
        restClient
            .get()
            .uri("/api/guests?name=Ali")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("""["Guest(id=1, name=Alice)"]""", JsonCompareMode.LENIENT)
    }

    @Test
    fun `searching with no matches returns an empty array`() {
        restClient
            .get()
            .uri("/api/guests?name=Zzz")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("[]", JsonCompareMode.LENIENT)
    }
}
