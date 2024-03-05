package io.github.pedrovsn.plugins

import io.github.pedrovsn.exception.CustomerNotFoundException
import io.github.pedrovsn.exception.InsufficientFundsException
import io.github.pedrovsn.exception.InvalidAttributeException
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime

data class Transaction(val id: Int, val customerId: Int, val amount: Int, val type: String, val description: String, val createdAt: LocalDateTime?)

data class Customer(val id: Int, val name: String, val creditLimit: Int, val balance: Int)

data class CustomerCurrentStatus(val creditLimit: Int, val balance: Int)

data class CustomerTransactions(val customerId: Int, val creditLimit: Int, val balance: Int, val transactions: List<Transaction>)

class TransactionService(private val connection: Connection) {

    // Create new transaction
    suspend fun create(
        customerId: Int,
        transactionType: String,
        description: String,
        amount: Int
    ): CustomerCurrentStatus = withContext(Dispatchers.IO) {
        if (isValidCustomerId(customerId).not()) {
            throw CustomerNotFoundException("Invalid customerId")
        }

        if (isValidTransactionType(transactionType).not()) {
            throw InvalidAttributeException("Invalid transaction type")
        }

        if (isValidDescription(description).not()) {
            throw InvalidAttributeException("Invalid description")
        }

        val selectCustomerStatement = connection.prepareStatement(SELECT_CUSTOMER_BY_ID)
        selectCustomerStatement.setInt(1, customerId)
        val resultSet = selectCustomerStatement.executeQuery()

        val customer = if (resultSet.next()) {
            val id = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val creditLimit = resultSet.getInt("credit_limit")
            val balance = resultSet.getInt("balance")
            Customer(id, name, creditLimit, balance)
        } else {
            throw Exception("Record not found")
        }

        var currentBalance = customer.balance

        when (transactionType) {
            "c" -> {
                currentBalance += amount
            }
            "d" -> {
                val maxValue = currentBalance + customer.creditLimit

                if (amount > maxValue) {
                    throw InsufficientFundsException("Transaction amount exceeds customer limit")
                }

                currentBalance -= amount
            }
        }

        val updateCustomerStatement = connection.prepareStatement(UPDATE_CUSTOMER_BALANCE)
        updateCustomerStatement.setInt(1, currentBalance)
        updateCustomerStatement.setInt(2, customerId)
        updateCustomerStatement.executeUpdate()

        val createTransactionStatement = connection.prepareStatement(INSERT_TRANSACTION)
        createTransactionStatement.setInt(1, customerId)
        createTransactionStatement.setInt(2, amount)
        createTransactionStatement.setString(3, transactionType)
        createTransactionStatement.setString(4, description)
        createTransactionStatement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()))
        createTransactionStatement.executeUpdate()

        return@withContext CustomerCurrentStatus(customer.creditLimit, currentBalance)
    }

    // Read transactions by customer
    suspend fun read(customerId: Int): CustomerTransactions = withContext(Dispatchers.IO) {
        if (isValidCustomerId(customerId).not()) {
            throw CustomerNotFoundException("Invalid customerId")
        }

        val statement = connection.prepareStatement(SELECT_CUSTOMER_BY_ID)
        statement.setInt(1, customerId)
        val resultSet = statement.executeQuery()

        val customer = if (resultSet.next()) {
            val id = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val creditLimit = resultSet.getInt("credit_limit")
            val balance = resultSet.getInt("balance")
            Customer(id, name, creditLimit, balance)
        } else {
            throw Exception("Record not found")
        }

        val transactionsStatement = connection.prepareStatement(SELECT_TRANSACTIONS_BY_CUSTOMER_ID)
        transactionsStatement.setInt(1, customerId)
        transactionsStatement.setInt(2, 10)

        val rs = transactionsStatement.executeQuery()

        val transactions = mutableListOf<Transaction>()
        while (rs.next()) {
            val id = rs.getInt("id")
            val customerId = rs.getInt("customer_id")
            val amount = rs.getInt("amount")
            val type = rs.getString("type")
            val description = rs.getString("description")
            val createdAt = rs.getTimestamp("created_at").toLocalDateTime()

            transactions.add(Transaction(
                id,
                customerId,
                amount,
                type,
                description,
                createdAt
            ))
        }

        return@withContext CustomerTransactions(
            customerId,
            customer.creditLimit,
            customer.balance,
            transactions
        )
    }

    private fun isValidCustomerId(customerId: Int): Boolean {
        return customerId in 1..5
    }

    private fun isValidTransactionType(transactionType: String): Boolean {
        return transactionType == "c" || transactionType == "d"
    }

    private fun isValidDescription(description: String): Boolean {
        if (description.isBlank()) {
            return false
        }

        if (description.length > 10) {
            return false
        }

        return true
    }

    companion object {
        private const val SELECT_CUSTOMER_BY_ID = """
            SELECT id, name, credit_limit, balance FROM customer WHERE id = ? FOR UPDATE
            """
        private const val UPDATE_CUSTOMER_BALANCE = "UPDATE customer SET balance = ? WHERE id = ?"

        private const val INSERT_TRANSACTION = """
            INSERT INTO transaction (customer_id, amount, type, description, created_at) 
            VALUES (?, ?, ?, ?, ?)
            """

        private const val SELECT_TRANSACTIONS_BY_CUSTOMER_ID = """
            SELECT id, customer_id, amount, type, description, created_at
            FROM transaction
            WHERE customer_id = ? ORDER BY id DESC LIMIT ?
            """
    }
}
