package com.arslan.customanimator

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutIconPack
import com.arslan.customanimator.ui.components.TileIconCell
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ShortcutIconPacks
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.TerminalTileIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BUILT_IN_SOURCE = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcutIconPickerSheet(
    targetPackage: String?,
    current: ShortcutIconImage,
    onPick: (ShortcutIconImage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var source by remember { mutableStateOf(BUILT_IN_SOURCE) }
    var query by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(ShortcutMaker.glyphColors.first()) }
    var builtInKey by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf(current) }

    val packs by produceState(initialValue = emptyList<ShortcutIconPack>()) {
        value = withContext(Dispatchers.IO) { ShortcutIconPacks.listPacks(context) }
    }
    val packIcons by produceState<List<String>?>(initialValue = null, source) {
        value = null
        if (source != BUILT_IN_SOURCE) {
            value = withContext(Dispatchers.IO) { ShortcutIconPacks.listIcons(context, source, targetPackage) }
        }
    }

    LaunchedEffect(builtInKey, color) {
        val key = builtInKey ?: return@LaunchedEffect
        preview = withContext(Dispatchers.IO) {
            ShortcutIconImage(ShortcutMaker.glyphIcon(context, TerminalTileIcons.resFor(key), color))
        }
    }
    val pickFromPack: (String) -> Unit = { name ->
        coroutineScope.launch {
            withContext(Dispatchers.IO) { ShortcutIconPacks.loadIcon(context, source, name) }?.let {
                builtInKey = null
                preview = it
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = AppShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.terminal_tile_icon_picker_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            IconPreviewRow(preview = preview, onUse = { onPick(preview) })
            Spacer(modifier = Modifier.height(12.dp))
            IconSourceChips(
                packs = packs,
                selected = source,
                onSelect = {
                    source = it
                    query = ""
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PickerSearchField(query, { query = it }, stringResource(R.string.terminal_tile_icon_search))
            if (source == BUILT_IN_SOURCE) {
                IconColorRow(selected = color, onSelect = { color = it })
                Spacer(modifier = Modifier.height(12.dp))
                BuiltInIconGrid(query = query, selected = builtInKey, onPick = { builtInKey = it })
            } else {
                PackIconGrid(packageName = source, names = packIcons, query = query, onPick = pickFromPack)
            }
        }
    }
}

@Composable
private fun IconPreviewRow(preview: ShortcutIconImage, onUse: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShortcutIcon(bitmap = preview.bitmap, legacy = preview.legacy, modifier = Modifier.size(64.dp))
        Button(onClick = onUse) {
            Text(stringResource(R.string.shortcut_icon_use))
        }
    }
}

@Composable
private fun IconSourceChips(packs: List<ShortcutIconPack>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == BUILT_IN_SOURCE,
                onClick = { onSelect(BUILT_IN_SOURCE) },
                label = { Text(stringResource(R.string.shortcut_icon_builtin)) }
            )
        }
        items(packs, key = { it.packageName }) { pack ->
            FilterChip(
                selected = selected == pack.packageName,
                onClick = { onSelect(pack.packageName) },
                label = { Text(pack.label) }
            )
        }
    }
    if (packs.isEmpty()) {
        Text(
            text = stringResource(R.string.shortcut_icon_packs_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconColorRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShortcutMaker.glyphColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (color == selected) 3.dp else 0.dp,
                        color = if (color == selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}

@Composable
private fun IconGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        content = content
    )
}

@Composable
private fun BuiltInIconGrid(query: String, selected: String?, onPick: (String) -> Unit) {
    val keys = remember(query) {
        val normalised = query.trim().lowercase().replace(' ', '_')
        TerminalTileIcons.keys.filter { it.contains(normalised) }
    }
    if (keys.isEmpty()) {
        PickerMessage(stringResource(R.string.terminal_tile_icon_none))
        return
    }
    IconGrid {
        items(keys, key = { it }) { key ->
            TileIconCell(iconKey = key, selected = key == selected, onClick = { onPick(key) })
        }
    }
}

@Composable
private fun PackIconGrid(packageName: String, names: List<String>?, query: String, onPick: (String) -> Unit) {
    if (names == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val filtered = remember(names, query) {
        val normalised = query.trim().lowercase().replace(' ', '_')
        names.filter { it.lowercase().contains(normalised) }
    }
    if (filtered.isEmpty()) {
        PickerMessage(stringResource(R.string.terminal_tile_icon_none))
        return
    }
    IconGrid {
        items(filtered, key = { it }) { name ->
            PackIconCell(packageName = packageName, name = name, onClick = { onPick(name) })
        }
    }
}

@Composable
private fun PackIconCell(packageName: String, name: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, packageName, name) {
        value = withContext(Dispatchers.IO) { ShortcutIconPacks.loadThumbnail(context, packageName, name) }
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = name.replace('_', ' '),
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
