package com.janusa.seaty

import com.janusa.seaty.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test

/**
 * Smoke test: the application context boots. Extends [AbstractIntegrationTest] so it runs
 * against a seeded temp database - no hand-created `data/app.db` required.
 */
class SeatyApplicationTests : AbstractIntegrationTest() {
    @Test
    fun contextLoads() {
    }
}
