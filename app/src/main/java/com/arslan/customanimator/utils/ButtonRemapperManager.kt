package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutType
import java.util.UUID

object ButtonRemapperManager {

    enum class PowerLongPress(val value: Int) {
        POWER_MENU(1),
        ASSISTANT(5),
        VOICE_ASSIST(4),
        NOTHING(0),
        SHUT_DOWN_CONFIRM(2),
        SHUT_DOWN(3);

        companion object {
            fun fromValue(value: Int): PowerLongPress? = entries.firstOrNull { it.value == value }
        }
    }

    private const val POWER_LONG_PRESS_KEY = "power_button_long_press"
    private const val POWER_LONG_PRESS_CONFIG = "config_longPressOnPowerBehavior"
    private const val ASSISTANT_KEY = "assistant"
    private const val RECOGNIZER_KEY = "voice_recognition_service"
    private const val ASSISTANT_ROLE = "android.app.role.ASSISTANT"
    private const val PREFS_NAME = "button_remapper_prefs"
    private const val ACTION_SUFFIX = "_action"
    private const val LEGACY_TYPE_SUFFIX = "_action_type"
    private const val LEGACY_VALUE_SUFFIX = "_action_value"
    private const val KEY_DOUBLE_PRESS_WINDOW = "double_press_window_ms"
    private const val KEY_PREVIOUS_ASSISTANT = "previous_assistant"
    private const val KEY_PREVIOUS_RECOGNIZER = "previous_recognizer"
    private const val UNSET = -1
    const val DOUBLE_PRESS_WINDOW_MIN_MS = 150
    const val DOUBLE_PRESS_WINDOW_MAX_MS = 400
    const val DOUBLE_PRESS_WINDOW_STEP_MS = 25
    private const val DOUBLE_PRESS_WINDOW_DEFAULT_MS = 250

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun getPowerLongPress(contentResolver: ContentResolver): PowerLongPress? {
        return try {
            PowerLongPress.fromValue(Settings.Global.getInt(contentResolver, POWER_LONG_PRESS_KEY, UNSET))
        } catch (e: Exception) {
            null
        }
    }

    fun getDefaultPowerLongPress(): PowerLongPress? =
        systemInteger(POWER_LONG_PRESS_CONFIG)?.let { PowerLongPress.fromValue(it) }

    fun setPowerLongPress(context: Context, contentResolver: ContentResolver, behavior: PowerLongPress?): Boolean =
        releaseAssistant(context, contentResolver) &&
            writePowerLongPress(context, contentResolver, behavior)

    fun getCustomLongPress(context: Context, contentResolver: ContentResolver): ShortcutEntry? {
        val active = getPowerLongPress(contentResolver) == PowerLongPress.ASSISTANT &&
            isAssistant(context, contentResolver)
        return if (active) getAction(context, ButtonTrigger.POWER_LONG_PRESS) else null
    }

    fun setCustomLongPress(context: Context, contentResolver: ContentResolver, action: ShortcutEntry): Boolean {
        if (!claimAssistant(context, contentResolver)) return false
        if (!writePowerLongPress(context, contentResolver, PowerLongPress.ASSISTANT)) {
            restoreAssistant(context)
            return false
        }
        setAction(context, ButtonTrigger.POWER_LONG_PRESS, action)
        return true
    }

    suspend fun performCustomLongPress(context: Context): Boolean =
        getAction(context, ButtonTrigger.POWER_LONG_PRESS)?.let { ShortcutMaker.launch(context, it) } ?: false

    suspend fun perform(context: Context, trigger: ButtonTrigger): Boolean {
        val action = getAction(context, trigger) ?: return false
        return if (action.startsActivity) {
            ShortcutMaker.startForTrigger(context, trigger)
        } else {
            ShortcutMaker.launch(context, action)
        }
    }

    fun keepPreviousRecognizer(context: Context): Boolean {
        val previous = prefs(context).getString(KEY_PREVIOUS_RECOGNIZER, null)
        if (previous.isNullOrEmpty()) return false
        return try {
            Settings.Secure.putString(context.contentResolver, RECOGNIZER_KEY, previous)
        } catch (e: Exception) {
            false
        }
    }

    fun getAction(context: Context, trigger: ButtonTrigger): ShortcutEntry? {
        val stored = prefs(context).getString(trigger.prefsKey + ACTION_SUFFIX, null)
        return if (stored == null) legacyAction(context, trigger) else ShortcutStore.decode(stored)
    }

    fun setAction(context: Context, trigger: ButtonTrigger, action: ShortcutEntry?) {
        prefs(context).edit()
            .putString(trigger.prefsKey + ACTION_SUFFIX, action?.let(ShortcutStore::encode))
            .remove(trigger.prefsKey + LEGACY_TYPE_SUFFIX)
            .remove(trigger.prefsKey + LEGACY_VALUE_SUFFIX)
            .commit()
    }

