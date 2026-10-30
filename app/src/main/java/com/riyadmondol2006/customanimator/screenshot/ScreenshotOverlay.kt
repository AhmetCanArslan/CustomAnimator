package com.riyadmondol2006.customanimator.screenshot

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.riyadmondol2006.customanimator.R

class ScreenshotOverlay(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var summonAnimator: ValueAnimator? = null

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
            summonAnimator?.end()
            bind(v, showCopy, showDelete, onCopy, onDelete)
            params?.let { p ->
                p.x = dpToPx(xDp)
                p.y = dpToPx(yDp)
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
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            y = dpToPx(yDp)
        }

        v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxX = (screenWidth - v.measuredWidth).coerceAtLeast(0)
        val targetX = dpToPx(xDp).coerceIn(0, maxX)
        val edgeX = if (targetX + v.measuredWidth / 2 < screenWidth / 2) -v.measuredWidth else screenWidth
        placeContent(v, lp, edgeX, maxX)
        runCatching { wm.addView(v, lp) }
            .onSuccess {
                view = v
                params = lp
                summon(v, lp, edgeX, targetX, maxX)
            }
    }

    private fun summon(root: View, lp: WindowManager.LayoutParams, fromX: Int, toX: Int, maxX: Int) {
        val animator = ValueAnimator.ofInt(fromX, toX)
        animator.duration = SUMMON_MS
        animator.interpolator = DecelerateInterpolator(SUMMON_DECELERATION)
        animator.addUpdateListener { a ->
            if (params !== lp) return@addUpdateListener
            placeContent(root, lp, a.animatedValue as Int, maxX)
            runCatching { wm.updateViewLayout(root, lp) }
        }
        summonAnimator = animator
        animator.start()
    }

    private fun placeContent(root: View, lp: WindowManager.LayoutParams, contentX: Int, maxX: Int) {
        lp.x = contentX.coerceIn(0, maxX)
        root.translationX = (contentX - lp.x).toFloat()
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
        var downX = 0f
        var startX = 0
        var dragging = false
        target.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    summonAnimator?.end()
                    downX = event.rawX
                    startX = lp.x
                    dragging = false
                    target.drawableHotspotChanged(event.x, event.y)
                    press(target, true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    if (!dragging && kotlin.math.abs(dx) > slop) {
                        dragging = true
                        press(target, false)
                    }
                    if (dragging) {
                        lp.x = (startX + dx.toInt()).coerceIn(0, maxX(root))
                        root.alpha = dismissAlpha(root, startX, lp.x)
                        runCatching { wm.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    press(target, false)
                    val atEdge = lp.x <= 0 || lp.x >= maxX(root)
                    when {
                        event.actionMasked == MotionEvent.ACTION_CANCEL -> settle(root, startX)
                        dragging && atEdge -> hide(if (lp.x <= 0) -1f else 1f)
                        dragging -> settle(root, startX)
                        else -> {
                            target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onTap()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun press(target: View, pressed: Boolean) {
        target.isPressed = pressed
        val scale = if (pressed) PRESSED_SCALE else 1f
        target.animate().cancel()
        target.animate()
            .scaleX(scale).scaleY(scale)
            .setDuration(PRESS_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun settle(root: View, startX: Int) {
        val lp = params ?: return
        val from = lp.x
        val animator = ValueAnimator.ofInt(from, startX)
        animator.duration = ENTER_MS
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { a ->
            val current = params ?: return@addUpdateListener
            current.x = a.animatedValue as Int
            root.alpha = dismissAlpha(root, startX, current.x)
            runCatching { wm.updateViewLayout(root, current) }
        }
        animator.start()
    }

    private fun maxX(root: View): Int =
        (context.resources.displayMetrics.widthPixels - root.width).coerceAtLeast(0)

    private fun dismissAlpha(root: View, startX: Int, x: Int): Float {
        val edge = if (x < startX) 0 else maxX(root)
        val travel = kotlin.math.abs(edge - startX)
        if (travel == 0) return 1f
        val fraction = kotlin.math.abs(x - startX).toFloat() / travel
        return (1f - fraction * 0.6f).coerceIn(0.4f, 1f)
    }

    fun hide(direction: Float = -1f) {
        val v = view ?: return
        summonAnimator?.cancel()
        summonAnimator = null
        view = null
        params = null
        v.findViewById<View>(R.id.overlay_copy).setOnTouchListener(null)
        v.findViewById<View>(R.id.overlay_delete).setOnTouchListener(null)
        v.animate()
            .alpha(0f).translationX(direction * slidePx())
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .withLayer()
            .withEndAction { v.post { runCatching { wm.removeView(v) } } }
            .start()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun slidePx(): Float = dpToPx(SLIDE_DP).toFloat()

    private companion object {
        const val ENTER_MS = 220L
        const val SUMMON_MS = 420L
        const val SUMMON_DECELERATION = 2f
        const val PRESS_MS = 90L
        const val PRESSED_SCALE = 0.86f
        const val SLIDE_DP = 56
    }
}
