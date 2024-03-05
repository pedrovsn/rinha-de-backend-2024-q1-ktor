package io.github.pedrovsn.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.*
import java.time.LocalDateTime
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(val tipo: String, val valor: Int, val descricao: String)

@Serializable
data class CreateTransactionResponse(val saldo: Int, val limite: Int)

@Serializable
data class TransactionDto(
    val valor: Int,
    val tipo: String,
    val descricao: String,
    @Serializable(with = DateSerializer::class) val realizadaEm: LocalDateTime
)

@Serializable
data class BalanceDto(
    val total: Int,
    val limite: Int,
    @Serializable(with = DateSerializer::class) val dataExtrato: LocalDateTime
)

@Serializable
data class TransactionHistoryResponse(val saldo: BalanceDto, val ultimasTransacoes: List<TransactionDto>)

fun Application.configureRouting() {
    val dbConnection: Connection = connectToPostgres(embedded = false)
    val transactionService = TransactionService(dbConnection)
    routing {
        // Create a Transaction for customer
        post("/clientes/{id}/transacoes") {
            val customerId = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val createTransactionRequest = call.receive<CreateTransactionRequest>()

            val customerCurrentStatus = transactionService.create(
                customerId = customerId,
                transactionType = createTransactionRequest.tipo,
                description = createTransactionRequest.descricao,
                amount = createTransactionRequest.valor
            )

            call.respond(
                HttpStatusCode.OK,
                CreateTransactionResponse(
                    customerCurrentStatus.balance,
                    customerCurrentStatus.creditLimit
                )
            )
        }
        // Read last 10 Transactions for customer
        get("/clientes/{id}/extrato") {
            val customerId = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")

            val customerTransactions = transactionService.read(customerId)

            call.respond(
                HttpStatusCode.OK,
                TransactionHistoryResponse(
                    BalanceDto(
                        total = customerTransactions.balance,
                        limite = customerTransactions.creditLimit,
                        dataExtrato = LocalDateTime.now()
                    ),
                    ultimasTransacoes = customerTransactions.transactions.map {
                        TransactionDto(
                            valor = it.amount,
                            tipo = it.type,
                            descricao = it.description,
                            realizadaEm = it.createdAt!!
                        )
                    }
                )
            )
        }
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        DriverManager.getConnection(url, user, password)
    }
}