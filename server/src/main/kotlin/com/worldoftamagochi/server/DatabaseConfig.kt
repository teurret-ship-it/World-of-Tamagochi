package com.worldoftamagochi.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

/** Where the server keeps its data; from the environment in production, H2 in tests. */
data class DatabaseConfig(
    val url: String,
    val user: String = "",
    val password: String = "",
) {
    companion object {
        fun fromEnvironment(): DatabaseConfig =
            DatabaseConfig(
                url = System.getenv("DATABASE_URL") ?: inMemory("wot").url,
                user = System.getenv("DATABASE_USER") ?: "",
                password = System.getenv("DATABASE_PASSWORD") ?: "",
            )

        /** H2 speaking PostgreSQL, for development and tests. */
        fun inMemory(name: String): DatabaseConfig =
            DatabaseConfig("jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
    }
}

/** Opens a pooled connection and brings the schema up to date. */
fun openDatabase(config: DatabaseConfig): DataSource {
    val dataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.user
                password = config.password
                maximumPoolSize = POOL_SIZE
            },
        )
    Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()
    return dataSource
}

private const val POOL_SIZE = 8
