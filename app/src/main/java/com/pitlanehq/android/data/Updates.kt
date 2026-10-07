package com.pitlanehq.android.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A newer TrackIQ for this phone: its version ("0.3.1") and where to download it. */
data class AppUpdate(val version: String, val url: String)

/** One piece of news of the app (web/dist/app-news.json on the server, edited by hand). */
data class AppNews(val id: String, val date: String, val title: String, val text: String, val url: String?, val view: String?)

/**
 * Looks at the newest release of the phone apps on GitHub (the same one the web's download
 * buttons point to) and says whether it is newer than this app.
 */
object Updates {
    private const val API = "https://api.github.com/repos/kevincsv/PitlaneHQ-Android/releases/latest"
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    private fun parts(v: String) = v.trim().removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    fun newer(latest: String, current: String): Boolean {
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** The news of the app, in the phone's language. */
    fun news(server: String, lang: String): List<AppNews> = runCatching {
        val req = Request.Builder().url("$server/app/app-news.json").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return emptyList()
            val items = JSONObject(r.body.string()).optJSONArray("items") ?: return emptyList()
            val pick = { o: org.json.JSONObject?, k: String -> o?.optString(k)?.ifBlank { null } }
            List(minOf(items.length(), 10)) { i ->
                val it = items.getJSONObject(i)
                val title = it.optJSONObject("title")
                val text = it.optJSONObject("text")
                AppNews(it.optString("id"), it.optString("date"), pick(title, lang) ?: pick(title, "en") ?: "", pick(text, lang) ?: pick(text, "en") ?: "", it.optString("url").ifBlank { null }, it.optString("view").ifBlank { null })
            }
        }
    }.getOrDefault(emptyList())

    fun check(current: String): AppUpdate? = runCatching {
        val req = Request.Builder().url(API).header("Accept", "application/vnd.github+json").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val j = JSONObject(r.body.string())
            val tag = j.optString("tag_name")
            if (tag.isEmpty() || !newer(tag, current)) return null
            val assets = j.optJSONArray("assets")
            var url = j.optString("html_url")
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") == "PitlaneHQ-Android.apk") url = a.optString("browser_download_url", url)
            }
            AppUpdate(tag.removePrefix("v").substringBefore("-"), url)
        }
    }.getOrNull()
}
