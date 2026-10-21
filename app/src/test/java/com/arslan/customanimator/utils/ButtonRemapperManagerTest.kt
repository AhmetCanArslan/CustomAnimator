package com.arslan.customanimator.utils

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.arslan.customanimator.utils.ButtonRemapperManager.PowerLongPress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ButtonRemapperManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun powerLongPressIsSystemDefaultUntilItIsSet() {
        assertNull(ButtonRemapperManager.getPowerLongPress(context.contentResolver))
    }

    @Test
    fun everyPowerLongPressOptionRoundTripsThroughTheGlobalSetting() {
        val resolver = context.contentResolver
        PowerLongPress.entries.forEach { behavior ->
            assertTrue(ButtonRemapperManager.setPowerLongPress(context, resolver, behavior))
            assertEquals(behavior.value, Settings.Global.getInt(resolver, "power_button_long_press", -1))
            assertEquals(behavior, ButtonRemapperManager.getPowerLongPress(resolver))
        }
    }

    @Test
    fun choosingSystemDefaultClearsTheOverride() {
        val resolver = context.contentResolver
        ButtonRemapperManager.setPowerLongPress(context, resolver, PowerLongPress.NOTHING)

        assertTrue(ButtonRemapperManager.setPowerLongPress(context, resolver, null))

        assertNull(Settings.Global.getString(resolver, "power_button_long_press"))
        assertNull(ButtonRemapperManager.getPowerLongPress(resolver))
    }

    @Test
    fun unknownOrCorruptValuesReadAsSystemDefault() {
        val resolver = context.contentResolver
        Settings.Global.putString(resolver, "power_button_long_press", "42")
        assertNull(ButtonRemapperManager.getPowerLongPress(resolver))
        Settings.Global.putString(resolver, "power_button_long_press", "menu")
        assertNull(ButtonRemapperManager.getPowerLongPress(resolver))
    }

    @Test
    fun powerLongPressValuesMatchTheFrameworkConstants() {
        assertEquals(PowerLongPress.NOTHING, PowerLongPress.fromValue(0))
        assertEquals(PowerLongPress.POWER_MENU, PowerLongPress.fromValue(1))
        assertEquals(PowerLongPress.SHUT_DOWN_CONFIRM, PowerLongPress.fromValue(2))
        assertEquals(PowerLongPress.SHUT_DOWN, PowerLongPress.fromValue(3))
        assertEquals(PowerLongPress.VOICE_ASSIST, PowerLongPress.fromValue(4))
        assertEquals(PowerLongPress.ASSISTANT, PowerLongPress.fromValue(5))
        assertNull(PowerLongPress.fromValue(6))
        assertEquals(PowerLongPress.entries.size, PowerLongPress.entries.map { it.value }.toSet().size)
    }

    @Test
    fun featureIsSupportedOnCurrentAndroid() {
        assertTrue(ButtonRemapperManager.isSupported())
    }

    @Test
    @Config(sdk = [28])
    fun featureIsUnsupportedBeforeAndroid10() {
        assertFalse(ButtonRemapperManager.isSupported())
    }

    @Test
    fun customLongPressFailsSafelyWithoutShizuku() {
        val resolver = context.contentResolver

        assertFalse(ButtonRemapperManager.setCustomLongPress(context, resolver, flashlight))

        assertNull(ButtonRemapperManager.getCustomLongPress(context, resolver))
        assertNull(ButtonRemapperManager.getAssistAction(context))
        assertFalse(ButtonRemapperManager.performCustomLongPress(context))
        assertNull(ButtonRemapperManager.getPowerLongPress(resolver))
        assertTrue(ButtonRemapperManager.setPowerLongPress(context, resolver, PowerLongPress.POWER_MENU))
    }

    @Test
    fun customLongPressIsReportedOnlyWhileThisAppIsTheAssistant() {
        val resolver = context.contentResolver
        storeAssistAction()
        Settings.Global.putInt(resolver, "power_button_long_press", PowerLongPress.ASSISTANT.value)
        Settings.Secure.putString(resolver, "assistant", ownAssistant())

        assertEquals(flashlight, ButtonRemapperManager.getCustomLongPress(context, resolver))

        Settings.Secure.putString(resolver, "assistant", "com.example.assistant/.Main")
        assertNull(ButtonRemapperManager.getCustomLongPress(context, resolver))

        Settings.Secure.putString(resolver, "assistant", ownAssistant())
        Settings.Global.putInt(resolver, "power_button_long_press", PowerLongPress.POWER_MENU.value)
        assertNull(ButtonRemapperManager.getCustomLongPress(context, resolver))
    }

    @Test
    fun unknownStoredActionTypeReadsAsNoAction() {
        remapperPrefs().edit()
            .putString("assist_action_type", "TELEPORT")
            .putString("assist_action_value", "somewhere")
            .commit()

        assertNull(ButtonRemapperManager.getAssistAction(context))
    }

    @Test
    fun previousRecognizerIsPutBackWhileThisAppIsTheAssistant() {
        val resolver = context.contentResolver
        assertFalse(ButtonRemapperManager.keepPreviousRecognizer(context))

        remapperPrefs().edit().putString("previous_recognizer", "com.example.speech/.Recognizer").commit()
        Settings.Secure.putString(resolver, "voice_recognition_service", ownRecognizer())

        assertTrue(ButtonRemapperManager.keepPreviousRecognizer(context))
        assertEquals("com.example.speech/.Recognizer", Settings.Secure.getString(resolver, "voice_recognition_service"))
    }

    @Test
    fun leavingCustomLongPressForgetsItOnceAnotherAssistantWasChosen() {
        val resolver = context.contentResolver
        storeAssistAction()
        remapperPrefs().edit()
            .putString("previous_assistant", "com.example.old")
            .putString("previous_recognizer", "com.example.speech/.Recognizer")
            .commit()
        Settings.Secure.putString(resolver, "assistant", "com.example.assistant/.Main")

        assertTrue(ButtonRemapperManager.setPowerLongPress(context, resolver, PowerLongPress.POWER_MENU))

        assertNull(ButtonRemapperManager.getAssistAction(context))
        assertFalse(remapperPrefs().contains("previous_assistant"))
        assertFalse(remapperPrefs().contains("previous_recognizer"))
        assertEquals(PowerLongPress.POWER_MENU, ButtonRemapperManager.getPowerLongPress(resolver))
    }

    @Test
    fun leavingCustomLongPressFailsWhileTheAssistantCannotBeHandedBack() {
        val resolver = context.contentResolver
        storeAssistAction()
        remapperPrefs().edit().putString("previous_assistant", "com.example.old").commit()
        Settings.Global.putInt(resolver, "power_button_long_press", PowerLongPress.ASSISTANT.value)
        Settings.Secure.putString(resolver, "assistant", ownAssistant())

        assertFalse(ButtonRemapperManager.setPowerLongPress(context, resolver, PowerLongPress.POWER_MENU))

        assertEquals(flashlight, ButtonRemapperManager.getCustomLongPress(context, resolver))
        assertEquals("com.example.old", remapperPrefs().getString("previous_assistant", null))
    }

    @Test
    fun doublePressActionRoundTripsAndIsIndependentOfLongPress() {
        assertNull(ButtonRemapperManager.getDoublePressAction(context))

        ButtonRemapperManager.setDoublePressAction(context, flashlight)
        assertEquals(flashlight, ButtonRemapperManager.getDoublePressAction(context))
        assertNull(ButtonRemapperManager.getAssistAction(context))

        ButtonRemapperManager.setDoublePressAction(context, null)
        assertNull(ButtonRemapperManager.getDoublePressAction(context))
        assertFalse(remapperPrefs().contains("double_press_action_type"))
    }

    @Test
    fun doublePressWindowDefaultsTo250AndStaysInRange() {
        assertEquals(250, ButtonRemapperManager.getDoublePressWindowMs(context))

        ButtonRemapperManager.setDoublePressWindowMs(context, 175)
        assertEquals(175, ButtonRemapperManager.getDoublePressWindowMs(context))

        ButtonRemapperManager.setDoublePressWindowMs(context, 10)
        assertEquals(ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MIN_MS, ButtonRemapperManager.getDoublePressWindowMs(context))

        remapperPrefs().edit().putInt("double_press_window_ms", 5000).commit()
        assertEquals(ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MAX_MS, ButtonRemapperManager.getDoublePressWindowMs(context))
    }

    private val flashlight = ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.FLASHLIGHT.key)

    private fun storeAssistAction() {
        remapperPrefs().edit()
            .putString("assist_action_type", flashlight.type.name)
            .putString("assist_action_value", flashlight.value)
            .commit()
    }

    private fun remapperPrefs() = context.getSharedPreferences("button_remapper_prefs", Context.MODE_PRIVATE)

    private fun ownAssistant() = context.packageName + "/.service.PowerAssistService"

    private fun ownRecognizer() = context.packageName + "/.service.PowerAssistRecognitionService"
}
