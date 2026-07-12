package com.janusa.seaty

import jakarta.validation.constraints.Size
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/guests")
class GuestController(
    private val guestRepository: GuestRepository,
) {
    @GetMapping()
    fun getGuests(
        @RequestParam @Size(
            min = 1,
            message = "Name must not be empty",
        ) name: String,
    ): List<Guest> {
        log.debug("Guest search requested with prefix='{}'", name)
        val result = guestRepository.findGuests(name)
        log.info("Guest search prefix='{}' returned {} match(es)", name, result.size)
        return result
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuestController::class.java)
    }
}
