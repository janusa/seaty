package com.janusa.seaty

import org.assertj.core.api.Assertions.assertThat
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * Unit-tests the DOM-free routing logic in the real `static/scripts/script.js` using GraalJS, an
 * in-JVM JavaScript engine - no Node, no browser, no running server.
 *
 * The actual script file is loaded as-is. Its page bootstrap is guarded by a `typeof document` check,
 * so outside a browser only the pure function declarations take effect and this test drives them
 * directly. `URLSearchParams` is a browser/Node API that GraalJS doesn't provide, so a minimal
 * polyfill is loaded first, letting the production code run unchanged. Loading the file also doubles
 * as a syntax check of the whole script.
 */
class ScriptRoutingLogicTest {
    private lateinit var js: Context

    @BeforeEach
    fun setUp() {
        js =
            Context
                .newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build()
        js.eval("js", URL_SEARCH_PARAMS_POLYFILL)
        js.eval("js", SCRIPT_JS)
    }

    @AfterEach
    fun tearDown() {
        js.close()
    }

    private fun fn(name: String): Value = js.eval("js", name)

    @Test
    fun `buildQuery assembles the query for a name and a guest`() {
        assertThat(fn("buildQuery").execute("", "Charlie", 42).asString())
            .isEqualTo("?name=Charlie&guest=42")
    }

    @Test
    fun `buildQuery drops the guest when only a name is given`() {
        assertThat(fn("buildQuery").execute("", "Ann", null).asString())
            .isEqualTo("?name=Ann")
    }

    @Test
    fun `buildQuery returns null when the state is unchanged`() {
        assertThat(fn("buildQuery").execute("?name=Charlie&guest=42", "Charlie", 42).isNull)
            .isTrue()
    }

    @Test
    fun `buildQuery clears the query for an empty name`() {
        assertThat(fn("buildQuery").execute("?name=Ann", "   ", null).asString())
            .isEqualTo("")
    }

    @Test
    fun `parseQuery reads the name and guest id from the query string`() {
        val parsed = fn("parseQuery").execute("?name=Ann&guest=7")
        assertThat(parsed.getMember("name").asString()).isEqualTo("Ann")
        assertThat(parsed.getMember("guestId").asString()).isEqualTo("7")
    }

    @Test
    fun `parseQuery yields an empty name and no guest for an empty query`() {
        val parsed = fn("parseQuery").execute("")
        assertThat(parsed.getMember("name").asString()).isEqualTo("")
        assertThat(parsed.getMember("guestId").isNull).isTrue()
    }

    @Test
    fun `seatElementId composes the chair id the seating map uses`() {
        assertThat(fn("seatElementId").execute(3, 5).asString()).isEqualTo("table-3-seat-5")
    }

    @Test
    fun `tableLabel names a regular table by its number`() {
        assertThat(fn("tableLabel").execute(7).asString()).isEqualTo("Table 7")
    }

    @Test
    fun `tableLabel names the head table by name`() {
        assertThat(fn("tableLabel").execute(18).asString()).isEqualTo("Head Table")
    }

    @Test
    fun `isAlreadyRendered reports a key unseen the first time and seen the second`() {
        val isAlreadyRendered = fn("isAlreadyRendered")
        assertThat(isAlreadyRendered.execute("list:1,2").asBoolean()).isFalse()
        assertThat(isAlreadyRendered.execute("list:1,2").asBoolean()).isTrue()
    }

    @Test
    fun `isAlreadyRendered treats a different key as new and only remembers the latest`() {
        val isAlreadyRendered = fn("isAlreadyRendered")
        isAlreadyRendered.execute("map:1")
        // A changed key is unseen...
        assertThat(isAlreadyRendered.execute("map:2").asBoolean()).isFalse()
        // ...and switching back is unseen too, since only the most recent key is retained.
        assertThat(isAlreadyRendered.execute("map:1").asBoolean()).isFalse()
    }

