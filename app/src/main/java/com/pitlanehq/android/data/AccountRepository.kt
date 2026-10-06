package com.pitlanehq.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.pitlanehq.android.model.CloudLap
import com.pitlanehq.android.model.CloudSession
import com.pitlanehq.android.model.Combo
import com.pitlanehq.android.model.CommunityLap
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
    val busy: Boolean = false,
    val syncedFiles: Int = 0,
    val syncVersion: Long = 0,
    val syncUpdated: Long = 0,
    val error: String? = null
)

class SignedOut : Exception("Signed out: sign in again")

/**
 * The Pitlane HQ account, exactly like the PC and the web app: the password only derives the
 * keys on this phone, the server gets the login key and gives back the data key sealed with
 * the wrap key. The session token and the data key are kept sealed with an Android Keystore key.
 */
class AccountRepository(context: Context) {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    private val prefs = context.getSharedPreferences("pitlane-account", Context.MODE_PRIVATE)

    fun storedState() = AccountState(
        signedIn = prefs.getString("token", null) != null,
        email = prefs.getString("email", "") ?: "",
        display = prefs.getString("display", "") ?: "",
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

    private fun protect(v: ByteArray): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, deviceKey())
        return Base64.encodeToString(c.iv + c.doFinal(v), Base64.NO_WRAP)
    }

    private fun unprotect(v: String): ByteArray {
        val b = Base64.decode(v, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, b, 0, 12))
        return c.doFinal(b, 12, b.size - 12)
    }

    fun token(): String = runCatching { String(unprotect(prefs.getString("token", null)!!)) }.getOrElse { throw SignedOut() }
    fun dataKey(): ByteArray = runCatching { unprotect(prefs.getString("dataKey", null)!!) }.getOrElse { throw SignedOut() }

    // ---------- server ----------
    private fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): String {
        val q = Request.Builder().url(SERVER + path).method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        if (auth) q.header("Authorization", "Bearer " + token())
        client.newCall(q.build()).execute().use { r ->
            val txt = r.body.string()
            if (r.code == 401 && auth) {
                prefs.edit().clear().apply()
                throw SignedOut()
            }
            if (!r.isSuccessful) throw IllegalStateException(runCatching { JSONObject(txt).optString("error") }.getOrNull()?.ifBlank { null } ?: "Server error ${r.code}")
            return txt
        }
    }

    fun login(email0: String, password: String): AccountState {
        val email = email0.trim().lowercase()
        require(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) { "Enter a valid email" }
        require(password.isNotEmpty()) { "Enter your password" }
        val k = Crypto.derive(email, password)
        val j = JSONObject(call("POST", "/account/login", JSONObject().put("email", email).put("auth", k.auth).put("device", "Android"), auth = false))
        val key = Crypto.open(k.wrap, j.getString("wrappedKey"), Crypto.ACCOUNT_AAD)
        require(key.size == 32) { "The account key is damaged" }
        prefs.edit()
            .putString("token", protect(j.getString("token").toByteArray()))
            .putString("dataKey", protect(key))
            .putString("email", email)
            .putString("display", j.optString("display", ""))
            .apply()
        return runCatching { sync() }.getOrElse { storedState() }
    }

    /** Pulls the encrypted settings bundle the PC keeps in the account and opens it here. */
    fun sync(): AccountState {
        val j = JSONObject(call("GET", "/account/sync"))
        val blob = j.optString("blob", "")
        var files = 0
        if (blob.isNotEmpty()) {
            val raw = Crypto.gunzip(Crypto.open(dataKey(), blob, Crypto.ACCOUNT_AAD))
            files = JSONObject(String(raw)).length()
        }
        prefs.edit()
            .putInt("syncedFiles", files)
            .putLong("syncVersion", j.optLong("version", 0))
            .putLong("syncUpdated", j.optLong("updated", 0))
            .apply()
        runCatching {
            val me = JSONObject(call("GET", "/account/me"))
            prefs.edit().putString("display", me.optString("display", "")).apply()
        }
        return storedState()
    }

    fun logout() {
        runCatching { call("POST", "/account/logout") }
        prefs.edit().clear().apply()
    }

    // ---------- your laps (uploaded by PitlaneHQ.exe) ----------
    fun sessions(): List<CloudSession> {
        val a = JSONArray(call("GET", "/api/sessions?limit=100"))
        return (0 until a.length()).map { i ->
            val s = a.getJSONObject(i)
            CloudSession(
                id = s.getString("id"),
                started = s.optLong("started"),
                track = s.optString("track"),
                trackConfig = s.optString("track_config").takeUnless { it == "null" } ?: "",
                car = s.optString("car"),
                kind = s.optString("kind").takeUnless { it == "null" } ?: "",
                laps = s.optInt("laps"),
                best = s.optDouble("best").takeUnless { it.isNaN() }
            )
        }
    }

    // session ids look like acct_<id>:<…>; the ':' stays as it is in the path
    fun laps(sessionId: String): List<CloudLap> {
        val j = JSONObject(call("GET", "/api/sessions/" + enc(sessionId).replace("%3A", ":")))
        val a = j.optJSONArray("laps") ?: JSONArray()
        return (0 until a.length()).map { i ->
            val l = a.getJSONObject(i)
            val sec = l.optJSONArray("sectors")
            CloudLap(l.optInt("n"), l.optDouble("time"), l.optInt("valid", 1) == 1, sec?.let { s -> (0 until s.length()).map { s.optDouble(it) } } ?: emptyList())
        }
    }

    // ---------- community ----------
    fun combos(): List<Combo> {
        val a = JSONObject(call("GET", "/community/combos?game=iracing")).optJSONArray("combos") ?: JSONArray()
        return (0 until a.length()).map { i ->
            val c = a.getJSONObject(i)
            Combo(c.optLong("trackId"), c.optString("track"), c.optLong("carId"), c.optString("car"), c.optInt("laps"), c.optDouble("best").takeUnless { it.isNaN() })
        }
    }

    fun leaderboard(c: Combo): List<CommunityLap> {
        val a = JSONObject(call("GET", "/community/laps?game=iracing&trackId=${c.trackId}&carId=${c.carId}")).optJSONArray("laps") ?: JSONArray()
        // one line per driver: their best lap
        val best = LinkedHashMap<String, CommunityLap>()
        for (i in 0 until a.length()) {
            val l = a.getJSONObject(i)
            val lap = CommunityLap(l.optString("alias", "Driver"), l.optDouble("time"), l.optLong("created"))
            if (lap.alias == "Anonymous" || best[lap.alias] == null) best[lap.alias + if (lap.alias == "Anonymous") i else ""] = lap
        }
        return best.values.sortedBy { it.time }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
