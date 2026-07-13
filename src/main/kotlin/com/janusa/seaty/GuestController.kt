package com.janusa.seaty

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/guests")
class GuestController(
    private val guestRepository: GuestRepository,
) {
    @GetMapping()
    fun getAllGuests(): ResponseEntity<List<Guest>> {
        log.debug("All guests requested")
        val result = guestRepository.findAllGuests()
        log.info("All guests returned {} row(s)", result.size)
        // The roster is fixed for the event (read-only database), so let the browser reuse it across
        // reloads instead of refetching. Private, since it is served only to an authenticated session.
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(GUEST_LIST_MAX_AGE).cachePrivate())
            .body(result)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuestController::class.java)

        // How long a browser may reuse the guest list before refetching it.
        val GUEST_LIST_MAX_AGE: Duration = Duration.ofHours(1)
    }
}