    fun activeServiceTriggers(context: Context): List<ButtonTrigger> =
        ButtonTrigger.service.filter { getAction(context, it) != null }

    fun clearServiceActions(context: Context) {
        ButtonTrigger.service.forEach { setAction(context, it, null) }
    }

    fun getDoublePressWindowMs(context: Context): Int =
        prefs(context).getInt(KEY_DOUBLE_PRESS_WINDOW, DOUBLE_PRESS_WINDOW_DEFAULT_MS)
            .coerceIn(DOUBLE_PRESS_WINDOW_MIN_MS, DOUBLE_PRESS_WINDOW_MAX_MS)

    fun setDoublePressWindowMs(context: Context, windowMs: Int) {
        prefs(context).edit()
            .putInt(KEY_DOUBLE_PRESS_WINDOW, windowMs.coerceIn(DOUBLE_PRESS_WINDOW_MIN_MS, DOUBLE_PRESS_WINDOW_MAX_MS))
            .commit()
    }

    private fun legacyAction(context: Context, trigger: ButtonTrigger): ShortcutEntry? {
        val prefs = prefs(context)
        val type = prefs.getString(trigger.prefsKey + LEGACY_TYPE_SUFFIX, null) ?: return null
        val value = prefs.getString(trigger.prefsKey + LEGACY_VALUE_SUFFIX, null) ?: return null
        return when (type) {
            "QUICK" -> ButtonActions.Quick.fromKey(value)?.let { ButtonActions.quickEntry(context, it) }
            "TOGGLE" -> ToggleTilePresets.byKey(value)?.let { ButtonActions.toggleEntry(context, it) }
            "APP" -> legacyEntry(ShortcutType.APP, appLabel(context, value) ?: value).copy(packageName = value)
            "COMMAND" -> legacyEntry(ShortcutType.COMMAND, value).copy(command = value)
            else -> null
        }
    }

    private fun legacyEntry(type: ShortcutType, label: String) =
        ShortcutEntry(id = UUID.randomUUID().toString(), type = type, label = label)

    private fun appLabel(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun claimAssistant(context: Context, contentResolver: ContentResolver): Boolean {
        if (isAssistant(context, contentResolver)) return true
        prefs(context).edit()
            .putString(KEY_PREVIOUS_ASSISTANT, assistantPackage(contentResolver).orEmpty())
            .putString(KEY_PREVIOUS_RECOGNIZER, secureString(contentResolver, RECOGNIZER_KEY).orEmpty())
            .commit()
        return ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "role", "add-role-holder", ASSISTANT_ROLE, context.packageName)
        )
    }

    private fun releaseAssistant(context: Context, contentResolver: ContentResolver): Boolean {
        if (!prefs(context).contains(KEY_PREVIOUS_ASSISTANT)) return true
        if (isAssistant(context, contentResolver)) return restoreAssistant(context)
        forgetAssistant(context)
        return true
    }

    private fun restoreAssistant(context: Context): Boolean {
        val previous = prefs(context).getString(KEY_PREVIOUS_ASSISTANT, null).orEmpty()
        val command = if (previous.isEmpty()) {
            arrayOf("cmd", "role", "remove-role-holder", ASSISTANT_ROLE, context.packageName)
        } else {
            arrayOf("cmd", "role", "add-role-holder", ASSISTANT_ROLE, previous)
        }
        val restored = ShizukuHelper.executeShellCommand(command)
        if (restored) forgetAssistant(context)
        return restored
    }

    private fun forgetAssistant(context: Context) {
        prefs(context).edit()
            .remove(KEY_PREVIOUS_ASSISTANT)
            .remove(KEY_PREVIOUS_RECOGNIZER)
            .apply()
        setAction(context, ButtonTrigger.POWER_LONG_PRESS, null)
    }

    private fun isAssistant(context: Context, contentResolver: ContentResolver): Boolean =
        assistantPackage(contentResolver) == context.packageName

    private fun assistantPackage(contentResolver: ContentResolver): String? =
        secureString(contentResolver, ASSISTANT_KEY)
            ?.let { ComponentName.unflattenFromString(it) }
            ?.packageName

    private fun secureString(contentResolver: ContentResolver, key: String): String? {
        return try {
            Settings.Secure.getString(contentResolver, key)
        } catch (e: Exception) {
            null
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun writePowerLongPress(
        context: Context,
        contentResolver: ContentResolver,
        behavior: PowerLongPress?
    ): Boolean {
        return if (behavior == null) {
            DeveloperOptionsManager.putGlobalString(context, contentResolver, POWER_LONG_PRESS_KEY, null)
        } else {
            DeveloperOptionsManager.putGlobalInt(context, contentResolver, POWER_LONG_PRESS_KEY, behavior.value)
        }
    }

    private fun systemInteger(name: String): Int? {
        return try {
            val resources = Resources.getSystem()
            val id = resources.getIdentifier(name, "integer", "android")
            if (id == 0) null else resources.getInteger(id)
        } catch (e: Exception) {
            null
        }
    }
}
