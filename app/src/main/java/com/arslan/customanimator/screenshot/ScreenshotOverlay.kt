package com.arslan.customanimator.screenshot

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.VelocityTracker
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.arslan.customanimator.R

class ScreenshotOverlay(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null

    fun isShowing(): Boolean = view != null

    fun show(
        xDp: Int,
        yDp: Int,
        showCopy: Boolean,
        showDelete: Boolean,
        onCopy: () -> Unit,
        onDelete: () -> Unit
    ) {
        if (!showCopy && !showDelete) return
        view?.let { v ->
            bind(v, showCopy, showDelete, onCopy, onDelete)
            params?.let { p ->
                p.x = dpToPx(xDp)
                p.y = clearOfSystemPreview(xDp, yDp)
                runCatching { wm.updateViewLayout(v, p) }
            }
            v.animate().cancel()
            v.animate()
                .alpha(1f).translationX(0f)
                .setDuration(ENTER_MS)
                .setInterpolator(DecelerateInterpolator())
                .withLayer()
                .start()
            return
        }
        val v = LayoutInflater.from(context).inflate(R.layout.overlay_screenshot_actions, null)
        bind(v, showCopy, showDelete, onCopy, onDelete)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            x = dpToPx(xDp)
            y = clearOfSystemPreview(xDp, yDp)
        }

        v.alpha = 0f
        v.translationX = -slidePx()
        runCatching { wm.addView(v, lp) }
            .onSuccess {
                view = v
                params = lp
                v.animate()
                    .alpha(1f).translationX(0f)
                    .setDuration(ENTER_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .withLayer()
                    .start()
            }
    }

    private fun bind(
        root: View,
        showCopy: Boolean,
        showDelete: Boolean,
        onCopy: () -> Unit,
        onDelete: () -> Unit
    ) {
        val copy = root.findViewById<View>(R.id.overlay_copy)
        val delete = root.findViewById<View>(R.id.overlay_delete)
        copy.visibility = if (showCopy) View.VISIBLE else View.GONE
        delete.visibility = if (showDelete) View.VISIBLE else View.GONE
        attachDrag(root, copy, onCopy)
        attachDrag(root, delete, onDelete)
    }

    private fun attachDrag(root: View, target: View, onTap: () -> Unit) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        val minFling = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        var downX = 0f
        var startX = 0
        var dragging = false
        var tracker: VelocityTracker? = null
        target.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    startX = lp.x
                    dragging = false
                    tracker = VelocityTracker.obtain()
                    tracker?.addMovement(event)
                    target.isPressed = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(event)
                    val dx = event.rawX - downX
                    if (!dragging && kotlin.math.abs(dx) > slop) {
                        dragging = true
                        target.isPressed = false
                    }
                    if (dragging) {
                        lp.x = startX + dx.toInt()
                        root.alpha = dismissAlpha(dx)
                        runCatching { wm.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    target.isPressed = false
                    tracker?.addMovement(event)
                    tracker?.computeCurrentVelocity(1000)
                    val velocity = tracker?.xVelocity ?: 0f
                    tracker?.recycle()
                    tracker = null
                    val dx = event.rawX - downX
                    val dismissed = dragging &&
                        (kotlin.math.abs(dx) > dismissPx() ||
                            kotlin.math.abs(velocity) > minFling)
                    when {
                        event.actionMasked == MotionEvent.ACTION_CANCEL -> settle(root, startX)
                        dismissed -> hide()
                        dragging -> settle(root, startX)
                        else -> onTap()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun settle(root: View, startX: Int) {
        val lp = params ?: return
        val from = lp.x
        val animator = android.animation.ValueAnimator.ofInt(from, startX)
        animator.duration = ENTER_MS
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { a ->
            val current = params ?: return@addUpdateListener
            current.x = a.animatedValue as Int
            root.alpha = dismissAlpha((current.x - startX).toFloat())
            runCatching { wm.updateViewLayout(root, current) }
        }
        animator.start()
    }

    private fun dismissAlpha(dx: Float): Float {
        val fraction = kotlin.math.abs(dx) / dismissPx()
        return (1f - fraction * 0.6f).coerceIn(0.3f, 1f)
    }

    private fun dismissPx(): Float = dpToPx(DISMISS_DP).toFloat()

    fun hide() {
        val v = view ?: return
        view = null
        params = null
        v.findViewById<View>(R.id.overlay_copy).setOnTouchListener(null)
        v.findViewById<View>(R.id.overlay_delete).setOnTouchListener(null)
        v.animate()
            .alpha(0f).translationX(-slidePx())
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .withLayer()
            .withEndAction { v.post { runCatching { wm.removeView(v) } } }
            .start()
    }

    private fun clearOfSystemPreview(xDp: Int, yDp: Int): Int {
        val overlaps = xDp < PREVIEW_WIDTH_DP && yDp < PREVIEW_HEIGHT_DP
        return dpToPx(if (overlaps) PREVIEW_HEIGHT_DP else yDp)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun slidePx(): Float = dpToPx(SLIDE_DP).toFloat()

    private companion object {
        const val ENTER_MS = 220L
        const val SLIDE_DP = 56
        const val DISMISS_DP = 96
        const val PREVIEW_WIDTH_DP = 200
        const val PREVIEW_HEIGHT_DP = 260
    }
}
