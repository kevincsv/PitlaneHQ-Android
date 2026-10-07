package com.pitlanehq.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.pitlanehq.android.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val SERVER = "https://pitlanehq.app"

data class AccountState(
    val signedIn: Boolean = false,
    val email: String = "",
    val display: String = "",
    val verified: Boolean = true,
    val admin: Boolean = false,
    val busy: Boolean = false,
    val syncedFiles: Int = 0,
    val syncVersion: Long = 0,
    val syncUpdated: Long = 0,
    val error: String? = null,
    val needCode: Boolean = false,      // the password was right: the authenticator code comes next
    val twoFactor: Boolean = false,     // two-step sign-in is on for this account
    val recoveryLeft: Int = 0
)

/** What the server gives to set up two-step sign-in: the key and the otpauth link for the app. */
data class TwoFactorSetup(val secret: String, val url: String)

/** Errors the screens show in the user's language: the message is a key of I18n. */
open class AppError(key: String) : Exception(key)
class SignedOut : AppError("signed_out")
class Offline : AppError("no_internet")

/** Data from the server, or the copy saved on this phone when the server cannot be reached ([stale]). */
class Got<T>(val data: T, val stale: Boolean = false)

/**
 * The Pitlane HQ account, exactly like the PC and the web app: the password only derives the
 * keys on this phone, the server gets the login key and gives back the data key sealed with
 * the wrap key. The session token, the data key and everything saved for offline use are kept
 * sealed with an Android Keystore key.
 */
class AccountRepository(context: Context) {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val prefs = context.getSharedPreferences("pitlane-account", Context.MODE_PRIVATE)
    private val app = context.getSharedPreferences("pitlane-app", Context.MODE_PRIVATE)
    private val cacheDir = File(context.filesDir, "saved").apply { mkdirs() }

    // demo data is only for the admins of the server (ADMINS), for testing
    var demo: Boolean
        get() = app.getBoolean("demo", false) && prefs.getBoolean("admin", false)
        set(v) = app.edit().putBoolean("demo", v).apply()
    var language: String
        get() = app.getString("lang", "system") ?: "system"
        set(v) = app.edit().putString("lang", v).apply()

    fun storedState() = AccountState(
        signedIn = prefs.getString("token", null) != null,
        email = prefs.getString("email", "") ?: "",
        display = prefs.getString("display", "") ?: "",
        verified = prefs.getBoolean("verified", true),
        admin = prefs.getBoolean("admin", false),
        syncedFiles = prefs.getInt("syncedFiles", 0),
        syncVersion = prefs.getLong("syncVersion", 0),
        syncUpdated = prefs.getLong("syncUpdated", 0),
        twoFactor = prefs.getBoolean("twoFactor", false),
        recoveryLeft = prefs.getInt("recoveryLeft", 0)
    )

    // ---------- device storage ----------
    private fun deviceKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry("pitlanehq-account", null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder("pitlanehq-account", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return g.generateKey()
    }

    private fun protectBytes(v: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, deviceKey())
        return c.iv + c.doFinal(v)
    }

