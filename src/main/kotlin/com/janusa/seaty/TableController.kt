package com.janusa.seaty

import jakarta.validation.constraints.Positive
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tables")
class TableController(
    private val guestRepository: GuestRepository,
) {
    @GetMapping("/{tableNumber}/guests")
    fun getTableGuests(
        @PathVariable @Positive(
            message = "Table number must be positive",
        ) tableNumber: Long,
    ): List<Guest> {
        log.debug("Table roster requested for tableNumber={}", tableNumber)
        val result = guestRepository.findGuestsByTable(tableNumber)
        log.info("Table {} roster returned {} guest(s)", tableNumber, result.size)
        return result
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TableController::class.java)
    }
}
