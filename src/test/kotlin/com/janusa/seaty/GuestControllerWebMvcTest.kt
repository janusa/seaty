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
 * Web-slice test of [Controller] - loads only the MVC layer (no datasource). This is the
 * "web seam": fast HTTP tests with the repository replaced by a MockK mock. The mock is
 * supplied through a nested [TestConfiguration] because the slice does not register the real
 * [Repository] bean, so there is nothing to override - it must be created.
 */
@WebMvcTest(Controller::class)
class GuestControllerWebMvcTest {
    @TestConfiguration
    class Mocks {
        @Bean
        fun repository(): Repository = mockk()
    }

    @Autowired
    private lateinit var webContext: WebApplicationContext

    @Autowired
    private lateinit var repository: Repository

    private val client: RestTestClient by lazy {
        RestTestClient.bindToApplicationContext(webContext).build()
    }

    @Test
    fun `valid name returns the matching guests as a json array`() {
        every { repository.findGuests("Ali") } returns listOf(Guest(1, "Alice"))

        client
            .get()
            .uri("/api/guests?name=Ali")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("""["Guest(id=1, name=Alice)"]""", JsonCompareMode.LENIENT)
    }

    @Test
    fun `name shorter than 3 characters is rejected with a problem detail`() {
        client
            .get()
            .uri("/api/guests?name=ab")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
