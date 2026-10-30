package com.arslan.customanimator.utils

import android.util.Base64
import com.arslan.customanimator.data.WifiNetwork
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object WifiBackupCodec {

    enum class Format { PLAIN, COMPRESSED, ENCRYPTED }

    private const val VERSION = 1
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    fun encode(networks: List<WifiNetwork>, format: Format, password: String?): ByteArray {
        val json = toJson(networks).toString().toByteArray(Charsets.UTF_8)
        return when (format) {
            Format.PLAIN -> json
            Format.COMPRESSED -> gzip(json)
            Format.ENCRYPTED -> encrypt(gzip(json), password.orEmpty())
        }
    }

    fun fileName(format: Format): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return when (format) {
            Format.PLAIN -> "wifi-networks-$stamp.json"
            Format.COMPRESSED -> "wifi-networks-$stamp.json.gz"
            Format.ENCRYPTED -> "wifi-networks-$stamp.enc.json"
        }
    }

    private fun toJson(networks: List<WifiNetwork>): JSONObject {
        val array = JSONArray()
        networks.forEach { network ->
            array.put(
                JSONObject().apply {
                    put("ssid", network.ssid)
                    put("password", network.password)
                    put("security", network.security.name)
                    put("hidden", network.isHidden)
                }
            )
        }
        return JSONObject().apply {
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("networks", array)
        }
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val encoded = JSONObject().apply {
            put("encrypted", true)
            put("version", VERSION)
            put("kdf", KDF_ALGORITHM)
            put("iterations", PBKDF2_ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP))
        }
        return encoded.toString().toByteArray(Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
