package com.doitsh.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val dbUrl = environment.config.propertyOrNull("db.url")?.getString()
        ?: "jdbc:sqlite:doitsh.db"
    val dbUser = environment.config.propertyOrNull("db.user")?.getString()
    val dbPassword = environment.config.propertyOrNull("db.password")?.getString()

    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        dbUrl.takeIf { it.contains("postgres") }?.let {
            username = dbUser
            password = dbPassword
        }
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    // Create tables
    transaction {
        SchemaUtils.create(TasksTable, ProjectsTable)
    }
}
