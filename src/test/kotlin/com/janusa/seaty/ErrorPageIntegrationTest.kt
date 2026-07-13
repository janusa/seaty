package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Covers the styled error pages under `static/error/`. Three concerns are checked separately:
 *
 * 1. The page artifacts exist, are served as HTML, and their flower image actually resolves - a
 *    broken file or a mistyped image path fails the build.
 * 2. The whole `/error` tree (page + images) is reachable without a session, so the unauthenticated
 *    401 page can render its artwork instead of being gated by the auth interceptor.
 * 3. Error responses are content-negotiated: API/other clients keep the RFC 9457 problem+json body,
 *    while a browser (an explicit `text/html` Accept) is routed to the error dispatch instead.
 *
 * The rendered error page itself is not asserted here: MockMvc (backing [restClient]) does not run
 * the container's error dispatch, so `sendError` only sets the status. The end-to-end render is
 * exercised against a running server.
 */
class ErrorPageIntegrationTest : AbstractWebIntegrationTest() {
    /** No session cookie - stands in for an unauthenticated guest hitting the 401 page. */
    private val anonymousClient: RestTestClient by lazy {
        RestTestClient.bindToApplicationContext(webContext).build()
    }

    @Test
    fun `every error page is served as html and its flower image resolves`() {
        mapOf(
            "/error/400.html" to "Bad request",
            "/error/401.html" to "Invitation",
            "/error/404.html" to "not found",
            "/error/500.html" to "went wrong",
            "/error/4xx.html" to "isn",
            "/error/5xx.html" to "went wrong",
        ).forEach { (path, titleFragment) ->
            val html =
                restClient
                    .get()
                    .uri(path)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody

            val doc = Jsoup.parse(html ?: "")
            assertThat(doc.title()).containsIgnoringCase(titleFragment)

            // The referenced flower image must actually be served (guards against a broken src path).
            val src = doc.selectFirst("img.flower")?.attr("src")
            assertThat(src).describedAs("flower image on %s", path).isNotBlank()
            restClient
                .get()
                .uri(src!!)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.IMAGE_PNG)

            // The shared stylesheet must resolve too (guards against a broken link href).
            val cssHref = doc.selectFirst("link[rel=stylesheet]")?.attr("href")
            assertThat(cssHref).describedAs("stylesheet on %s", path).isNotBlank()
            restClient
                .get()
                .uri(cssHref!!)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.valueOf("text/css"))
        }
    }

    @Test
    fun `error assets are reachable without authentication`() {
        // An anonymous guest can load the flower image referenced by the 401 page...
        anonymousClient
            .get()
            .uri("/error/img/rose.png")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.IMAGE_PNG)

        // ...while ordinary paths remain gated for that same anonymous client.
        anonymousClient
            .get()
            .uri("/index.html")
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `api clients keep problem+json but browsers are routed to the error page`() {
        restClient
            .get()
            .uri("/api/tables/0/guests")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)

        val browserResult =
            restClient
                .get()
                .uri("/api/tables/0/guests")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String::class.java)
                .returnResult()

        // Routed to the error dispatch (sendError), so it is not the problem+json body.
        assertThat(browserResult.responseHeaders.contentType).isNotEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
    }
}
