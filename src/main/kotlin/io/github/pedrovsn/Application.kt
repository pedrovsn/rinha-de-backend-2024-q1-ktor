package io.github.pedrovsn

import io.github.pedrovsn.exception.CustomerNotFoundException
import io.github.pedrovsn.exception.InsufficientFundsException
import io.github.pedrovsn.exception.InvalidAttributeException
import io.github.pedrovsn.plugins.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureRouting()
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondText(text = "422: $cause" , status = HttpStatusCode.UnprocessableEntity)
        }
        exception<InsufficientFundsException> { call, cause ->
            call.respondText(text = "422: $cause" , status = HttpStatusCode.UnprocessableEntity)
        }
        exception<InvalidAttributeException> { call, cause ->
            call.respondText(text = "422: $cause" , status = HttpStatusCode.UnprocessableEntity)
        }
        exception<CustomerNotFoundException> { call, cause ->
            call.respondText(text = "404: $cause" , status = HttpStatusCode.NotFound)
        }
    }
}