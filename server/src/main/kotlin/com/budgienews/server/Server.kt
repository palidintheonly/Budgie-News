package com.budgienews.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.Connection

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val db = BudgieDatabase(environment.config)
    db.ensureSchema()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        post("/v1/account/sync") {
            val request = call.receive<AccountSyncRequest>()
            if (request.email.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email_required"))
                return@post
            }
            val response = db.syncAccount(request)
            call.respond(response)
        }
    }
}

private class BudgieDatabase(config: io.ktor.server.config.ApplicationConfig) {
    private val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.property("database.jdbcUrl").getString()
            username = config.property("database.username").getString()
            password = config.property("database.password").getString()
            maximumPoolSize = 5
        },
    )

    fun ensureSchema() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        display_name VARCHAR(120) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS user_settings (
                        user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
                        uk_location VARCHAR(80) NOT NULL DEFAULT 'United Kingdom',
                        default_section VARCHAR(40) NOT NULL DEFAULT 'HEADLINES',
                        default_source VARCHAR(40) NOT NULL DEFAULT 'ALL',
                        biometric_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                        breaking_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                        important_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                        send_app_statistics BOOLEAN NOT NULL DEFAULT TRUE,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_user_settings_user
                            FOREIGN KEY (user_id) REFERENCES users(id)
                            ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    fun syncAccount(request: AccountSyncRequest): AccountSyncResponse {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            val userId = upsertUser(connection, request)
            upsertSettings(connection, userId, request)
            connection.commit()
            return AccountSyncResponse(userId = userId, synced = true)
        }
    }

    private fun upsertUser(connection: Connection, request: AccountSyncRequest): Long {
        connection.prepareStatement(
            """
            INSERT INTO users (email, display_name)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.email.trim())
            statement.setString(2, request.displayName.ifBlank { "Budgie reader" }.trim())
            statement.executeUpdate()
        }

        connection.prepareStatement("SELECT id FROM users WHERE email = ?").use { statement ->
            statement.setString(1, request.email.trim())
            statement.executeQuery().use { rows ->
                if (rows.next()) return rows.getLong("id")
            }
        }
        error("Unable to load synced user")
    }

    private fun upsertSettings(connection: Connection, userId: Long, request: AccountSyncRequest) {
        connection.prepareStatement(
            """
            INSERT INTO user_settings (
                user_id,
                uk_location,
                default_section,
                default_source,
                biometric_enabled,
                breaking_notifications_enabled,
                important_notifications_enabled,
                send_app_statistics
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                uk_location = VALUES(uk_location),
                default_section = VALUES(default_section),
                default_source = VALUES(default_source),
                biometric_enabled = VALUES(biometric_enabled),
                breaking_notifications_enabled = VALUES(breaking_notifications_enabled),
                important_notifications_enabled = VALUES(important_notifications_enabled),
                send_app_statistics = VALUES(send_app_statistics)
            """.trimIndent(),
        ).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, request.ukLocation)
            statement.setString(3, request.defaultSection)
            statement.setString(4, request.defaultSource)
            statement.setBoolean(5, request.biometricEnabled)
            statement.setBoolean(6, request.breakingNotificationsEnabled)
            statement.setBoolean(7, request.importantNotificationsEnabled)
            statement.setBoolean(8, request.sendAppStatistics)
            statement.executeUpdate()
        }
    }
}

@Serializable
data class AccountSyncRequest(
    val email: String,
    val displayName: String,
    val ukLocation: String,
    val defaultSection: String,
    val defaultSource: String,
    val biometricEnabled: Boolean,
    val breakingNotificationsEnabled: Boolean,
    val importantNotificationsEnabled: Boolean,
    val sendAppStatistics: Boolean,
)

@Serializable
data class AccountSyncResponse(
    val userId: Long,
    val synced: Boolean,
)
