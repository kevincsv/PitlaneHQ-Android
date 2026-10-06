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
    val error: String? = null
)

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
        syncUpdated = prefs.getLong("syncUpdated", 0)
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

    fun login(email0: String, password: String): AccountState {
        val email = email0.trim().lowercase()
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) throw AppError("bad_email")
        if (password.isEmpty()) throw AppError("enter_pw")
        val k = Crypto.derive(email, password)
        val j = JSONObject(call("POST", "/account/login", JSONObject().put("email", email).put("auth", k.auth).put("device", "Android"), auth = false))
        val key = Crypto.open(k.wrap, j.getString("wrappedKey"), Crypto.ACCOUNT_AAD)
        require(key.size == 32) { "The account key is damaged" }
        prefs.edit()
            .putString("token", protect(j.getString("token").toByteArray()))
            .putString("dataKey", protect(key))
            .putString("email", email)
            .putString("display", j.optString("display", ""))
            .putBoolean("verified", j.optBoolean("verified", true))
            .putBoolean("admin", j.optBoolean("admin", false))
            .apply()
        return runCatching { sync() }.getOrElse { storedState() }
    }

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
            prefs.edit().putString("display", me.optString("display", "")).putBoolean("verified", me.optBoolean("verified", true)).putBoolean("admin", me.optBoolean("admin", false)).apply()
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
                r.remove("brakes"); r.remove("incidents")
                val laps = r.optJSONArray("laps") ?: JSONArray()
                val slim = JSONArray()
                for (i in 0 until laps.length()) laps.optJSONObject(i)?.let { l ->
                    slim.put(JSONObject().put("n", l.optInt("n")).put("t", l.optDouble("t")).put("p", l.optInt("p")).put("i", l.optInt("i")).put("pit", l.optBoolean("pit")))
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
                id = r.optString("id"), whenMs = r.optLong("when"), track = r.optString("track"), car = r.optString("car"),
                official = r.optBoolean("official"), start = r.optInt("start"), finish = r.optInt("finish"), field = r.optInt("field"),
                inc = r.optInt("inc"), best = r.optDouble("best").pos(), fieldBest = r.optDouble("fieldBest").pos(),
                avg = r.optDouble("avg").pos(), consistency = r.optDouble("consistency").pos(), pits = r.optInt("pits"),
                fuelUsed = r.optDouble("fuelUsed").pos(), ir = r.optInt("ir"), irChange = r.optInt("irChange"), sof = r.optInt("sof"),
                dnf = r.optBoolean("dnf"),
                laps = (0 until laps.length()).map { k -> laps.getJSONObject(k).let { RaceLap(it.optInt("n"), it.optDouble("t"), it.optInt("p"), it.optInt("i"), it.optBoolean("pit")) } },
                results = (0 until res.length()).map { k ->
                    res.getJSONObject(k).let { RaceResult(it.optInt("cpos").takeIf { p -> p > 0 } ?: it.optInt("pos"), it.optString("name"), it.optInt("ir"), it.optDouble("best").pos(), it.optInt("inc"), it.optInt("laps")) }
                }.sortedBy { it.pos }
            )
        }
    }

    private fun Double.pos() = takeIf { !it.isNaN() && it > 0 }

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
            CloudLap(l.optString("id"), l.optInt("n"), l.optDouble("time"), l.optInt("valid", 1) == 1, doubles(l.optJSONArray("sectors")))
        }, g.stale)
    }

    private fun doubles(a: JSONArray?) = a?.let { s -> (0 until s.length()).map { s.optDouble(it) } } ?: emptyList()

    private fun parseTrace(j: JSONObject?): Trace? {
        val d = j?.optJSONArray("d") ?: return null
        if (d.length() == 0) return null
        return Trace(j.optDouble("bin", 10.0), (0 until d.length()).map { i -> d.getJSONArray(i).let { r -> DoubleArray(6) { k -> r.optDouble(k, 0.0) } } })
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