    private fun unprotectBytes(b: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, b, 0, 12))
        return c.doFinal(b, 12, b.size - 12)
    }

    private fun protect(v: ByteArray) = Base64.encodeToString(protectBytes(v), Base64.NO_WRAP)
    private fun unprotect(v: String) = unprotectBytes(Base64.decode(v, Base64.NO_WRAP))

    fun token(): String = runCatching { String(unprotect(prefs.getString("token", null)!!)) }.getOrElse { throw SignedOut() }
    fun dataKey(): ByteArray = runCatching { unprotect(prefs.getString("dataKey", null)!!) }.getOrElse { throw SignedOut() }

    // saved copies for offline use, sealed like the token
    private fun save(name: String, text: String) = runCatching { File(cacheDir, name).writeBytes(protectBytes(text.toByteArray())) }
    private fun saved(name: String): String? = runCatching { String(unprotectBytes(File(cacheDir, name).readBytes())) }.getOrNull()
    private fun safeName(s: String) = s.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(120)

    private fun clearAll() {
        prefs.edit().clear().apply()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    // ---------- server ----------
    private fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): String {
        val q = Request.Builder().url(SERVER + path).method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        if (auth) q.header("Authorization", "Bearer " + token())
        val r = try {
            client.newCall(q.build()).execute()
        } catch (e: IOException) {
            throw Offline()
        }
        r.use {
            val txt = it.body.string()
            if (it.code == 401 && auth) {
                clearAll()
                throw SignedOut()
            }
            if (!it.isSuccessful) {
                if (it.code >= 500) throw AppError("server_down")
                throw AppError(runCatching { JSONObject(txt).optString("error") }.getOrNull()?.ifBlank { null } ?: "server_down")
            }
            return txt
        }
    }

    /** A GET whose answer is saved, so the screen still has it without a connection. */
    private fun cachedGet(path: String): Got<String> = try {
        call("GET", path).also { save(safeName(path), it) }.let { Got(it) }
    } catch (e: Offline) {
        Got(saved(safeName(path)) ?: throw e, stale = true)
    } catch (e: AppError) {
        if (e.message == "server_down") Got(saved(safeName(path)) ?: throw e, stale = true) else throw e
    }

    // a sign-in waiting for its authenticator code (5 minutes): the key stays here, never stored
    private var pending: Triple<String, String, ByteArray>? = null // pending token, email, wrap key

    fun login(email0: String, password: String): AccountState {
        val email = email0.trim().lowercase()
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) throw AppError("bad_email")
        if (password.isEmpty()) throw AppError("enter_pw")
        val k = Crypto.derive(email, password)
        val j = JSONObject(call("POST", "/account/login", JSONObject().put("email", email).put("auth", k.auth).put("device", "Android"), auth = false))
        if (j.optBoolean("twoFactor") && j.has("pending")) {
            pending = Triple(j.getString("pending"), email, k.wrap)
            return AccountState(email = email, needCode = true)
        }
        return signedIn(j, email, k.wrap)
    }

    /** Second step: the 6-digit code of the authenticator app, or a recovery code. */
    fun loginCode(code: String): AccountState {
        val p = pending ?: throw AppError("sign_in_again")
        val j = JSONObject(call("POST", "/account/login/2fa", JSONObject().put("pending", p.first).put("code", code.trim()), auth = false))
        pending = null
        return signedIn(j, p.second, p.third)
    }

    private fun signedIn(j: JSONObject, email: String, wrap: ByteArray): AccountState {
        val key = Crypto.open(wrap, j.getString("wrappedKey"), Crypto.ACCOUNT_AAD)
        require(key.size == 32) { "The account key is damaged" }
        prefs.edit()
            .putString("token", protect(j.getString("token").toByteArray()))
            .putString("dataKey", protect(key))
            .putString("email", email)
            .putString("display", j.optString("display", ""))
            .putBoolean("verified", j.optBoolean("verified", true))
            .putBoolean("admin", j.optBoolean("admin", false))
            .putBoolean("twoFactor", j.optBoolean("twoFactor", false))
            .apply()
        return runCatching { sync() }.getOrElse { storedState() }
    }

    // ---------- two-step sign-in (optional, recommended) ----------
    fun setup2fa(password: String): TwoFactorSetup {
        val k = Crypto.derive(email(), password)
        val j = JSONObject(call("POST", "/account/2fa/setup", JSONObject().put("auth", k.auth)))
        return TwoFactorSetup(j.getString("secret"), j.getString("url"))
    }

    /** Confirms the first code; gives the recovery codes (shown once). */
    fun enable2fa(code: String): List<String> {
        val j = JSONObject(call("POST", "/account/2fa/enable", JSONObject().put("code", code.trim())))
        val a = j.optJSONArray("codes") ?: JSONArray()
        prefs.edit().putBoolean("twoFactor", true).putInt("recoveryLeft", a.length()).apply()
        return List(a.length()) { a.getString(it) }
    }

    fun disable2fa(password: String, code: String) {
        val k = Crypto.derive(email(), password)
        call("POST", "/account/2fa/disable", JSONObject().put("auth", k.auth).put("code", code.trim()))
        prefs.edit().putBoolean("twoFactor", false).putInt("recoveryLeft", 0).apply()
    }

    private fun email() = prefs.getString("email", "") ?: ""

    // the inbox: which items were dismissed (ids), kept on this phone
    fun inboxSeen(): Set<String> = prefs.getStringSet("inboxSeen", emptySet()) ?: emptySet()
    fun inboxMark(ids: Collection<String>) { prefs.edit().putStringSet("inboxSeen", inboxSeen() + ids).apply() }

    /** Pulls the encrypted settings bundle the PC keeps in the account and opens it here: your races come from it. */
    fun sync(): AccountState {
        val j = JSONObject(call("GET", "/account/sync"))
        val blob = j.optString("blob", "")
        var files = 0
        if (blob.isNotEmpty()) {
            val all = JSONObject(String(Crypto.gunzip(Crypto.open(dataKey(), blob, Crypto.ACCOUNT_AAD))))
            files = all.length()
            // Go writes the files as base64 strings
            all.optString("races.json").takeIf { it.isNotEmpty() }?.let { b64 ->
                runCatching { save("races", trimRaces(JSONArray(String(Base64.decode(b64, Base64.DEFAULT))))) }
            }
        }
        prefs.edit()
            .putInt("syncedFiles", files)
            .putLong("syncVersion", j.optLong("version", 0))
            .putLong("syncUpdated", j.optLong("updated", 0))
            .apply()
        runCatching {
            val me = JSONObject(call("GET", "/account/me"))
            prefs.edit().putString("display", me.optString("display", "")).putBoolean("verified", me.optBoolean("verified", true)).putBoolean("admin", me.optBoolean("admin", false))
                .putBoolean("twoFactor", me.optBoolean("twoFactor", false)).putInt("recoveryLeft", me.optInt("recoveryLeft", 0)).apply()
        }
        return storedState()
    }

    // only what the phone shows, newest first: races.json also has braking points and more
    private fun trimRaces(a: JSONArray): String {
        val out = (0 until a.length()).mapNotNull { a.optJSONObject(it) }
            .filter { it.optString("game", "").let { g -> g.isEmpty() || g == "iracing" } }
            .sortedByDescending { it.optLong("when") }
            .take(60)
            .map { r ->
                val laps = r.optJSONArray("laps") ?: JSONArray()
                // the race's incident total, like the web: the largest of the report's total, the laps' and the events'
                val ev = r.optJSONArray("incidents")
                val fromLaps = (0 until laps.length()).sumOf { laps.optJSONObject(it)?.optInt("i") ?: 0 }
                val fromEv = ev?.let { e -> (0 until e.length()).sumOf { e.optJSONObject(it)?.let { x -> x.optInt("pts", x.optInt("p")) } ?: 0 } } ?: 0
                r.put("inc", maxOf(r.optInt("inc"), fromLaps, fromEv))
                r.remove("brakes"); r.remove("incidents")
                val slim = JSONArray()
                for (i in 0 until laps.length()) laps.optJSONObject(i)?.let { l ->
                    slim.put(JSONObject().put("n", l.optInt("n")).put("t", l.optDouble("t")).put("p", l.optInt("p")).put("i", l.optInt("i")).put("pit", l.optBoolean("pit")).put("cut", l.optBoolean("cut")))
                }
                r.put("laps", slim)
            }
        return JSONArray(out).toString()
    }

    fun races(): List<Race> {
        if (demo) return Demo.races()
        val a = saved("races")?.let { JSONArray(it) } ?: return emptyList()
        return (0 until a.length()).map { i ->
            val r = a.getJSONObject(i)
            val laps = r.optJSONArray("laps") ?: JSONArray()
            val res = r.optJSONArray("results") ?: JSONArray()
            Race(
                id = r.optString("id"), whenMs = r.optLong("when"), track = fixTxt(r.optString("track")), car = fixTxt(r.optString("car")),
                official = r.optBoolean("official"), start = r.optInt("start"), finish = r.optInt("finish"), field = r.optInt("field"),
                inc = maxOf(r.optInt("inc"), (0 until laps.length()).sumOf { laps.getJSONObject(it).optInt("i") }), best = r.optDouble("best").pos(), fieldBest = r.optDouble("fieldBest").pos(),
                avg = r.optDouble("avg").pos(), consistency = r.optDouble("consistency").pos(), pits = r.optInt("pits"),
                fuelUsed = r.optDouble("fuelUsed").pos(), ir = r.optInt("ir"), irChange = r.optInt("irChange"), sof = r.optInt("sof"),
                dnf = r.optBoolean("dnf"),
                laps = (0 until laps.length()).map { k -> laps.getJSONObject(k).let { RaceLap(it.optInt("n"), it.optDouble("t"), it.optInt("p"), it.optInt("i"), it.optBoolean("pit"), it.optBoolean("cut")) } },
                results = (0 until res.length()).map { k ->
                    res.getJSONObject(k).let { RaceResult(it.optInt("cpos").takeIf { p -> p > 0 } ?: it.optInt("pos"), fixTxt(it.optString("name")), it.optInt("ir"), it.optDouble("best").pos(), it.optInt("inc"), it.optInt("laps")) }
                }.sortedBy { it.pos }
            )
        }
    }

    private fun Double.pos() = takeIf { !it.isNaN() && it > 0 }

    /** Text saved with the wrong encoding by an older PC ("AutÃ³dromo"): only the damaged pieces are repaired. */
    private val mojibake = Regex("[\u00C2-\u00DF][\u0080-\u00BF]|[\u00E0-\u00EF][\u0080-\u00BF]{2}")
    private fun fixTxt(s: String): String = mojibake.replace(s) { m ->
        val b = ByteArray(m.value.length) { m.value[it].code.toByte() }
        val d = String(b, Charsets.UTF_8)
        if (d.contains('\uFFFD')) m.value else d
    }

    fun logout() {
        runCatching { call("POST", "/account/logout") }
        clearAll()
    }

    fun devices(): List<Device> {
        val a = JSONObject(call("GET", "/account/sessions")).optJSONArray("sessions") ?: JSONArray()
        return (0 until a.length()).map { i -> a.getJSONObject(i).let { Device(it.optString("id"), it.optString("device"), it.optLong("lastSeen"), it.optBoolean("current")) } }
    }

    fun revoke(id: String) {
        call("POST", "/account/sessions/revoke", JSONObject().put("id", id))
    }

    // ---------- your laps (uploaded by PitlaneHQ.exe) ----------
    fun sessions(): Got<List<CloudSession>> {
        if (demo) return Got(Demo.sessions())
        val g = cachedGet("/api/sessions?limit=100")
        val a = JSONArray(g.data)
        return Got((0 until a.length()).map { i ->
            val s = a.getJSONObject(i)
            CloudSession(
                id = s.getString("id"), started = s.optLong("started"), track = s.optString("track"),
                trackConfig = s.optString("track_config").takeUnless { it == "null" } ?: "", car = s.optString("car"),
                kind = s.optString("kind").takeUnless { it == "null" } ?: "", laps = s.optInt("laps"), best = s.optDouble("best").pos()
            )
        }, g.stale)
    }

    // session and lap ids look like acct_<id>:<…>; the ':' stays as it is in the path
    private fun idPath(id: String) = URLEncoder.encode(id, "UTF-8").replace("+", "%20").replace("%3A", ":")

    fun laps(sessionId: String): Got<List<CloudLap>> {
        if (demo) return Got(Demo.laps(sessionId))
        val g = cachedGet("/api/sessions/" + idPath(sessionId))
        val a = JSONObject(g.data).optJSONArray("laps") ?: JSONArray()
        return Got((0 until a.length()).map { i ->
            val l = a.getJSONObject(i)
            CloudLap(l.optString("id"), l.optInt("n"), l.optDouble("time"), l.optInt("valid", 1) == 1, doubles(l.optJSONArray("sectors")), l.optInt("inc", 0))
        }, g.stale)
    }

    private fun doubles(a: JSONArray?) = a?.let { s -> (0 until s.length()).map { s.optDouble(it) } } ?: emptyList()

    private fun parseTrace(j: JSONObject?): Trace? {
        val d = j?.optJSONArray("d") ?: return null
        if (d.length() == 0) return null
        val x = j.optJSONArray("x")?.let { doubles(it) }
        val y = j.optJSONArray("y")?.let { doubles(it) }
        val xy = x != null && y != null && x.size == d.length() && y.size == d.length()
        return Trace(j.optDouble("bin", 10.0), (0 until d.length()).map { i -> d.getJSONArray(i).let { r -> DoubleArray(6) { k -> r.optDouble(k, 0.0) } } }, if (xy) x else null, if (xy) y else null, j.optJSONArray("inc")?.let { doubles(it) } ?: emptyList(),
            j.optJSONArray("incK")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList())
    }

    fun lapTrace(lapId: String): Trace? {
        if (demo) return Demo.trace(lapId)
        return parseTrace(JSONObject(cachedGet("/api/laps/" + idPath(lapId)).data).optJSONObject("trace"))
    }

    fun bests(): Got<List<PersonalBest>> {
        if (demo) return Got(Demo.bests())
        val g = cachedGet("/api/bests")
        val a = JSONArray(g.data)
        return Got((0 until a.length()).mapNotNull { i ->
            val b = a.getJSONObject(i)
            if (b.optString("game", "iracing") != "iracing") return@mapNotNull null
            PersonalBest(
                b.optString("track"), b.optString("track_config").takeUnless { it == "null" } ?: "", b.optString("car"),
                b.optDouble("best"), b.optInt("laps"), b.optLong("last"),
                b.optString("bestLapId").takeUnless { it.isEmpty() || it == "null" }, b.optString("bestSessionId").takeUnless { it.isEmpty() || it == "null" }
            )
        }.sortedByDescending { it.last }, g.stale)
    }

    // ---------- community ----------
    fun combos(): Got<List<Combo>> {
        if (demo) return Got(Demo.combos())
        val g = cachedGet("/community/combos?game=iracing")
        val a = JSONObject(g.data).optJSONArray("combos") ?: JSONArray()
        return Got((0 until a.length()).map { i ->
            val c = a.getJSONObject(i)
            Combo(c.optLong("trackId"), c.optString("track"), c.optLong("carId"), c.optString("car"), c.optInt("laps"), c.optDouble("best").pos())
        }, g.stale)
    }

    /** One line per driver: their best lap. */
    fun leaderboard(trackId: Long, carId: Long): Got<List<CommunityLap>> {
        if (demo) return Got(Demo.board(trackId, carId))
        val g = cachedGet("/community/laps?game=iracing&trackId=$trackId&carId=$carId")
        val a = JSONObject(g.data).optJSONArray("laps") ?: JSONArray()
        val seen = HashSet<String>()
        val out = ArrayList<CommunityLap>()
        for (i in 0 until a.length()) {
            val l = a.getJSONObject(i)
            val lap = CommunityLap(l.optString("id"), l.optString("alias", "Driver"), l.optDouble("time"), l.optLong("created"), l.optBoolean("hasTrace"), doubles(l.optJSONArray("sectors")))
            if (lap.alias != "Anonymous" && !seen.add(lap.alias)) continue
            out.add(lap)
        }
        return Got(out.sortedBy { it.time }, g.stale)
    }

    fun communityTrace(id: String): Trace? {
        if (demo) return Demo.trace(id)
        return parseTrace(JSONObject(cachedGet("/community/laps/" + idPath(id)).data).optJSONObject("trace"))
    }

    fun reports(): Got<List<SharedReport>> {
        if (demo) return Got(Demo.reports())
        val g = cachedGet("/community/reports?game=iracing")
        val a = JSONObject(g.data).optJSONArray("reports") ?: JSONArray()
        return Got((0 until a.length()).map { i ->
            a.getJSONObject(i).let { SharedReport(it.optString("id"), it.optString("alias"), it.optString("track"), it.optString("car"), it.optLong("created"), it.optInt("finish"), it.optInt("field"), it.optDouble("best").pos()) }
        }, g.stale)
    }

    fun setups(): Got<List<SharedSetup>> {
        if (demo) return Got(Demo.setups())
        val g = cachedGet("/community/setups?game=iracing")
        val a = JSONObject(g.data).optJSONArray("setups") ?: JSONArray()
        return Got((0 until a.length()).map { i ->
            a.getJSONObject(i).let {
                SharedSetup(it.optString("id"), it.optString("alias"), it.optString("name"), it.optString("car"), it.optString("track").takeUnless { t -> t == "null" } ?: "",
                    it.optString("notes").takeUnless { t -> t == "null" } ?: "", it.optInt("downloads"), it.optLong("created"))
            }
        }, g.stale)
    }
}
