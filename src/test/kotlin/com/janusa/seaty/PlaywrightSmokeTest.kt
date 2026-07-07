package com.janusa.seaty

import com.janusa.seaty.support.TestDatabase
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Real-browser smoke tests for the guest-search frontend: drive headless Chromium against the app on a
 * random port and assert on the rendered DOM. Covers the happy path plus the DOM/fetch branches the
 * GraalJS unit tests can't reach (deep-link restore, stale-link fallback, empty results), the table
 * close-up beneath the map, and the landscape layout on a portrait phone. One Chromium and one server
 * are shared across the class; each test uses a fresh authenticated context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlaywrightSmokeTest {
    @LocalServerPort
    private var port: Int = 0

    private val baseUrl get() = "http://localhost:$port"

    private fun newAuthenticatedContext(
        options: Browser.NewContextOptions = Browser.NewContextOptions(),
    ): BrowserContext {
        val context = browser.newContext(options)
        context.addCookies(listOf(Cookie("session", TEST_SECRET).setUrl(baseUrl)))
        return context
    }

    @Test
    fun `search, select a guest, and see the seat highlighted on the map`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/")
            page.fill("#guest-search", "Cha")
            page.waitForSelector("li.search-result")
            assertThat(page.querySelectorAll("li.search-result")).isNotEmpty()
            page.locator("li.search-result").first().click()
            page.waitForSelector(".seating-map .seat-highlight")
            assertThat(page.querySelector(".seat-highlight")).isNotNull()
        }
    }

    @Test
    fun `a deep link to a selected guest restores the seating map on load`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=8")
            page.waitForSelector(".seating-map .seat-highlight")
            assertThat(page.querySelector(".seating-map-name")?.textContent()).isEqualTo("Charlotte")
        }
    }

    @Test
    fun `a stale guest link falls back to the search results list`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Cha&guest=999999")
            page.waitForSelector("li.search-result")
            assertThat(page.querySelectorAll("li.search-result")).isNotEmpty()
            assertThat(page.querySelector(".seating-map")).isNull()
        }
    }

    @Test
    fun `searching a name with no matches shows the empty-state message`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/")
            page.fill("#guest-search", "Zzz")
            val message = page.waitForSelector("text=No guests found with this name.")
            assertThat(message.textContent()?.trim()).isEqualTo("No guests found with this name.")
        }
    }

    @Test
    fun `on a portrait phone viewport the seating map stays landscape`() {
        val portrait = Browser.NewContextOptions().setViewportSize(390, 844)
        newAuthenticatedContext(portrait).use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=8")
            page.waitForSelector(".seating-map svg")

            // The map is never rotated any more, so its computed transform is either "none" or an
            // identity matrix - never the (0, 1, -1, 0) quarter-turn the old portrait layout applied.
            val quarterTurned =
                page.evaluate(
                    """
                    () => {
                        const svg = document.querySelector('.seating-map svg');
                        const match = getComputedStyle(svg).transform.match(/matrix\(([^)]+)\)/);
                        if (!match) return false;
                        const [a, b, c, d] = match[1].split(',').map(Number);
                        const near = (value, target) => Math.abs(value - target) < 0.001;
                        return near(a, 0) && near(b, 1) && near(c, -1) && near(d, 0);
                    }
                    """.trimIndent(),
                )

            assertThat(quarterTurned as Boolean).isFalse()
        }
    }

    @Test
    fun `selecting a guest also renders a cropped close-up of their table`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=8")
            page.waitForSelector(".seating-map-detail svg .seat-highlight")

            // The close-up is cropped to the guest's table once the browser has laid it out, so it
            // carries its own viewBox and lights up exactly one seat.
            val viewBox = page.querySelector(".seating-map-detail svg")?.getAttribute("viewBox")
            assertThat(viewBox).isNotNull()
            assertThat(page.querySelectorAll(".seating-map-detail .seat-highlight")).hasSize(1)
        }
    }

    private companion object {
        const val TEST_SECRET = "123"

        private lateinit var playwright: Playwright
        private lateinit var browser: Browser

        @JvmStatic
        @BeforeAll
        fun launchBrowser() {
            playwright = Playwright.create()
            browser = playwright.chromium().launch()
        }

        @JvmStatic
        @AfterAll
        fun closeBrowser() {
            browser.close()
            playwright.close()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { TestDatabase.readOnlyUrl }
            registry.add("auth.secret") { TEST_SECRET }
        }
    }
}
