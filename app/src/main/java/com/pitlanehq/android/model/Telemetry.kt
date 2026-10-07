package com.pitlanehq.android.model

/** Live telemetry from TrackIQ.exe, relayed end-to-end encrypted through the TrackIQ server. */
data class LiveState(
    val link: LinkState = LinkState.OFF,
    val pcOnline: Boolean = false,
    val simConnected: Boolean = false,
    val values: Map<String, Any?> = emptyMap(),
    val drinks: Drinks? = null,
    val message: String? = null
) {
    fun num(name: String): Double? = (values[name] as? Number)?.toDouble()
}

/** DRINKS mode on the PC (admins only): friends drive and their laps go to the community under their name. */
data class Drinks(val admin: Boolean, val on: Boolean, val guest: String, val guestAuto: Boolean, val guests: List<String>, val driver: String)

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

data class CloudLap(val id: String, val n: Int, val time: Double, val valid: Boolean, val sectors: List<Double>, val inc: Int = 0)

/** A lap trace: one row every [bin] metres: speed m/s, throttle 0-1, brake 0-1, gear, steering rad, lap time s. */
data class Trace(val bin: Double, val rows: List<DoubleArray>, val x: List<Double>? = null, val y: List<Double>? = null, val inc: List<Double> = emptyList()) {
    /** The incidents of the lap: (distance m, points), from the trace's [d, pts, d, pts…]. */
    val incidents: List<Pair<Double, Int>> get() = (0 until inc.size / 2).map { inc[2 * it] to inc[2 * it + 1].toInt() }
    /** The shape of the track: where the car was at every row (TrackIQ 0.5 and later record it). */
    val hasShape get() = x != null && y != null && x.size == rows.size && x.size > 10
    val speedKph get() = rows.map { it[0] * 3.6 }
    val throttle get() = rows.map { it.getOrElse(1) { 0.0 } }
    val brake get() = rows.map { it.getOrElse(2) { 0.0 } }
    val time get() = rows.map { it.getOrElse(5) { 0.0 } }
}

/** Your best lap per track and car (GET /api/bests). */
data class PersonalBest(
    val track: String, val trackConfig: String, val car: String, val best: Double, val laps: Int, val last: Long,
    val bestLapId: String?, val bestSessionId: String?
)

/** A track and car the community has laps for (GET /community/combos). */
data class Combo(val trackId: Long, val track: String, val carId: Long, val car: String, val laps: Int, val best: Double?)

data class CommunityLap(val id: String, val alias: String, val time: Double, val created: Long, val hasTrace: Boolean, val sectors: List<Double>)

data class SharedReport(val id: String, val alias: String, val track: String, val car: String, val created: Long, val finish: Int, val field: Int, val best: Double?)

data class SharedSetup(val id: String, val alias: String, val name: String, val car: String, val track: String, val notes: String, val downloads: Int, val created: Long)

data class Device(val id: String, val device: String, val lastSeen: Long, val current: Boolean)

/** A race the PC recorded (races.json in the account sync). */
data class Race(
    val id: String, val whenMs: Long, val track: String, val car: String, val official: Boolean,
    val start: Int, val finish: Int, val field: Int, val inc: Int, val best: Double?, val fieldBest: Double?,
    val avg: Double?, val consistency: Double?, val pits: Int, val fuelUsed: Double?,
    val ir: Int, val irChange: Int, val sof: Int, val dnf: Boolean,
    val laps: List<RaceLap>, val results: List<RaceResult>
)

/** One lap of a race; [cut]: the car left the track, the lap is not valid. */
data class RaceLap(val n: Int, val time: Double, val pos: Int, val inc: Int, val pit: Boolean, val cut: Boolean = false)

data class RaceResult(val pos: Int, val name: String, val ir: Int, val best: Double?, val inc: Int, val laps: Int)

/** What a screen shows: [stale] is saved data shown while the server cannot be reached. */
data class Loadable<T>(val loading: Boolean = false, val data: T? = null, val error: String? = null, val stale: Boolean = false)

fun lapTime(s: Double?): String {
    if (s == null || s.isNaN() || s <= 0) return "—"
    val m = (s / 60).toInt()
    val r = s - m * 60
    return if (m > 0) "%d:%06.3f".format(m, r) else "%.3f".format(r)
}

fun signed(n: Int) = if (n > 0) "+$n" else "$n"
