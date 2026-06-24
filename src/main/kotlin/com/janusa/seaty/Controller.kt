package com.janusa.seaty

import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/guests")
class Controller {

    @GetMapping()
    fun getGuests(
        @RequestParam @Size(
            min = 3,
            message = "Name must be at least 3 characters"
        ) name: String
    ): List<String> {
        return listOf("A", "B", "C")
    }
}