package com.janusa.seaty

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
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
    fun `all guests are returned as a json array`() {
        every { guestRepository.findAllGuests() } returns
            listOf(Guest(7, "Alice", 7, 1), Guest(28, "Charlotte", 2, 4))

        client
            .get()
            .uri("/api/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(
                """
                [
                  {"id":7,"name":"Alice","seatNumber":7,"tableNumber":1},
                  {"id":28,"name":"Charlotte","seatNumber":2,"tableNumber":4}
                ]
                """.trimIndent(),
                JsonCompareMode.LENIENT,
            )
    }

    @Test
    fun `an empty guest list is returned as an empty array`() {
        every { guestRepository.findAllGuests() } returns emptyList()

        client
            .get()
            .uri("/api/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("[]", JsonCompareMode.LENIENT)
    }

    @Test
    fun `the guest list is cacheable privately by the browser`() {
        every { guestRepository.findAllGuests() } returns emptyList()

        client
            .get()
            .uri("/api/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals("Cache-Control", "max-age=3600, private")
    }
}
