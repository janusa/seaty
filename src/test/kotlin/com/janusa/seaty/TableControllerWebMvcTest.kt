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
 * Web-slice test of [TableController] - loads only the MVC layer (no datasource), with the
 * repository replaced by a MockK mock supplied through a nested [TestConfiguration].
 */
@WebMvcTest(TableController::class, properties = ["auth.secret=123"])
class TableControllerWebMvcTest {
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
    fun `positive table number returns the roster as a json array`() {
        every { guestRepository.findGuestsByTable(4) } returns listOf(Guest(28, "Charlotte", 2, 4))

        client
            .get()
            .uri("/api/tables/4/guests")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(
                """[{"id":28,"name":"Charlotte","seatNumber":2,"tableNumber":4}]""",
                JsonCompareMode.LENIENT,
            )
    }

    @Test
    fun `non-positive table number is rejected with a problem detail`() {
        client
            .get()
            .uri("/api/tables/0/guests")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
