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
        context.setDefaultTimeout(DEFAULT_TIMEOUT_MS)
        context.addCookies(listOf(Cookie("session", TestDatabase.TEST_SECRET).setUrl(baseUrl)))
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
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
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
    fun `a single character returns results, and emptying the box restores the prompt`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/")
            // The invitation shows before any input.
            page.waitForSelector("text=Start typing to find your seat!")
            // A single character is enough to search now: matches appear.
            page.fill("#guest-search", "C")
            page.waitForSelector("li.search-result")
            assertThat(page.querySelectorAll("li.search-result")).isNotEmpty()
            // Emptying the box brings the original invitation back.
            page.fill("#guest-search", "")
            page.waitForSelector("text=Start typing to find your seat!")
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
    fun `a misspelled name still finds the intended guest`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/")
            // "Denis" is a one-edit misspelling of the seeded "Dennis"; near-match still finds him.
            page.fill("#guest-search", "Denis")
            page.waitForSelector("li.search-result")
            val names = page.querySelectorAll("li.search-result h2").map { it.textContent() }
            assertThat(names).contains("Dennis")
        }
    }

    @Test
    fun `an unaccented query matches an accented guest name`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/")
            // The seeded guest is "Zoë"; typing plain "Zoe" should still find her.
            page.fill("#guest-search", "Zoe")
            page.waitForSelector("li.search-result")
            val names = page.querySelectorAll("li.search-result h2").map { it.textContent() }
            assertThat(names).contains("Zoë")
        }
    }

    @Test
    fun `on a portrait phone viewport the seating map stays landscape`() {
        val portrait = Browser.NewContextOptions().setViewportSize(390, 844)
        newAuthenticatedContext(portrait).use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
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
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
            page.waitForSelector(".seating-map-detail svg .seat-highlight")

            // The close-up is cropped to the guest's table on the animation frame after it's attached.
            // Wait for a real (non-degenerate) viewBox: a near-empty box means getBBox ran before the
            // close-up was in the document - the failure mode when a fetch awaits mid-render.
            page.waitForFunction(
                """
                () => {
                    const svg = document.querySelector('.seating-map-detail svg');
                    const viewBox = svg?.getAttribute('viewBox');
                    if (!viewBox) return false;
                    const [, , width, height] = viewBox.split(' ').map(Number);
                    return width > 40 && height > 40;
                }
                """.trimIndent(),
            )
            assertThat(page.querySelectorAll(".seating-map-detail .seat-highlight")).hasSize(1)

            // A round table is labelled with its number in the middle of the close-up (guest 28 is at table 4).
            assertThat(page.querySelector(".seating-map-detail .table-label")?.textContent()).isEqualTo("4")
        }
    }

    @Test
    fun `the head table close-up is labelled Head Table`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            // Guest 135 (Zara) sits at the rectangular head table (table 17).
            page.navigate("$baseUrl/?name=Zara&guest=135")
            page.waitForSelector(".seating-map-detail .table-label")

            val label = page.querySelector(".seating-map-detail .table-label")
            assertThat(label?.textContent()).isEqualTo("Head Table")
            assertThat(label?.getAttribute("class")).contains("head-table-label")
        }
    }

    @Test
    fun `the roster lists the guests at the selected guest's table`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
            page.waitForSelector(".roster-list .roster-row")
            // Table 4 seats eight guests; the roster lists all of them (this deep link also proves the
            // roster is restored on load, not only after a click).
            assertThat(page.querySelectorAll(".roster-list .roster-row")).hasSize(8)
        }
    }

    @Test
    fun `the selected guest is pinned first`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
            page.waitForSelector(".roster-list .roster-row")
            val first = page.querySelector(".roster-list .roster-row")
            assertThat(first?.getAttribute("class")).contains("roster-self")
            assertThat(first?.textContent()).contains("Charlotte")
        }
    }

    @Test
    fun `faint seat numbers are drawn on the close-up only`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Charlotte&guest=28")
            page.waitForSelector(".seating-map-detail .seat-number-label")
            // One faint number per chair on the table-4 close-up...
            assertThat(page.querySelectorAll(".seating-map-detail .seat-number-label")).hasSize(8)
            // ...and none on the full room map, which stays uncluttered.
            assertThat(page.querySelectorAll(".seating-map .seat-number-label")).isEmpty()
        }
    }

    @Test
    fun `the head table seat numbers stack above and below their chairs`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            // Guest 135 (Zara) sits at the rectangular head table (table 17): two straight rows of four.
            page.navigate("$baseUrl/?name=Zara&guest=135")
            page.waitForSelector(".seating-map-detail .seat-number-label")

            // Each faint number sits straight over its chair (same x as a chair column) and clears the
            // row vertically - above the top row or below the bottom row - rather than fanning out on a
            // radial as it would on a round table. This is the defining property of the stacked layout.
            val stacked =
                page.evaluate(
                    """
                    () => {
                        const detail = document.querySelector('.seating-map-detail svg');
                        const chairs = [...detail.querySelectorAll('circle')];
                        const columns = new Set(chairs.map((c) => Number(c.getAttribute('cx'))));
                        const cys = chairs.map((c) => Number(c.getAttribute('cy')));
                        const topRow = Math.min(...cys);
                        const bottomRow = Math.max(...cys);
                        const labels = [...detail.querySelectorAll('.seat-number-label')];
                        return labels.every((label) => {
                            const x = Number(label.getAttribute('x'));
                            const y = Number(label.getAttribute('y'));
                            return columns.has(x) && (y < topRow || y > bottomRow);
                        });
                    }
                    """.trimIndent(),
                )
            assertThat(stacked as Boolean).isTrue()
        }
    }

    @Test
    fun `the head table roster lists its eight guests`() {
        newAuthenticatedContext().use { context ->
            val page = context.newPage()
            page.navigate("$baseUrl/?name=Zara&guest=135")
            page.waitForSelector(".roster-list .roster-row")
            assertThat(page.querySelectorAll(".roster-list .roster-row")).hasSize(8)
        }
    }

    @Test
    fun `a ten-seat table roster renders in full on a portrait phone`() {
        val portrait = Browser.NewContextOptions().setViewportSize(390, 844)
        newAuthenticatedContext(portrait).use { context ->
            val page = context.newPage()
            // Guest 14 (Bobby) sits at table 2, a ten-seat round table.
            page.navigate("$baseUrl/?name=Bobby&guest=14")
            page.waitForSelector(".roster-list .roster-row")
            assertThat(page.querySelectorAll(".roster-list .roster-row")).hasSize(10)
        }
    }

    private companion object {
        // Keep the browser waits short so a broken selector fails fast instead of hanging on
        // Playwright's 30s default and dragging out the feedback loop.
        const val DEFAULT_TIMEOUT_MS = 2_000.0

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
        fun springProperties(registry: DynamicPropertyRegistry) {
            TestDatabase.registerProperties(registry)
        }
    }
}
