package com.arslan.customanimator.notify.data

import android.content.Context
import com.google.gson.Gson

object AddEditRuleDraft {

    private const val PREFS_NAME = "add_edit_rule_draft"
    private val gson = Gson()

    data class Draft(
        val ruleId: String,
        val selectedPackageNames: List<String> = emptyList(),
        val titleKeywords: List<String> = emptyList(),
        val currentTitleKeyword: String = "",
        val bodyKeywords: List<String> = emptyList(),
        val currentBodyKeyword: String = "",
        val flashEnabled: Boolean = false,
        val flashPattern: String = FlashPattern.HEARTBEAT.name,
        val flashCustomPatternId: String? = null,
        val wakeUpEnabled: Boolean = false,
        val screenDurationSeconds: Int = 10,
        val pocketModeEnabled: Boolean = true,
        val aodEnabled: Boolean = false,
        val aodDurationSeconds: Int = 10,
        val screenFlashEnabled: Boolean = false,
        val screenFlashColor: String = "RED",
        val screenFlashDurationSeconds: Int = 5,
        val widgetEnabled: Boolean = false,
        val widgetKeepBody: Boolean = true,
        val applyOnVibration: Boolean = true,
        val applyOnSilent: Boolean = true,
        val applyOnDND: Boolean = true,
        val preventMultipleNotifications: Boolean = false,
    )

    private fun prefKey(ruleId: String) = "draft_$ruleId"

    fun save(context: Context, draft: Draft) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(prefKey(draft.ruleId), gson.toJson(draft))
            .apply()
    }

    fun load(context: Context, ruleId: String): Draft? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(prefKey(ruleId), null) ?: return null
        return try {
            gson.fromJson(json, Draft::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context, ruleId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(prefKey(ruleId))
            .apply()
    }
}
