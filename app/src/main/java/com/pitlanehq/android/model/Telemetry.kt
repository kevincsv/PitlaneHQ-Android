package com.pitlanehq.android.model

/** Live telemetry from PitlaneHQ.exe, relayed end-to-end encrypted through the Pitlane HQ server. */
data class LiveState(
    val link: LinkState = LinkState.OFF,
    val pcOnline: Boolean = false,
    val simConnected: Boolean = false,
    val values: Map<String, Any?> = emptyMap(),
    val message: String? = null
) {
    fun num(name: String): Double? = (values[name] as? Number)?.toDouble()
}

enum class LinkState { OFF, CONNECTING, OPEN }

/** The variables the phone asks the PC for. */
val LIVE_VARS = listOf(
    "Speed", "RPM", "Gear", "FuelLevel", "Lap", "LapCurrentLapTime", "LapLastLapTime", "LapBestLapTime",
    "LapDeltaToBestLap", "Throttle", "Brake", "PlayerCarPosition"
)

/** One of your own sessions uploaded by the PC (GET /api/sessions). */
data class CloudSession(
    val id: String,
    val started: Long,
    val track: String,
    val trackConfig: String,
    val car: String,
    val kind: String,
    val laps: Int,
    val best: Double?
)

data class CloudLap(val n: Int, val time: Double, val valid: Boolean, val sectors: List<Double>)

/** A track and car the community has laps for (GET /community/combos). */
data class Combo(val trackId: Long, val track: String, val carId: Long, val car: String, val laps: Int, val best: Double?)

data class CommunityLap(val alias: String, val time: Double, val created: Long)

data class Loadable<T>(val loading: Boolean = false, val data: T? = null, val error: String? = null)

fun lapTime(s: Double?): String {
    if (s == null || s.isNaN() || s <= 0) return "—"
    val m = (s / 60).toInt()
    val r = s - m * 60
    return if (m > 0) "%d:%06.3f".format(m, r) else "%.3f".format(r)
}
