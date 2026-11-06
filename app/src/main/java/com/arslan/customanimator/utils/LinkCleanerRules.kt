package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.R

object LinkCleanerRules {

    @Volatile
    private var providers: List<LinkCleaner.Provider>? = null

    fun load(context: Context): List<LinkCleaner.Provider> = providers ?: context.resources
        .openRawResource(R.raw.clearurls_rules)
        .bufferedReader()
        .use { LinkCleaner.parseRules(it.readText()) }
        .also { providers = it }
}
