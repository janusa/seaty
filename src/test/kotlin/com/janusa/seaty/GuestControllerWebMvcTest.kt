package com.janusa.seaty

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.web.context.WebApplicationContext

/**
 * Web-slice test of [GuestController] - loads only the MVC layer (no datasource). This is the
 * "web seam": fast HTTP tests with the repository replaced by a MockK mock. The mock is
 * supplied through a nested [TestConfiguration] because the slice does not register the real
 * [GuestRepository] bean, so there is nothing to override - it must be created.
 */
@WebMvcTest(GuestController::class, properties = ["auth.secret=123"])
class GuestControllerWebMvcTest {
    @TestConfiguration
    class Mocks {
        @Bean
        fun repository(): GuestRepository = mockk()
    }

    @Autowired
    private lateinit var webContext: WebApplicationContext

    @Autowired
    private lateinit var guestRepository: GuestRepository

    private val client: RestTestClient by lazy {
        RestTestClient
            .bindToApplicationContext(webContext)
            .defaultCookie<RestTestClient.WebAppContextSetupBuilder>("session", "123")
            .build()
    }

    @Test
    fun `valid name returns the matching guests as a json array`() {
        every { guestRepository.findGuests("Ali") } returns listOf(Guest(1, "Alice", 1, 1))

        client
            .get()
            .uri("/api/guests?name=Ali")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(
                """[{"id":1,"name":"Alice","seatNumber":1,"tableNumber":1}]""",
                JsonCompareMode.LENIENT,
            )
    }

    @Test
    fun `a single-character name is accepted`() {
        every { guestRepository.findGuests("C") } returns listOf(Guest(1, "Chad", 3, 5))

        client
            .get()
            .uri("/api/guests?name=C")
            .exchange()
            .expectStatus()
            .isOk()
    }

    @Test
    fun `an empty name is rejected with a problem detail`() {
        client
            .get()
            .uri("/api/guests?name=")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
