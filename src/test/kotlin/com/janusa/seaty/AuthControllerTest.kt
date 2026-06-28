package com.janusa.seaty

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertContains

@WebMvcTest(AuthController::class)
class AuthControllerTest {
    @Autowired
    private lateinit var webContext: WebApplicationContext

    private val client: RestTestClient by lazy {
        RestTestClient.bindToApplicationContext(webContext).build()
    }

    @Test
    fun `incorrect secret results in 401`() {
        client
            .get()
            .uri("/auth?secret=456")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody()
            .isEmpty
    }

    @Test
    fun `auth should set a session cookie`() {
        client
            .get()
            .uri("/auth?secret=123")
            .exchange()
            .expectCookie()
            .valueEquals("session", "123")
            .expectCookie()
            .maxAge("session") { 86400 }
            .expectCookie()
            .secure("session", true)
            .expectCookie()
            .httpOnly("session", true)
            .expectCookie()
            .sameSite("session", "strict")
    }

    @Test
    fun `auth should respond with a 303 redirect to the index page`() {
        client
            .get()
            .uri("/auth?secret=123")
            .exchange()
            .expectStatus()
            .isSeeOther
            .expectHeader()
            .location("/index.html")
            .expectBody()
            .isEmpty
    }
}
