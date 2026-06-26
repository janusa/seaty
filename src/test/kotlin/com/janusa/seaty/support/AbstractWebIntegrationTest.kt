package com.janusa.seaty.support

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.web.context.WebApplicationContext

/**
 * Base class for full-context tests that drive HTTP endpoints. Adds a [RestTestClient] bound
 * to the mock servlet environment — no running server and no WebFlux/reactor dependency. The
 * same client API also binds to a live server, so these tests port directly if a real-server
 * (`webEnvironment = RANDOM_PORT`) variant is ever needed.
 */
abstract class AbstractWebIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var webContext: WebApplicationContext

    protected val restClient: RestTestClient by lazy {
        RestTestClient.bindToApplicationContext(webContext).build()
    }
}
