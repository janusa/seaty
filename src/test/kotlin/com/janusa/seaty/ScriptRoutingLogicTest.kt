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

    private fun matchScore(
        name: String,
        foldedQuery: String,
    ): Int = js.eval("js", "guestMatchScore({name:'$name'}, '$foldedQuery')").asInt()

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
        assertThat(fn("tableLabel").execute(17).asString()).isEqualTo("Head Table")
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
    fun `guestsAtTable keeps only the guests seated at the given table`() {
        val roster =
            js.eval(
                "js",
                "guestsAtTable([{id:1,tableNumber:4},{id:2,tableNumber:2},{id:3,tableNumber:4}], 4)",
            )
        assertThat(roster.arraySize).isEqualTo(2L)
        assertThat(roster.getArrayElement(0).getMember("id").asInt()).isEqualTo(1)
        assertThat(roster.getArrayElement(1).getMember("id").asInt()).isEqualTo(3)
    }

    @Test
    fun `seatNumberPosition pushes the label radially outward from the table centre`() {
        val position = fn("seatNumberPosition").execute(30, 100, 30, 122, 11)
        assertThat(position.getMember("x").asDouble()).isEqualTo(30.0)
        assertThat(position.getMember("y").asDouble()).isEqualTo(89.0)
    }

    @Test
    fun `stackedSeatNumberPosition lifts a top-row seat straight up and drops a bottom-row seat straight down`() {
        val stacked = fn("stackedSeatNumberPosition")
        // Head-table centre y is 198; a top-row seat (y=176) gets its number 10 units above it...
        val top = stacked.execute(241, 176, 198, 10)
        assertThat(top.getMember("x").asDouble()).isEqualTo(241.0)
        assertThat(top.getMember("y").asDouble()).isEqualTo(166.0)
        // ...and a bottom-row seat (y=220) gets its number 10 units below, x unchanged in both cases.
        val bottom = stacked.execute(241, 220, 198, 10)
        assertThat(bottom.getMember("x").asDouble()).isEqualTo(241.0)
        assertThat(bottom.getMember("y").asDouble()).isEqualTo(230.0)
    }

    // Near-match search: the pure matching helpers (foldName, fuzzyPrefixDistance, guestMatchScore,
    // matchGuests) that back the client-side guest search. Passing plain objects and strings mirrors
    // how prepareRoster is exercised above.

    @Test
    fun `the JS engine strips diacritics via NFD normalize and the Unicode property escape`() {
        // Guards foldName's approach: if a future engine lacks \p{Diacritic}, this canary fails first.
        val stripped =
            js.eval("js", """ "Zoë".normalize("NFD").replace(/\p{Diacritic}/gu, "") """).asString()
        assertThat(stripped).isEqualTo("Zoe")
    }

    @Test
    fun `foldName strips accents and lowercases`() {
        val fold = fn("foldName")
        assertThat(fold.execute("Zoë").asString()).isEqualTo("zoe")
        assertThat(fold.execute("María").asString()).isEqualTo("maria")
        assertThat(fold.execute("Irène").asString()).isEqualTo("irene")
        assertThat(fold.execute("ALICE").asString()).isEqualTo("alice")
    }

    @Test
    fun `fuzzyPrefixDistance treats trailing characters of the name as free`() {
        val distance = fn("fuzzyPrefixDistance")
        // A query that is already a prefix costs nothing, however much name follows it.
        assertThat(distance.execute("cha", "charlie").asInt()).isEqualTo(0)
        // A single missing or wrong character costs one edit.
        assertThat(distance.execute("jon", "john").asInt()).isEqualTo(1)
        assertThat(distance.execute("denis", "dennis").asInt()).isEqualTo(1)
    }

    @Test
    fun `guestMatchScore ranks prefix over word-start over fuzzy and honours the length floor`() {
        assertThat(matchScore("Charlie", "cha")).isEqualTo(0)
        assertThat(matchScore("Mary Chase", "cha")).isEqualTo(1)
        assertThat(matchScore("Dennis", "denis")).isEqualTo(11)
        // Below the four-character floor a non-prefix query never fuzzy-matches, so it can't flood.
        assertThat(matchScore("Charlie", "cba")).isEqualTo(-1)
        // Beyond the edit-distance threshold there is no match either.
        assertThat(matchScore("Charlotte", "zzzz")).isEqualTo(-1)
        // Folding lets an unaccented query match at the prefix tier.
        assertThat(matchScore("María", "maria")).isEqualTo(0)
    }

    @Test
    fun `matchGuests ranks the tiers then alphabetises within a tier`() {
        val ranked =
            js.eval(
                "js",
                "matchGuests('cha', [{id:3,name:'Chase'},{id:2,name:'Mary Chase'},{id:1,name:'Charlie'}])",
            )
        assertThat(ranked.arraySize).isEqualTo(3L)
        assertThat(ranked.getArrayElement(0).getMember("name").asString()).isEqualTo("Charlie")
        assertThat(ranked.getArrayElement(1).getMember("name").asString()).isEqualTo("Chase")
        assertThat(ranked.getArrayElement(2).getMember("name").asString()).isEqualTo("Mary Chase")
    }

    @Test
    fun `matchGuests does not fuzzy-flood on a short non-prefix query`() {
        val ranked = js.eval("js", "matchGuests('cba', [{id:1,name:'Charlie'},{id:2,name:'Chase'}])")
        assertThat(ranked.arraySize).isEqualTo(0L)
    }

    @Test
    fun `matchGuests finds an accented name from an unaccented query`() {
        val ranked = js.eval("js", "matchGuests('zoe', [{id:144,name:'Zoë'}])")
        assertThat(ranked.arraySize).isEqualTo(1L)
        assertThat(ranked.getArrayElement(0).getMember("name").asString()).isEqualTo("Zoë")
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
