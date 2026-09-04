package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.provider.Settings

object StatusBarIconsManager {

    private const val ICON_BLACKLIST_KEY = "icon_blacklist"

    const val SLOT_CLOCK = "clock"
    const val SLOT_BATTERY = "battery"
    const val SLOT_ALARM = "alarm_clock"
    const val SLOT_WIFI = "wifi"
    const val SLOT_INTERNET = "internet"
    const val SLOT_MOBILE = "mobile"
    const val SLOT_ETHERNET = "ethernet"
    const val SLOT_AIRPLANE = "airplane"
    const val SLOT_HOTSPOT = "hotspot"
    const val SLOT_BLUETOOTH = "bluetooth"
    const val SLOT_NFC = "nfc"
    const val SLOT_CAST = "cast"
    const val SLOT_VPN = "vpn"
    const val SLOT_LOCATION = "location"
    const val SLOT_ROTATE = "rotate"
    const val SLOT_HEADSET = "headset"
    const val SLOT_ZEN = "zen"
    const val SLOT_VOLUME = "volume"
    const val SLOT_MUTE = "mute"
    const val SLOT_SPEAKERPHONE = "speakerphone"
    const val SLOT_TTY = "tty"
    const val SLOT_DATA_SAVER = "data_saver"
    const val SLOT_MANAGED_PROFILE = "managed_profile"
    const val SLOT_SCREEN_RECORD = "screen_record"
    const val SLOT_SENSORS_OFF = "sensors_off"
    const val SLOT_IMS = "ims"
    const val SLOT_VOLTE = "volte"
    const val SLOT_VONR = "vonr"
    const val SLOT_VOWIFI = "vowifi"
    const val SLOT_WIFI_CALLING = "wifi_calling"
    const val SLOT_CALL_STRENGTH = "call_strength"
    const val SLOT_NO_CALLING = "no_calling"
    const val SLOT_ROAMING = "roaming"
    const val SLOT_WIFI_STANDARD = "wifi_standard"
    const val SLOT_CONNECTED_DISPLAY = "connected_display"
    const val SLOT_SECURE = "secure"
    const val SLOT_DATA_CONNECTION = "data_connection"
    const val SLOT_CDMA_ERI = "cdma_eri"
    const val SLOT_IME = "ime"
    const val SLOT_SYNC_ACTIVE = "sync_active"
    const val SLOT_SYNC_FAILING = "sync_failing"
    const val SLOT_BLUETOOTH_BATTERY = "bluetooth_handsfree_battery"

    val allSlots = listOf(
        SLOT_CLOCK, SLOT_BATTERY, SLOT_ALARM,
        SLOT_WIFI, SLOT_INTERNET, SLOT_MOBILE, SLOT_ETHERNET, SLOT_AIRPLANE,
        SLOT_HOTSPOT, SLOT_BLUETOOTH, SLOT_NFC, SLOT_CAST, SLOT_VPN,
        SLOT_LOCATION, SLOT_ROTATE, SLOT_HEADSET,
        SLOT_ZEN, SLOT_VOLUME, SLOT_MUTE, SLOT_SPEAKERPHONE, SLOT_TTY,
        SLOT_DATA_SAVER, SLOT_MANAGED_PROFILE, SLOT_SCREEN_RECORD, SLOT_SENSORS_OFF,
        SLOT_IMS, SLOT_VOLTE, SLOT_VONR, SLOT_VOWIFI, SLOT_WIFI_CALLING,
        SLOT_CALL_STRENGTH, SLOT_NO_CALLING, SLOT_ROAMING, SLOT_WIFI_STANDARD,
        SLOT_DATA_CONNECTION, SLOT_CDMA_ERI, SLOT_IME, SLOT_SYNC_ACTIVE, SLOT_SYNC_FAILING,
        SLOT_BLUETOOTH_BATTERY, SLOT_CONNECTED_DISPLAY, SLOT_SECURE
    )

    fun getHiddenSlots(contentResolver: ContentResolver): Set<String> {
        val raw = try {
            Settings.Secure.getString(contentResolver, ICON_BLACKLIST_KEY)
        } catch (e: Exception) {
            null
        }
        return raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun isSlotHidden(contentResolver: ContentResolver, slot: String): Boolean =
        slot in getHiddenSlots(contentResolver)

    fun setSlotHidden(
        contentResolver: ContentResolver,
        slot: String,
        hidden: Boolean
    ): Boolean {
        val current = getHiddenSlots(contentResolver).toMutableSet()
        if (hidden) current.add(slot) else current.remove(slot)
        return setHiddenSlots(contentResolver, current)
    }

    fun hideAll(contentResolver: ContentResolver): Boolean =
        setHiddenSlots(contentResolver, allSlots.toSet())

    fun showAll(contentResolver: ContentResolver): Boolean =
        setHiddenSlots(contentResolver, emptySet())

    private fun setHiddenSlots(contentResolver: ContentResolver, slots: Set<String>): Boolean {
        val value = allSlots.filter { it in slots }.joinToString(",")
        return putSecureString(contentResolver, ICON_BLACKLIST_KEY, value)
    }

    private fun putSecureString(
        contentResolver: ContentResolver,
        key: String,
        value: String
    ): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = if (value.isEmpty()) {
                ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "secure", key))
            } else {
                ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "secure", key, value))
            }
            if (success) return true
        }
        return try {
            Settings.Secure.putString(contentResolver, key, value.ifEmpty { null })
        } catch (e: Exception) {
            false
        }
    }
}
