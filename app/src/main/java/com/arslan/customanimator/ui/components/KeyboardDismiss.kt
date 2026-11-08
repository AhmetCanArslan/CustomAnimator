package com.arslan.customanimator.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.getFocusedRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntSize

fun Modifier.clearFocusOnTapOutside(): Modifier = this then ClearFocusOnTapOutsideElement

private object ClearFocusOnTapOutsideElement : ModifierNodeElement<ClearFocusOnTapOutsideNode>() {
    override fun create() = ClearFocusOnTapOutsideNode()

    override fun update(node: ClearFocusOnTapOutsideNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "clearFocusOnTapOutside"
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun equals(other: Any?): Boolean = other === this
}

private class ClearFocusOnTapOutsideNode :
    DelegatingNode(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private val focusTarget = delegate(FocusTargetModifierNode(focusability = Focusability.Never))
    private var pressPosition: Offset? = null

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        val change = pointerEvent.changes.firstOrNull() ?: return
        when {
            pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Press -> pressPosition = change.position
            pass == PointerEventPass.Final && pointerEvent.type == PointerEventType.Release -> clearFocusIfOutside()
        }
    }

    private fun clearFocusIfOutside() {
        val position = pressPosition ?: return
        pressPosition = null
        val focused = focusTarget.getFocusedRect() ?: return
        if (!focused.contains(position)) currentValueOf(LocalFocusManager).clearFocus()
    }

    override fun onCancelPointerInput() {
        pressPosition = null
    }
}
