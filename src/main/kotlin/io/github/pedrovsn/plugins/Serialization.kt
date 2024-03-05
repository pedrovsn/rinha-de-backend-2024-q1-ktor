package io.github.pedrovsn.plugins

import com.fasterxml.jackson.databind.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
        jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
                disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            }
    }
    routing {
        get("/json/kotlinx-serialization") {
                call.respond(mapOf("hello" to "world"))
            }
        get("/json/jackson") {
                call.respond(mapOf("hello" to "world"))
            }
    }
}

@Serializer(forClass = LocalDateTime::class)
object DateSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}