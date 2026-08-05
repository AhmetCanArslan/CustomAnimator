package com.arslan.customanimator.notify.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arslan.customanimator.notify.data.AppListManager
import com.arslan.customanimator.notify.data.IgnoreManager
import com.arslan.customanimator.notify.data.IgnoreRule
import com.arslan.customanimator.notify.data.IgnoreType
import com.arslan.customanimator.notify.data.LogEntry
import com.arslan.customanimator.notify.data.LoggingManager
import com.arslan.customanimator.notify.data.LoggingPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoggingViewModel(application: Application) : AndroidViewModel(application) {

    private val loggingManager = LoggingManager.getInstance(application)
    private val ignoreManager = IgnoreManager(application)
    val loggingPreferences = LoggingPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawLogs = MutableStateFlow<List<LogEntry>>(emptyList())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _iconCache = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val iconCache: StateFlow<Map<String, ImageBitmap?>> = _iconCache.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(loggingPreferences.autoDeleteDays)
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.purgeOlderThan(loggingPreferences.autoDeleteDays)
        }

        refreshLogs()

        viewModelScope.launch {
            AppListManager.installedApps
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { loaded ->
                    if (loaded) {
                        rebuildIconCache()
                    }
                }
        }

        viewModelScope.launch {
            combine(_rawLogs, _searchQuery) { raw, query ->
                @Suppress("UNCHECKED_CAST")
                filterLogs(raw, query)
            }.collectLatest { filtered ->
                _logs.value = filtered
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAutoDeleteDays(days: Int) {
        loggingPreferences.autoDeleteDays = days
        _autoDeleteDays.value = days
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.purgeOlderThan(days)
            val result = loggingManager.getLogs()
            withContext(Dispatchers.Main) { _rawLogs.value = result }
        }
    }

    fun ignoreEntry(entry: LogEntry, type: IgnoreType) {
        val rule = when (type) {
            IgnoreType.APP -> IgnoreRule(
                type = IgnoreType.APP,
                packageName = entry.packageName,
                appName = entry.appName
            )
            IgnoreType.TITLE -> IgnoreRule(
                type = IgnoreType.TITLE,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.title
            )
            IgnoreType.BODY -> IgnoreRule(
                type = IgnoreType.BODY,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.body
            )
            IgnoreType.TITLE_AND_BODY -> IgnoreRule(
                type = IgnoreType.TITLE_AND_BODY,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.title,
                matchValue2 = entry.body
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.addRule(rule)
        }
    }

    fun ignoreEntryWithPattern(
        entry: LogEntry,
        type: IgnoreType,
        pattern: String,
        isRegex: Boolean
    ) {
        val rule = IgnoreRule(
            type = type,
            packageName = entry.packageName,
            appName = entry.appName,
            matchValue = pattern,
            isRegex = isRegex
        )
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.addRule(rule)
        }
    }

    fun ignoreFromDialog(
        entry: LogEntry,
        titlePattern: String?,
        titleIsRegex: Boolean,
        bodyPattern: String?,
        bodyIsRegex: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (titlePattern == null && bodyPattern == null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.APP,
                        packageName = entry.packageName,
                        appName = entry.appName
                    )
                )
                return@launch
            }
            if (titlePattern != null && bodyPattern != null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.TITLE_AND_BODY,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = titlePattern,
                        isRegex = titleIsRegex,
                        matchValue2 = bodyPattern,
                        isRegex2 = bodyIsRegex
                    )
                )
                return@launch
            }
            if (titlePattern != null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.TITLE,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = titlePattern,
                        isRegex = titleIsRegex
                    )
                )
            }
            if (bodyPattern != null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.BODY,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = bodyPattern,
                        isRegex = bodyIsRegex
                    )
                )
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.clearLogs()
            withContext(Dispatchers.Main) {
                _rawLogs.value = emptyList()
                _iconCache.value = emptyMap()
            }
        }
    }

    fun deleteLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.deleteLog(id)
            val result = loggingManager.getLogs()
            withContext(Dispatchers.Main) { _rawLogs.value = result }
        }
    }

    private fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = loggingManager.getLogs()
            withContext(Dispatchers.Main) { _rawLogs.value = result }
            val icons = buildIconCache(result)
            withContext(Dispatchers.Main) { _iconCache.value = icons }
        }
    }

    private fun filterLogs(
        raw: List<LogEntry>,
        query: String
    ): List<LogEntry> {
        if (query.isNotBlank()) {
            val q = query.lowercase()
            return raw.filter { entry ->
                entry.appName.lowercase().contains(q) ||
                    entry.title.lowercase().contains(q) ||
                    entry.body.lowercase().contains(q)
            }
        }
        return raw
    }

    private suspend fun rebuildIconCache() {
        val currentLogs = _rawLogs.value
        if (currentLogs.isEmpty()) return
        val icons = withContext(Dispatchers.IO) { buildIconCache(currentLogs) }
        _iconCache.value = icons
    }

    private fun buildIconCache(logs: List<LogEntry>): Map<String, ImageBitmap?> {
        val packages = HashSet<String>(logs.size)
        for (entry in logs) packages.add(entry.packageName)
        return packages.associateWith { AppListManager.getIconForPackage(it) }
    }
}
