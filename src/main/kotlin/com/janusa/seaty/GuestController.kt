package com.janusa.seaty

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/guests")
class GuestController(
    private val guestRepository: GuestRepository,
) {
    @GetMapping()
    fun getAllGuests(): List<Guest> {
        log.debug("All guests requested")
        val result = guestRepository.findAllGuests()
        log.info("All guests returned {} row(s)", result.size)
        return result
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuestController::class.java)
    }
}
