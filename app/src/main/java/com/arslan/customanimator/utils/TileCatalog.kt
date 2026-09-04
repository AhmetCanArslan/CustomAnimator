package com.arslan.customanimator.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arslan.customanimator.R

object TileCatalog {

    enum class Group(@StringRes val titleRes: Int) {
        APP(R.string.qs_tiles_group_app),
        TOGGLE(R.string.qs_tiles_group_toggle),
        ANIMATION(R.string.qs_tiles_group_animation),
        WIDTH(R.string.qs_tiles_group_width),
        TERMINAL(R.string.qs_tiles_group_terminal),
        PROFILE(R.string.qs_tiles_group_profile)
    }

    sealed class Face {
        data class Drawable(@DrawableRes val res: Int) : Face()
        data class Number(val text: String) : Face()
    }

    data class Entry(
        val id: String,
        val group: Group,
        val label: String,
        @StringRes val subtitleRes: Int,
        val face: Face,
        val addToPanel: (Context) -> Unit,
        val remove: (Context) -> Unit
    )

    fun activeTiles(context: Context): List<Entry> =
        builtInTiles(context, enabled = true) + toggleTiles(context) + animationTiles(context) +
            widthTiles(context) + terminalTiles(context) + profileTiles(context)

    fun disabledAppTiles(context: Context): List<Entry> = builtInTiles(context, enabled = false)

    fun canRequestAdd(): Boolean = BuiltInTiles.canRequestAdd()

    private fun builtInTiles(context: Context, enabled: Boolean): List<Entry> =
        BuiltInTiles.all.filter { BuiltInTiles.isEnabled(context, it) == enabled }.map { entry ->
            Entry(
                id = entry.key,
                group = Group.APP,
                label = context.getString(entry.nameRes),
                subtitleRes = entry.descriptionRes,
                face = Face.Drawable(entry.iconRes),
                addToPanel = { ctx ->
                    BuiltInTiles.setEnabled(ctx, entry, true)
                    BuiltInTiles.requestAddTile(ctx, entry)
                },
                remove = { ctx -> BuiltInTiles.setEnabled(ctx, entry, false) }
            )
        }

    private fun toggleTiles(context: Context): List<Entry> =
        ToggleTileManager(context).getTiles().map { tile ->
            Entry(
                id = tile.id,
                group = Group.TOGGLE,
                label = tile.label,
                subtitleRes = Group.TOGGLE.titleRes,
                face = Face.Drawable(TerminalTileIcons.resFor(tile.iconKey)),
                addToPanel = { ctx ->
                    ToggleTileSlots.requestAddTile(ctx, tile.slot, tile.label, tile.iconKey)
                },
                remove = { ctx -> ToggleTileManager(ctx).removeTile(tile.id) }
            )
        }

    private fun animationTiles(context: Context): List<Entry> =
        PresetManager(context).getAllPresets().mapNotNull { preset ->
            val tile = preset.tile ?: return@mapNotNull null
            val numberText = TileNumberIcon.animationText(
                preset.windowAnimationScale,
                preset.transitionAnimationScale,
                preset.animatorDurationScale
            )
            val label = tile.label.ifBlank { preset.name }
            Entry(
                id = preset.id,
                group = Group.ANIMATION,
                label = label,
                subtitleRes = Group.ANIMATION.titleRes,
                face = Face.Number(numberText),
                addToPanel = { ctx ->
                    AnimationTileSlots.requestAddTile(
                        ctx,
                        tile.slot,
                        label,
                        TileNumberIcon.create(numberText)
                    )
                },
                remove = { ctx -> PresetManager(ctx).setTileConfig(preset.id, null) }
            )
        }

    private fun widthTiles(context: Context): List<Entry> =
        WidthPresetManager(context).getAllPresets().mapNotNull { preset ->
            val tile = preset.tile ?: return@mapNotNull null
            val numberText = TileNumberIcon.widthText(preset.widthDp)
            val label = tile.label.ifBlank { preset.name }
            Entry(
                id = preset.id,
                group = Group.WIDTH,
                label = label,
                subtitleRes = Group.WIDTH.titleRes,
                face = Face.Number(numberText),
                addToPanel = { ctx ->
                    WidthTileSlots.requestAddTile(
                        ctx,
                        tile.slot,
                        label,
                        TileNumberIcon.create(numberText)
                    )
                },
                remove = { ctx -> WidthPresetManager(ctx).setTileConfig(preset.id, null) }
            )
        }

    private fun terminalTiles(context: Context): List<Entry> =
        TerminalPresetManager(context).getAllPresets().mapNotNull { preset ->
            val tile = preset.tile ?: return@mapNotNull null
            val label = tile.label.ifBlank { preset.name }
            Entry(
                id = preset.id,
                group = Group.TERMINAL,
                label = label,
                subtitleRes = Group.TERMINAL.titleRes,
                face = Face.Drawable(TerminalTileIcons.resFor(tile.iconKey)),
                addToPanel = { ctx ->
                    TerminalTileSlots.requestAddTile(ctx, tile.slot, label, tile.iconKey)
                },
                remove = { ctx -> TerminalPresetManager(ctx).setTileConfig(preset.id, null) }
            )
        }

    private fun profileTiles(context: Context): List<Entry> =
        ProfileManager(context).getAllProfiles().mapNotNull { profile ->
            val tile = profile.tile ?: return@mapNotNull null
            val label = tile.label.ifBlank { profile.name }
            Entry(
                id = profile.id,
                group = Group.PROFILE,
                label = label,
                subtitleRes = Group.PROFILE.titleRes,
                face = Face.Drawable(TerminalTileIcons.resFor(profile.iconKey)),
                addToPanel = { ctx ->
                    ProfileTileSlots.requestAddTile(ctx, tile.slot, label, profile.iconKey)
                },
                remove = { ctx -> ProfileManager(ctx).saveProfile(profile.copy(tile = null)) }
            )
        }
}
