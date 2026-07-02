package com.janusa.seaty

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SeatyApplication

// SpreadOperator: runApplication's vararg is the idiomatic Spring Boot entrypoint; the array copy is
// negligible and unavoidable. MissingUseCall: the returned ApplicationContext must stay open for the
// app's lifetime - wrapping it in use { } would close it immediately and shut the app down.
@Suppress("SpreadOperator", "MissingUseCall")
fun main(args: Array<String>) {
    runApplication<SeatyApplication>(*args)
}
