package com.arslan.customanimator.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.arslan.customanimator.data.WifiNetwork
import com.arslan.customanimator.data.WifiSecurity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeRenderer {

    fun wifiPayload(network: WifiNetwork): String {
        val type = when (network.security) {
            WifiSecurity.WPA -> "WPA"
            WifiSecurity.WEP -> "WEP"
            WifiSecurity.OPEN -> "nopass"
        }
        val hidden = if (network.isHidden) "H:true;" else ""
        return "WIFI:T:$type;S:${escape(network.ssid)};P:${escape(network.password)};$hidden;"
    }

    private fun escape(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }

    fun render(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(matrix.width * matrix.height)
            for (y in 0 until matrix.height) {
                val offset = y * matrix.width
                for (x in 0 until matrix.width) {
                    pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565).apply {
                setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
            }
        } catch (e: Exception) {
            null
        }
    }
}
