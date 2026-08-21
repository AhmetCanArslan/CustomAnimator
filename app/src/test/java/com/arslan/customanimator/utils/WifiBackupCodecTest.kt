package com.arslan.customanimator.utils

import com.arslan.customanimator.data.WifiNetwork
import com.arslan.customanimator.data.WifiSecurity
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiBackupCodecTest {

    private val networks = listOf(
        WifiNetwork(ssid = "Home", password = "s3cret", security = WifiSecurity.WPA),
        WifiNetwork(ssid = "Café", password = "", security = WifiSecurity.OPEN, isHidden = true)
    )

    @Test
    fun plainExportContainsReadableJson() {
        val text = String(WifiBackupCodec.encode(networks, WifiBackupCodec.Format.PLAIN, null))
        assertTrue(text.contains("Home"))
        assertTrue(text.contains("s3cret"))
        assertTrue(text.contains("Café"))
    }

    @Test
    fun compressedExportIsGzipAndSmallerForRepetitiveData() {
        val many = List(50) { networks.first().copy(ssid = "Network$it") }
        val plain = WifiBackupCodec.encode(many, WifiBackupCodec.Format.PLAIN, null)
        val compressed = WifiBackupCodec.encode(many, WifiBackupCodec.Format.COMPRESSED, null)
        assertTrue(compressed.size < plain.size)

        val restored = String(GZIPInputStream(compressed.inputStream()).readBytes())
        assertTrue(restored.contains("Network49"))
        assertTrue(restored.startsWith("{"))
    }

    @Test
    fun encryptedExportHidesPasswordsAndIsSaltedPerRun() {
        val first = WifiBackupCodec.encode(networks, WifiBackupCodec.Format.ENCRYPTED, "hunter2")
        val second = WifiBackupCodec.encode(networks, WifiBackupCodec.Format.ENCRYPTED, "hunter2")
        assertFalse(String(first).contains("s3cret"))
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun fileNamesMatchTheirFormat() {
        assertTrue(WifiBackupCodec.fileName(WifiBackupCodec.Format.PLAIN).endsWith(".json"))
        assertTrue(WifiBackupCodec.fileName(WifiBackupCodec.Format.COMPRESSED).endsWith(".json.gz"))
        assertTrue(WifiBackupCodec.fileName(WifiBackupCodec.Format.ENCRYPTED).endsWith(".enc.json"))
    }
}
