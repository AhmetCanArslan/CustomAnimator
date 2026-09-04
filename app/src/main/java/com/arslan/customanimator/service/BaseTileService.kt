package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TileNumberIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

sealed class TileIcon {
    data class Res(@DrawableRes val id: Int) : TileIcon()
    data class Number(val text: String) : TileIcon()
}

data class TileSpec(
    val label: String,
    val icon: TileIcon,
    val state: Int,
    val subtitle: String? = null
)

abstract class BaseTileService : TileService() {

    private var refreshJob: Job? = null
    private var rendered: TileSpec? = null

    protected abstract suspend fun loadSpec(): TileSpec

    override fun onStartListening() {
        super.onStartListening()
        cachedSpecs[javaClass.name]?.let { render(it) }
        refresh()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refresh()
    }

    override fun onStopListening() {
        refreshJob?.cancel()
        refreshJob = null
        super.onStopListening()
    }

    protected fun refresh() {
        refreshJob?.cancel()
        refreshJob = tileScope.launch { loadAndRender() }
    }

    protected fun runTileAction(block: suspend () -> Unit) {
        tileScope.launch {
            block()
            loadAndRender()
        }
    }

    protected fun toast(@StringRes message: Int, vararg args: Any) {
        val text = if (args.isEmpty()) getString(message) else getString(message, *args)
        mainHandler.post { Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show() }
    }

    protected fun collapseShade() {
        ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "collapse"))
    }

    protected fun unavailableSpec(@StringRes label: Int, icon: TileIcon): TileSpec =
        TileSpec(getString(label), icon, Tile.STATE_UNAVAILABLE)

    protected fun stateOf(ready: Boolean, active: Boolean): Int = when {
        !ready -> Tile.STATE_UNAVAILABLE
        active -> Tile.STATE_ACTIVE
        else -> Tile.STATE_INACTIVE
    }

    private suspend fun loadAndRender() {
        val spec = loadSpec()
        iconFor(spec.icon)
        withContext(Dispatchers.Main) { render(spec) }
    }

    private fun render(spec: TileSpec) {
        cachedSpecs[javaClass.name] = spec
        if (rendered == spec) return
        val tile = qsTile ?: return
        rendered = spec
        tile.label = spec.label
        tile.icon = iconFor(spec.icon)
        tile.state = spec.state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = spec.subtitle
        }
        runCatching { tile.updateTile() }
    }

    private fun iconFor(icon: TileIcon): Icon = synchronized(iconCache) {
        iconCache.getOrPut(icon) {
            when (icon) {
                is TileIcon.Res -> Icon.createWithResource(applicationContext, icon.id)
                is TileIcon.Number -> TileNumberIcon.create(icon.text)
            }
        }
    }

    private companion object {
        val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val mainHandler = Handler(Looper.getMainLooper())
        val cachedSpecs = ConcurrentHashMap<String, TileSpec>()
        val iconCache = HashMap<TileIcon, Icon>()
    }
}