    @Test
    fun `prepareRoster pins the selected guest first then orders the rest by seat`() {
        val roster =
            js.eval(
                "js",
                "prepareRoster([{id:29,seatNumber:3},{id:28,seatNumber:2},{id:27,seatNumber:1}], 28)",
            )
        assertThat(roster.arraySize).isEqualTo(3L)
        assertThat(roster.getArrayElement(0).getMember("id").asInt()).isEqualTo(28)
        assertThat(roster.getArrayElement(0).getMember("isSelf").asBoolean()).isTrue()
        assertThat(roster.getArrayElement(1).getMember("id").asInt()).isEqualTo(27)
        assertThat(roster.getArrayElement(2).getMember("id").asInt()).isEqualTo(29)
        assertThat(roster.getArrayElement(1).getMember("isSelf").asBoolean()).isFalse()
    }

    @Test
    fun `prepareRoster marks only the guest whose id matches as you`() {
        // Two guests share the first name "Charlie"; only the one whose id is selected is "you".
        val roster =
            js.eval(
                "js",
                "prepareRoster([{id:26,name:'Charlie',seatNumber:8},{id:27,name:'Charlie',seatNumber:1}], 27)",
            )
        assertThat(roster.getArrayElement(0).getMember("id").asInt()).isEqualTo(27)
        assertThat(roster.getArrayElement(0).getMember("isSelf").asBoolean()).isTrue()
        assertThat(roster.getArrayElement(1).getMember("id").asInt()).isEqualTo(26)
        assertThat(roster.getArrayElement(1).getMember("isSelf").asBoolean()).isFalse()
    }

    @Test
    fun `prepareRoster returns a single self row for a solo table`() {
        val roster = js.eval("js", "prepareRoster([{id:28,seatNumber:2}], 28)")
        assertThat(roster.arraySize).isEqualTo(1L)
        assertThat(roster.getArrayElement(0).getMember("isSelf").asBoolean()).isTrue()
    }

    @Test
    fun `seatNumberPosition pushes the label radially outward from the table centre`() {
        val position = fn("seatNumberPosition").execute(30, 100, 30, 122, 11)
        assertThat(position.getMember("x").asDouble()).isEqualTo(30.0)
        assertThat(position.getMember("y").asDouble()).isEqualTo(89.0)
    }

    private companion object {
        val SCRIPT_JS: String =
            ClassPathResource("static/scripts/script.js")
                .inputStream
                .bufferedReader()
                .use { it.readText() }

        // Minimal URLSearchParams covering only what script.js uses: a constructor that tolerates a
        // leading "?", get, set, and form-urlencoded toString (space as "+"). Test-only scaffolding.
        val URL_SEARCH_PARAMS_POLYFILL =
            """
            globalThis.URLSearchParams = class URLSearchParams {
                constructor(init = "") {
                    this._pairs = [];
                    let s = String(init);
                    if (s.startsWith("?")) s = s.slice(1);
                    for (const part of s.split("&")) {
                        if (!part) continue;
                        const i = part.indexOf("=");
                        const k = i === -1 ? part : part.slice(0, i);
                        const v = i === -1 ? "" : part.slice(i + 1);
                        this._pairs.push([
                            decodeURIComponent(k.replace(/\+/g, " ")),
                            decodeURIComponent(v.replace(/\+/g, " ")),
                        ]);
                    }
                }
                get(name) {
                    for (const [k, v] of this._pairs) if (k === name) return v;
                    return null;
                }
                set(name, value) {
                    this._pairs = this._pairs.filter(([k]) => k !== name);
                    this._pairs.push([name, String(value)]);
                }
                toString() {
                    const enc = (x) => encodeURIComponent(x).replace(/%20/g, "+");
                    return this._pairs.map(([k, v]) => enc(k) + "=" + enc(v)).join("&");
                }
            };
            """.trimIndent()
    }
}
