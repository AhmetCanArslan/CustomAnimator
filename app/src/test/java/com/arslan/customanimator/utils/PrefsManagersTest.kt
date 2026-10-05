package com.arslan.customanimator.utils

import android.content.Context
import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefsManagersTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun infoNoticeStartsVisibleAndStaysDismissed() {
        assertFalse(InfoNoticeManager.isDismissed(context, "game_mode_info"))
        InfoNoticeManager.dismiss(context, "game_mode_info")
        assertTrue(InfoNoticeManager.isDismissed(context, "game_mode_info"))
        assertFalse(InfoNoticeManager.isDismissed(context, "hwui_restart"))
    }

    @Test
    fun selectedAppsRoundTripAndToggle() {
        val manager = GameModeManager(context)
        manager.setSelectedPackages(setOf("com.example.game", "com.example.other"))
        assertEquals(setOf("com.example.game", "com.example.other"), manager.getSelectedPackages())

        assertTrue(manager.isPackageSelected("com.example.game"))
        val afterRemove = manager.togglePackage("com.example.game")
        assertFalse(afterRemove.contains("com.example.game"))
        val afterAdd = manager.togglePackage("com.example.game")
        assertTrue(afterAdd.contains("com.example.game"))
    }

    @Test
    fun gameModeExclusionsAreStoredApartFromGames() {
        GameModeManager(context).setSelectedPackages(setOf("com.example.game"))
        val exclusions = GameModeExclusionManager(context)
        exclusions.setSelectedPackages(setOf("com.example.messenger"))

        assertEquals(setOf("com.example.messenger"), exclusions.getSelectedPackages())
        assertEquals(setOf("com.example.game"), GameModeManager(context).getSelectedPackages())
        assertFalse(
            GameModeTargets.resolve(context, setOf("com.example.game"), exclusions.getSelectedPackages())
                .contains("com.example.messenger")
        )
    }

    @Test
    fun selectedAppsNeverKeepOwnPackage() {
        val manager = GameModeManager(context)
        manager.setSelectedPackages(setOf(context.packageName, "com.example.app"))
        assertFalse(manager.getSelectedPackages().contains(context.packageName))
        assertTrue(manager.getSelectedPackages().contains("com.example.app"))
    }

    @Test
    fun selectedAppsSurviveCorruptStorage() {
        context.getSharedPreferences("game_mode_apps", Context.MODE_PRIVATE)
            .edit().putString("selected_packages", "not-json").apply()
        assertTrue(GameModeManager(context).getSelectedPackages().isEmpty())
    }

    @Test
    fun threadingConfigsPersistOnlyNonDefaults() {
        val manager = AppThreadingManager(context)
        manager.clearAll()

        manager.setConfig("com.example.game", AppThreadingConfig(ThreadAffinityMode.BIG, ThreadPriority.HIGH))
        manager.setConfig("com.example.plain", AppThreadingConfig())

        val configs = manager.getConfigs()
        assertEquals(1, configs.size)
        assertEquals(ThreadAffinityMode.BIG, configs["com.example.game"]?.affinity)
        assertEquals(ThreadPriority.HIGH, configs["com.example.game"]?.priority)
        assertTrue(manager.getConfig("com.example.plain").isDefault)

        manager.setConfig("com.example.game", AppThreadingConfig())
        assertTrue(manager.getConfigs().isEmpty())
    }

    @Test
    fun threadingEnumsFallBackToDefaults() {
        assertEquals(ThreadAffinityMode.ALL, ThreadAffinityMode.fromValue(null))
        assertEquals(ThreadAffinityMode.ALL, ThreadAffinityMode.fromValue("nonsense"))
        assertEquals(ThreadAffinityMode.LITTLE, ThreadAffinityMode.fromValue("little"))
        assertEquals(ThreadPriority.NORMAL, ThreadPriority.fromValue("nonsense"))
        assertEquals(ThreadPriority.HIGH, ThreadPriority.fromValue("high"))
        assertTrue(ThreadPriority.HIGH.nice < ThreadPriority.NORMAL.nice)
        assertTrue(ThreadPriority.LOW.nice > ThreadPriority.NORMAL.nice)
    }

    @Test
    fun threadingSurvivesCorruptStorage() {
        context.getSharedPreferences("app_threading", Context.MODE_PRIVATE)
            .edit().putString("configs", "{{{").apply()
        assertTrue(AppThreadingManager(context).getConfigs().isEmpty())
    }

    @Test
    fun settingsManagerDefaultsAndRoundTrip() {
        assertEquals(ThemeMode.SYSTEM, SettingsManager.getThemeMode(context))
        SettingsManager.setThemeMode(context, ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, SettingsManager.getThemeMode(context))

        assertEquals("slider", SettingsManager.getInputMode(context))
        SettingsManager.setInputMode(context, "text")
        assertEquals("text", SettingsManager.getInputMode(context))

        assertNull(SettingsManager.getLastScreen(context))
        SettingsManager.setLastScreen(context, "HWUI_TWEAKS")
        SettingsManager.setLastTab(context, "MORE")
        assertEquals("HWUI_TWEAKS", SettingsManager.getLastScreen(context))
        assertEquals("MORE", SettingsManager.getLastTab(context))
    }

    @Test
    fun animationPresetsRoundTrip() {
        val manager = PresetManager(context)
        assertTrue(manager.savePreset("Fast", 0.5f, 0.5f, 0.5f))
        val presets = manager.getAllPresets()
        assertEquals(1, presets.size)
        assertEquals("Fast", presets[0].name)
        assertEquals(0.5f, presets[0].windowAnimationScale, 0.001f)

        assertTrue(manager.deletePreset(presets[0].id))
        assertTrue(manager.getAllPresets().isEmpty())
    }

    @Test
    fun widthPresetsRoundTrip() {
        val manager = WidthPresetManager(context)
        assertTrue(manager.savePreset("Compact", 400))
        val presets = manager.getAllPresets()
        assertEquals(1, presets.size)
        assertEquals(400, presets[0].widthDp)
        assertTrue(manager.deletePreset(presets[0].id))
        assertTrue(manager.getAllPresets().isEmpty())
    }

    @Test
    fun backupExportsAndRestoresPreferences() {
        SettingsManager.setInputMode(context, "text")
        GameModeManager(context).setSelectedPackages(setOf("com.example.game"))

        val json = BackupManager.exportToJson(context)
        assertTrue(json.contains("custom_animator_prefs"))

        SettingsManager.setInputMode(context, "slider")
        assertTrue(BackupManager.importFromJson(context, json))
        assertEquals("text", SettingsManager.getInputMode(context))
    }

    @Test
    fun backupRejectsGarbage() {
        assertFalse(BackupManager.importFromJson(context, "definitely not json"))
    }

    @Test
    fun shortcutsRoundTripUpdateAndDelete() {
        val icon = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val entry = ShortcutEntry(
            id = "photo",
            type = ShortcutType.FILE,
            label = "Holiday",
            uri = "content://media/1",
            mimeType = "image/jpeg"
        )

        ShortcutStore.save(context, entry, icon)
        assertEquals(listOf(entry), ShortcutStore.getAll(context))
        assertEquals(8, ShortcutStore.loadIcon(context, "photo")?.width)

        val reassigned = entry.copy(packageName = "com.example.gallery", activityName = "com.example.gallery.View")
        ShortcutStore.update(context, reassigned)
        assertEquals(reassigned, ShortcutStore.get(context, "photo"))

        ShortcutStore.delete(context, "photo")
        assertTrue(ShortcutStore.getAll(context).isEmpty())
        assertNull(ShortcutStore.loadIcon(context, "photo"))
    }

    @Test
    fun shortcutDefaultHandlerIsRememberedPerFileType() {
        val gallery = ComponentName("com.example.gallery", "com.example.gallery.View")
        assertNull(ShortcutStore.getDefaultHandler(context, "image/jpeg"))

        ShortcutStore.setDefaultHandler(context, "image/jpeg", gallery)
        assertEquals(gallery, ShortcutStore.getDefaultHandler(context, "image/jpeg"))
        assertNull(ShortcutStore.getDefaultHandler(context, "application/pdf"))

        ShortcutStore.setDefaultHandler(context, "image/jpeg", null)
        assertNull(ShortcutStore.getDefaultHandler(context, "image/jpeg"))
    }

    @Test
    fun shortcutLinksGetASchemeOnlyWhenMissing() {
        assertEquals("https://example.com", ShortcutMaker.normalizeLink(" example.com "))
        assertEquals("myapp://open", ShortcutMaker.normalizeLink("myapp://open"))
    }

    @Test
    fun shortcutFoldersPointAtTheDocumentBehindThePickedTree() {
        val tree = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload")
        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3ADownload",
            ShortcutMaker.folderUri(tree).toString()
        )
    }
}
