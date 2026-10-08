package com.pitlanehq.android.model

/** Live telemetry from PitlaneHQ.exe, relayed end-to-end encrypted through the Pitlane HQ server. */
data class LiveState(
    val link: LinkState = LinkState.OFF,
    val pcOnline: Boolean = false,
    val simConnected: Boolean = false,
    val values: Map<String, Any?> = emptyMap(),
    val drinks: Drinks? = null,
    val message: String? = null,
    val mode: LiveMode = LiveMode.IDLE,   // what this screen watches
    val code: String = "",                // the code of the driver you watch (CODE)
    val myCode: String = ""               // your share code, as your PC says (OWN)
) {
    fun num(name: String): Double? = (values[name] as? Number)?.toDouble()
}

/** IDLE: only whether your PC is online (it sends nothing); OWN: you watch your PC; CODE: you watch someone's share code. */
enum class LiveMode { IDLE, OWN, CODE }

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
    val best: Double?,
    val cat: String? = null,   // the discipline: oval, sports_car, formula_car, dirt_oval, dirt_road
    val lic: String? = null    // your license class in it: R, D, C, B, A, P
)

data class CloudLap(val id: String, val n: Int, val time: Double, val valid: Boolean, val sectors: List<Double>, val inc: Int = 0)

/** A lap trace: one row every [bin] metres: speed m/s, throttle 0-1, brake 0-1, gear, steering rad, lap time s. */
/** One incident of a lap: where (m), its points and what it was ("off", "loss", "light" contact or "contact"). */
data class Incident(val d: Double, val pts: Int, val kind: String)

data class Trace(val bin: Double, val rows: List<DoubleArray>, val x: List<Double>? = null, val y: List<Double>? = null, val inc: List<Double> = emptyList(), val incK: List<String> = emptyList()) {
    /** The incidents of the lap, from the trace's [d, pts, d, pts…] and their kinds (older laps: by the points). */
    val incidents: List<Incident> get() = (0 until inc.size / 2).map { i ->
        val pts = inc[2 * i + 1].toInt()
        Incident(inc[2 * i], pts, incK.getOrNull(i) ?: when { pts >= 4 -> "contact"; pts == 2 -> "loss"; else -> "off" })
    }
    /** The shape of the track: where the car was at every row (Pitlane HQ 0.5 and later record it). */
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
data class Combo(val trackId: Long, val track: String, val carId: Long, val car: String, val laps: Int, val best: Double?, val cat: String? = null)

/** `mine`: the signed-in driver's own lap, also when it was shared anonymously (only they see that). */
data class CommunityLap(val id: String, val alias: String, val time: Double, val created: Long, val hasTrace: Boolean, val sectors: List<Double>, val mine: Boolean = false,
    val lic: String? = null, val prof: Boolean = false, val sup: Boolean = false, val field: Boolean = false)

/** A driver's profile: their nickname (never their iRacing name), license classes, recent races and laps on the leaderboards. */
data class DriverProfile(val name: String, val since: Long, val mine: Boolean, val admin: Boolean, val anonymous: Boolean, val supporter: Boolean, val supporterHidden: Boolean,
    val lics: Map<String, String>, val races: List<ProfileRace>, val laps: List<ProfileLap>, val days: Map<String, Int> = emptyMap())
data class ProfileRace(val whenMs: Long, val track: String, val car: String, val cat: String?, val lic: String?, val official: Boolean, val start: Int, val finish: Int, val field: Int,
    val inc: Int, val best: Double?, val irChange: Int, val dnf: Boolean)
data class ProfileLap(val track: String, val car: String, val time: Double, val created: Long, val cat: String?, val lic: String?, val anon: Boolean,
    val trackId: Long = 0, val carId: Long = 0, val pos: Int = 0, val of: Int = 0)

/** A league posted on Pitlane HQ, with a direct link to its Discord. */
data class League(val id: String, val name: String, val about: String, val cat: String?, val discord: String, val web: String, val schedule: String, val cars: String, val lang: String, val mine: Boolean = false, val by: String = "")

data class SharedReport(val id: String, val alias: String, val track: String, val car: String, val created: Long, val finish: Int, val field: Int, val best: Double?, val mine: Boolean = false)

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
