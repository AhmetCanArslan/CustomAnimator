package com.arslan.customanimator.utils

import android.content.Context
import androidx.annotation.StringRes
import com.arslan.customanimator.R

object CompileFilterManager {

    private const val PREFS_NAME = "compile_booster_prefs"
    private const val KEY_FILTER = "compile_filter"

    enum class CompileFilter(
        val value: String,
        @StringRes val labelRes: Int,
        @StringRes val descriptionRes: Int
    ) {
        EVERYTHING("everything", R.string.compile_filter_everything, R.string.compile_filter_everything_desc),
        SPEED("speed", R.string.compile_filter_speed, R.string.compile_filter_speed_desc),
        SPEED_PROFILE("speed-profile", R.string.compile_filter_speed_profile, R.string.compile_filter_speed_profile_desc),
        VERIFY("verify", R.string.compile_filter_verify, R.string.compile_filter_verify_desc);

        companion object {
            fun fromValue(value: String?): CompileFilter =
                entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }

    val DEFAULT = CompileFilter.SPEED

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFilter(context: Context): CompileFilter =
        CompileFilter.fromValue(prefs(context).getString(KEY_FILTER, DEFAULT.value))

    fun setFilter(context: Context, filter: CompileFilter) {
        prefs(context).edit().putString(KEY_FILTER, filter.value).apply()
    }
}
