package com.mcclicker.white

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

/**
 * Dịch vụ Trợ năng: thực hiện chạm bằng GestureDescription và hiển thị
 * nút nổi + dấu chọn vị trí bằng cửa sổ TYPE_ACCESSIBILITY_OVERLAY
 * (không cần quyền "Hiển thị trên ứng dụng khác").
 */
class ClickerService : AccessibilityService() {

    companion object {
        const val MIN_INTERVAL_MS = 20L

        @Volatile
        var instance: ClickerService? = null
            private set

        /** UI (MainActivity) đăng ký để nhận thông báo đổi trạng thái. */
        var listener: (() -> Unit)? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager

    private var panel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var marker: View? = null
    private var markerParams: WindowManager.LayoutParams? = null

    var running = false
        private set
    var clicksDone = 0
        private set

    private var intervalMs = 100L
    private var maxClicks = 0 // 0 = liên tục
    private var runId = 0     // chống callback cũ chạy chồng

    val overlayVisible: Boolean get() = panel != null

    private val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    // ---------------- Vòng đời ----------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        instance = this
        notifyUi()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Không cần xử lý sự kiện.
    }

    override fun onInterrupt() {
        stopClicking()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        running = false
        runId++
        handler.removeCallbacksAndMessages(null)
        removeOverlayViews()
        instance = null
        notifyUi()
    }

    private fun notifyUi() {
        listener?.invoke()
    }

    // ---------------- Auto click ----------------

    fun startClicking(): Boolean {
        if (running) return false
        if (marker == null) showOverlay()
        val prefs = ClickPrefs(this)
        intervalMs = prefs.intervalMs.coerceAtLeast(MIN_INTERVAL_MS)
        maxClicks = if (prefs.limited) prefs.count.coerceAtLeast(1) else 0
        clicksDone = 0
        handler.removeCallbacksAndMessages(null)
        runId++
        running = true
        setMarkerTouchable(false)
        updatePanel()
        notifyUi()
        scheduleNext(runId, 300L)
        return true
    }

    fun stopClicking() {
        val was = running
        running = false
        runId++
        handler.removeCallbacksAndMessages(null)
        setMarkerTouchable(true)
        updatePanel()
        if (was) notifyUi()
    }

    private fun scheduleNext(id: Int, delayMs: Long) {
        handler.postDelayed({
            if (running && id == runId) doClick(id)
        }, delayMs)
    }

    private fun targetPoint(): Pair<Float, Float>? {
        val m = marker ?: return null
        if (m.width == 0) return null
        val loc = IntArray(2)
        m.getLocationOnScreen(loc)
        return Pair(loc[0] + m.width / 2f, loc[1] + m.height / 2f)
    }

    private fun doClick(id: Int) {
        val point = targetPoint()
        if (point == null) {
            stopClicking()
            return
        }
        val path = Path().apply { moveTo(point.first, point.second) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 40L))
            .build()
        val started = SystemClock.uptimeMillis()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                afterClick(id, started, true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                afterClick(id, started, false)
            }
        }, null)
        if (!accepted) afterClick(id, started, false)
    }

    private fun afterClick(id: Int, started: Long, success: Boolean) {
        if (!running || id != runId) return
        if (success) clicksDone++
        updatePanel()
        notifyUi()
        if (maxClicks > 0 && clicksDone >= maxClicks) {
            stopClicking()
            return
        }
        val elapsed = SystemClock.uptimeMillis() - started
        var delay = (intervalMs - elapsed).coerceAtLeast(0L)
        if (!success) delay = delay.coerceAtLeast(100L)
        scheduleNext(id, delay)
    }

    // ---------------- Cửa sổ nổi ----------------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    fun showOverlay() {
        if (panel != null) return
        val dm = resources.displayMetrics

        // Dấu chọn vị trí chạm
        val size = dp(56)
        val mv = ImageView(this).apply { setImageResource(R.drawable.ic_target) }
        val mp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            baseFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels - size) / 2
            y = (dm.heightPixels - size) / 2
        }

        // Bảng điều khiển nổi
        val pv = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)
        val pp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            baseFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(160)
        }

        try {
            wm.addView(mv, mp)
            wm.addView(pv, pp)
        } catch (e: Exception) {
            try { wm.removeView(mv) } catch (_: Exception) {}
            Toast.makeText(this, "Không thể hiển thị nút nổi", Toast.LENGTH_SHORT).show()
            return
        }
        marker = mv; markerParams = mp
        panel = pv; panelParams = pp

        attachDrag(mv, mv, mp)
        attachDrag(pv.findViewById(R.id.panelHandle), pv, pp)

        pv.findViewById<TextView>(R.id.panelToggle).setOnClickListener {
            if (running) stopClicking() else startClicking()
        }
        pv.findViewById<TextView>(R.id.panelClose).setOnClickListener {
            stopClicking()
            hideOverlay()
        }
        setMarkerTouchable(!running)
        updatePanel()
        notifyUi()
    }

    fun hideOverlay() {
        stopClicking()
        removeOverlayViews()
        notifyUi()
    }

    private fun removeOverlayViews() {
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { marker?.let { wm.removeView(it) } } catch (_: Exception) {}
        panel = null; panelParams = null
        marker = null; markerParams = null
    }

    private fun setMarkerTouchable(touchable: Boolean) {
        val v = marker ?: return
        val p = markerParams ?: return
        p.flags = if (touchable) baseFlags
        else baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
    }

    private fun updatePanel() {
        val t = panel?.findViewById<TextView>(R.id.panelToggle) ?: return
        if (running) {
            t.text = "DỪNG · $clicksDone"
            t.setBackgroundResource(R.drawable.bg_btn_red)
        } else {
            t.text = "BẮT ĐẦU"
            t.setBackgroundResource(R.drawable.bg_btn_green)
        }
    }

    /** Kéo thả: touchView nhận cử chỉ, targetView là cửa sổ được di chuyển. */
    private fun attachDrag(touchView: View, targetView: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        touchView.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    downX = ev.rawX; downY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (ev.rawX - downX).toInt()
                    params.y = startY + (ev.rawY - downY).toInt()
                    try { wm.updateViewLayout(targetView, params) } catch (_: Exception) {}
                    true
                }
                else -> true
            }
        }
    }
}
