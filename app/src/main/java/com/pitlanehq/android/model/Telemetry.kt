package com.pitlanehq.android.model

data class TelemetryFrame(
    val timestampMs: Long = System.currentTimeMillis(),
    val speedKph: Double? = null,
    val rpm: Double? = null,
    val gear: Int? = null,
    val fuelLitres: Double? = null,
    val fuelLevelPct: Double? = null,
    val lap: Int? = null,
    val lapTimeMs: Long? = null,
    val sessionTimeMs: Long? = null
)

enum class ConnectionState { OFFLINE, CONNECTING, LIVE, STALE, ERROR }

data class PitWallConnection(
    val state: ConnectionState = ConnectionState.OFFLINE,
    val host: String = "192.168.1.100",
    val port: Int = 8080,
    val latencyMs: Long? = null,
    val message: String? = null
)
