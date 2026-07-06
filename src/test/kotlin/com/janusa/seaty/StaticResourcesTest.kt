package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * Tests the statically served frontend.
 *
 * Note on "/": Spring's WelcomePageHandlerMapping serves the root as a *forward* to index.html,
 * and MockMvc (which backs RestTestClient here) does not execute forwards - so GET "/" returns
 * 200 but an empty body. We therefore assert only that the route is mapped for "/", and make the
 * content/structure assertions against "/index.html", which the resource handler serves directly
 * with its body intact. Verifying "/" end to end with a real body would require a running server
 * (@SpringBootTest(webEnvironment = RANDOM_PORT)).
 */
class StaticResourcesTest : AbstractWebIntegrationTest() {
    private val css = MediaType.valueOf("text/css")

    @Test
    fun `welcome page route is mapped`() {
        restClient
            .get()
            .uri("/")
            .exchange()
            .expectStatus()
            .isOk()
    }

    @Test
    fun `serves the index page as html`() {
        restClient
            .get()
            .uri("/index.html")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_HTML)
    }

    @Test
    fun `serves the stylesheet as css`() {
        restClient
            .get()
            .uri("/css/styles.css")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(css)
    }

    @Test
    fun `index page has the expected structure and a working stylesheet link`() {
        val html =
            restClient
                .get()
                .uri("/index.html")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

        val doc = Jsoup.parse(html ?: "")

        assertThat(doc.title()).isEqualTo("Seaty")
        assertThat(doc.selectFirst("h1")?.text()).isEqualTo("Guest Search")

        // Resolve the stylesheet the page references and confirm it is actually served
        // (guards against a broken <link href> after future edits).
        val link = doc.selectFirst("link[rel=stylesheet]")
        assertThat(link).isNotNull()
        val href = link!!.attr("href")
        assertThat(href).isNotBlank()
        val cssPath = if (href.startsWith("/")) href else "/$href"

        restClient
            .get()
            .uri(cssPath)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(css)
    }
}
