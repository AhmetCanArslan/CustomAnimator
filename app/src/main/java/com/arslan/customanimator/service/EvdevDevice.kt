package com.arslan.customanimator.service

import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

class EvdevDevice private constructor(private val descriptor: ParcelFileDescriptor) {

    val fileDescriptor: FileDescriptor
        get() = descriptor.fileDescriptor

    fun grab(grab: Boolean): Boolean = nativeGrab(descriptor.fd, grab)

    fun close() {
        descriptor.close()
    }

    companion object {
        const val EV_KEY = 1
        const val KEY_POWER = 116

        val forwardedKeys = mapOf(
            114 to KeyEvent.KEYCODE_VOLUME_DOWN,
            115 to KeyEvent.KEYCODE_VOLUME_UP
        )

        init {
            System.loadLibrary("evdev")
        }

        fun openPowerKeyDevices(): List<EvdevDevice> =
            File("/dev/input")
                .listFiles { file -> file.name.startsWith("event") }
                .orEmpty()
                .mapNotNull { openIfPowerKey(it) }

        private fun openIfPowerKey(file: File): EvdevDevice? {
            val descriptor = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: IOException) {
                return null
            }
            val keys = nativeKeyCodes(descriptor.fd).toSet()
            if (KEY_POWER in keys && (keys - KEY_POWER).all { it in forwardedKeys }) {
                return EvdevDevice(descriptor)
            }
            descriptor.close()
            return null
        }

        @JvmStatic
        private external fun nativeGrab(fd: Int, grab: Boolean): Boolean

        @JvmStatic
        private external fun nativeKeyCodes(fd: Int): IntArray
    }
}
