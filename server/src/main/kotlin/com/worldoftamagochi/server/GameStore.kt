package com.worldoftamagochi.server

import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

data class Player(
    val id: String,
    val displayName: String,
)

/** A verified run to store. */
data class NewRun(
    val playerId: String,
    val trackId: String,
    val finishMicros: Long,
    val log: List<Int>,
    val simVersion: Int,
    val createdAtMs: Long,
)

data class StoredRun(
    val playerId: String,
    val displayName: String,
    val finishMicros: Long,
    val log: List<Int>,
)

/** All server data access, in plain JDBC: three queries do not need an ORM. */
class GameStore(
    private val db: DataSource,
) {
    fun createPlayer(
        id: String,
        tokenHash: String,
        displayName: String,
        nowMs: Long,
    ) = write("INSERT INTO players (id, token_hash, display_name, created_at_ms) VALUES (?, ?, ?, ?)", id, tokenHash, displayName, nowMs)

    fun playerByTokenHash(tokenHash: String): Player? =
        query(
            "SELECT id, display_name FROM players WHERE token_hash = ?",
            tokenHash,
        ) { Player(it.getString("id"), it.getString("display_name")) }
            .firstOrNull()

    fun insertRun(run: NewRun) =
        write(
            "INSERT INTO runs (player_id, track_id, finish_micros, log, sim_version, created_at_ms) VALUES (?, ?, ?, ?, ?, ?)",
            run.playerId,
            run.trackId,
            run.finishMicros,
            run.log.joinToString(","),
            run.simVersion,
            run.createdAtMs,
        )

    /** Each player's best run on [trackId] since [sinceMs], fastest first. */
    fun bestRuns(
        trackId: String,
        sinceMs: Long,
        simVersion: Int,
    ): List<StoredRun> =
        query(
            """
            SELECT r.player_id, p.display_name, r.finish_micros, r.log
            FROM runs r JOIN players p ON p.id = r.player_id
            WHERE r.track_id = ? AND r.created_at_ms >= ? AND r.sim_version = ?
              AND r.finish_micros = (
                SELECT MIN(r2.finish_micros) FROM runs r2
                WHERE r2.player_id = r.player_id AND r2.track_id = r.track_id
                  AND r2.created_at_ms >= ? AND r2.sim_version = ?
              )
            ORDER BY r.finish_micros, r.id
            """.trimIndent(),
            trackId,
            sinceMs,
            simVersion,
            sinceMs,
            simVersion,
        ) { StoredRun(it.getString("player_id"), it.getString("display_name"), it.getLong("finish_micros"), it.getString("log").toCodes()) }
            .distinctBy { it.playerId }

    private fun String.toCodes(): List<Int> = if (isEmpty()) emptyList() else split(',').map(String::toInt)

    private fun write(
        sql: String,
        vararg args: Any,
    ) {
        db.connection.use { c -> c.prepare(sql, args).use { it.executeUpdate() } }
    }

    private fun <T> query(
        sql: String,
        vararg args: Any,
        row: (ResultSet) -> T,
    ): List<T> =
        db.connection.use { c ->
            c.prepare(sql, args).use { statement ->
                statement.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(row(rs)) }
                }
            }
        }

    private fun Connection.prepare(
        sql: String,
        args: Array<out Any>,
    ) = prepareStatement(sql).apply { args.forEachIndexed { i, arg -> setObject(i + 1, arg) } }
}
