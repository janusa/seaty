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
            .json(
                """
                [
                  {"id":6,"name":"Ali","seatNumber":6,"tableNumber":1},
                  {"id":7,"name":"Alice","seatNumber":7,"tableNumber":1}
                ]
                """.trimIndent(),
                JsonCompareMode.LENIENT,
            )
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
