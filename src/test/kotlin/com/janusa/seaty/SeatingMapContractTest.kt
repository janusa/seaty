package com.janusa.seaty

import com.janusa.seaty.support.AbstractDatabaseTest
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

/**
 * Guards the silent coupling between the seating-map SVG and the seat data. The frontend lights up a
 * guest's chair by composing the element id `table-{tableNumber}-seat-{seatNumber}` and looking it up
 * in the SVG; when nothing matches, the highlight just does nothing - no error, the seat simply never
 * lights up (the text caption is all that remains). That failure is invisible to every other test.
 *
 * Scope: like the rest of the suite, this runs against the sample data in `db/seed.sql`. It therefore
 * proves the *committed seed* stays consistent with the map - not the real guest list a deployment
 * loads into its own database. A production database built from different data is not covered here and
 * needs the same seat-to-chair check applied wherever that data is loaded.
 */
class SeatingMapContractTest : AbstractDatabaseTest() {
    @Test
    fun `every assigned seat has a matching chair on the seating map`() {
        val chairIds = chairIdsFromMap()
        // Guards against a renamed/moved SVG or a changed id scheme, which would otherwise make every
        // seat "missing" with a confusing failure instead of this clear one.
        assertThat(chairIds)
            .describedAs("chairs (id=\"table-N-seat-N\") found in the seating-map SVG")
            .isNotEmpty()

        val assignedSeatIds =
            jdbcClient
                .sql(
                    """
                    SELECT dt.table_number, s.seat_number
                    FROM seating_assignment sa
                    JOIN seat s ON s.id = sa.seat_id
                    JOIN dining_table dt ON dt.id = s.table_id
                    """.trimIndent(),
                ).query { rs, _ ->
                    "table-${rs.getLong("table_number")}-seat-${rs.getLong("seat_number")}"
                }.list()

        val missing = assignedSeatIds.filterNot { it in chairIds }
        assertThat(missing)
            .describedAs("assigned seats with no matching chair on the seating map")
            .isEmpty()
    }

    private fun chairIdsFromMap(): Set<String> {
        val markup =
            ClassPathResource("static/images/seating-map.svg")
                .inputStream
                .bufferedReader()
                .use { it.readText() }
        val doc = Jsoup.parse(markup, "", Parser.xmlParser())
        val chairId = Regex("""table-\d+-seat-\d+""")
        return doc
            .select("[id]")
            .map { it.id() }
            .filter(chairId::matches)
            .toSet()
    }
}
