package com.janusa.seaty

import com.janusa.seaty.support.AbstractWebIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

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

    // Spring has served ".js" as either "application/javascript" or "text/javascript" across versions;
    // accept both so the test guards "is it JavaScript" without pinning the exact spelling.
    private val javascript = listOf("text/javascript", "application/javascript").map(MediaType::valueOf)

    /** No session cookie - stands in for a browser or mobile OS auto-probing for an icon. */
    private val anonymousClient: RestTestClient by lazy {
        RestTestClient.bindToApplicationContext(webContext).build()
    }

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
    fun `serves the script as javascript`() {
        val contentType =
            restClient
                .get()
                .uri("/scripts/script.js")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String::class.java)
                .returnResult()
                .responseHeaders.contentType

        assertThat(contentType)
            .describedAs("content type of /scripts/script.js")
            .isNotNull()
        assertThat(javascript.any { contentType!!.isCompatibleWith(it) })
            .describedAs("content type %s is a JavaScript type", contentType)
            .isTrue()
    }

    @Test
    fun `static resources are cacheable privately by the browser`() {
        restClient
            .get()
            .uri("/scripts/script.js")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals("Cache-Control", "max-age=3600, private")
    }

    @Test
    fun `index page references a working script`() {
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

        // Resolve the script the page references and confirm it is actually served
        // (guards against a broken <script src> after future edits).
        val script = doc.selectFirst("script[src]")
        assertThat(script).isNotNull()
        val src = script!!.attr("src")
        assertThat(src).isNotBlank()
        val scriptPath = if (src.startsWith("/")) src else "/$src"

        restClient
            .get()
            .uri(scriptPath)
            .exchange()
            .expectStatus()
            .isOk()
    }

    @Test
    fun `precomposed touch-icon probe falls through to a clean 404, not a gated 401`() {
        // The auth interceptor excludes this legacy iOS/Android probe path, so an unauthenticated
        // request reaches the resource layer and 404s (no file is shipped) instead of being gated
        // to a misleading 401.
        anonymousClient
            .get()
            .uri("/apple-touch-icon-precomposed.png")
            .exchange()
            .expectStatus()
            .isNotFound()

        // The icon that clients fall back to is itself auth-open and actually served, so the
        // fallback the 404 relies on works for the same unauthenticated client.
        anonymousClient
            .get()
            .uri("/apple-touch-icon.png")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.IMAGE_PNG)

        // Control: a non-excluded missing path is still gated to 401 for the same anonymous client,
        // proving the 404 above comes from the exclusion, not from anonymity 404ing everything.
        anonymousClient
            .get()
            .uri("/not-an-excluded-path.png")
            .exchange()
            .expectStatus()
            .isUnauthorized()
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
