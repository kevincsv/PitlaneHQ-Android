package com.pitlanehq.android.data

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The same keys and sealing as the PC (placcount.go, liverelay.go) and the web app:
 * PBKDF2-SHA256 600 000 rounds -> HKDF ("pitlanehq auth" / "pitlanehq wrap") -> AES-256-GCM
 * with a 12-byte nonce in front and an associated-data label.
 */
object Crypto {
    const val ACCOUNT_AAD = "pitlanehq-v1"
    const val LIVE_AAD = "pitlanehq-live-v1"
    private val rnd = SecureRandom()

    class Keys(val auth: String, val wrap: ByteArray)

    fun derive(email: String, password: String): Keys {
        val salt = MessageDigest.getInstance("SHA-256").digest(("pitlanehq-account-v1:" + email.trim().lowercase()).toByteArray())
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        val master = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Keys(hkdf(master, "pitlanehq auth").joinToString("") { "%02x".format(it) }, hkdf(master, "pitlanehq wrap"))
    }

    // RFC 5869 with an empty salt and one 32-byte block
    private fun hkdf(ikm: ByteArray, info: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        return mac.doFinal(info.toByteArray() + byteArrayOf(1))
    }

    fun open(key: ByteArray, sealed: String, aad: String): ByteArray {
        val b = Base64.decode(sealed, Base64.DEFAULT)
        require(b.size > 28) { "damaged data" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, b, 0, 12))
        c.updateAAD(aad.toByteArray())
        return c.doFinal(b, 12, b.size - 12)
    }

    fun seal(key: ByteArray, plain: ByteArray, aad: String): String {
        val nonce = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(aad.toByteArray())
        return Base64.encodeToString(nonce + c.doFinal(plain), Base64.NO_WRAP)
    }

    fun gunzip(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(b)).use { it.copyTo(out) }
        return out.toByteArray()
    }

    fun gzip(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(b) }
        return out.toByteArray()
    }
}
