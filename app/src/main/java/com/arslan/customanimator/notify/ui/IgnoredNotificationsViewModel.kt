package com.arslan.customanimator.notify.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arslan.customanimator.notify.data.AppListManager
import com.arslan.customanimator.notify.data.IgnoreManager
import com.arslan.customanimator.notify.data.IgnoreRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IgnoredNotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val ignoreManager = IgnoreManager(application)

    private val _rules = MutableStateFlow<List<IgnoreRule>>(emptyList())
    val rules: StateFlow<List<IgnoreRule>> = _rules.asStateFlow()

    private val _iconCache = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val iconCache: StateFlow<Map<String, ImageBitmap?>> = _iconCache.asStateFlow()

    init {
        refresh()
    }

    fun removeRule(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.removeRule(id)
            val updated = ignoreManager.getRules()
            withContext(Dispatchers.Main) {
                _rules.value = updated
                rebuildIconCache(updated)
            }
        }
    }

    fun updateRule(rule: IgnoreRule) {
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.updateRule(rule)
            val updated = ignoreManager.getRules()
            withContext(Dispatchers.Main) {
                _rules.value = updated
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ignoreManager.getRules()
            withContext(Dispatchers.Main) { _rules.value = result }
            rebuildIconCache(result)
        }
    }

    private suspend fun rebuildIconCache(rules: List<IgnoreRule>) {
        if (rules.isEmpty()) return
        val icons = withContext(Dispatchers.IO) {
            rules.map { it.packageName }.toHashSet()
                .associateWith { AppListManager.getIconForPackage(it) }
        }
        withContext(Dispatchers.Main) { _iconCache.value = icons }
    }
}
