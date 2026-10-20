package com.mcclicker.white

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: ClickPrefs
    private lateinit var root: View
    private lateinit var tvStatus: TextView
    private lateinit var tvTime: TextView
    private lateinit var etInterval: EditText
    private lateinit var etCount: EditText
    private lateinit var rbLimited: RadioButton
    private lateinit var rbContinuous: RadioButton
    private lateinit var cbKeepAwake: CheckBox
    private lateinit var btnOverlay: TextView

    private val ui = Handler(Looper.getMainLooper())
    private var swRunning = false
    private var swBase = 0L
    private var swAccum = 0L

    private val ticker = object : Runnable {
        override fun run() {
            renderTime()
            if (swRunning) ui.postDelayed(this, 16L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = ClickPrefs(this)

        root = findViewById(R.id.root)
        tvStatus = findViewById(R.id.tvStatus)
        tvTime = findViewById(R.id.tvTime)
        etInterval = findViewById(R.id.etInterval)
        etCount = findViewById(R.id.etCount)
        rbLimited = findViewById(R.id.rbLimited)
        rbContinuous = findViewById(R.id.rbContinuous)
        cbKeepAwake = findViewById(R.id.cbKeepAwake)
        btnOverlay = findViewById(R.id.btnOverlay)

        etInterval.setText(prefs.intervalMs.toString())
        etCount.setText(prefs.count.toString())
        rbLimited.isChecked = prefs.limited
        rbContinuous.isChecked = !prefs.limited
        cbKeepAwake.isChecked = prefs.keepAwake
        root.keepScreenOn = prefs.keepAwake
        cbKeepAwake.setOnCheckedChangeListener { _, checked ->
            prefs.keepAwake = checked
            root.keepScreenOn = checked
        }

        // Khôi phục đồng hồ
        swRunning = prefs.swRunning
        swBase = prefs.swBase
        swAccum = prefs.swAccum
        if (swRunning && swBase > SystemClock.elapsedRealtime()) { // máy đã khởi động lại
            swRunning = false; swAccum = 0L; saveStopwatch()
        }

        findViewById<View>(R.id.btnAccessibility).setOnClickListener { openAccessibilitySettings() }
        btnOverlay.setOnClickListener { onOverlayClicked() }
        findViewById<View>(R.id.btnStart).setOnClickListener { onStartClicked() }
        findViewById<View>(R.id.btnStop).setOnClickListener { ClickerService.instance?.stopClicking() }

        findViewById<View>(R.id.btnSwStart).setOnClickListener { swStart() }
        findViewById<View>(R.id.btnSwPause).setOnClickListener { swPause() }
        findViewById<View>(R.id.btnSwReset).setOnClickListener { swReset() }
    }

    override fun onResume() {
        super.onResume()
        ClickerService.listener = { refreshStatus() }
        refreshStatus()
        ui.removeCallbacks(ticker)
        ui.post(ticker)
    }

    override fun onPause() {
        ClickerService.listener = null
        ui.removeCallbacks(ticker)
        saveSettings()
        super.onPause()
    }

    // ---------------- Auto click ----------------

    private fun saveSettings() {
        val interval = etInterval.text.toString().toLongOrNull() ?: 100L
        prefs.intervalMs = interval.coerceIn(ClickerService.MIN_INTERVAL_MS, 3_600_000L)
        prefs.count = (etCount.text.toString().toIntOrNull() ?: 100).coerceAtLeast(1)
        prefs.limited = rbLimited.isChecked
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Không mở được Cài đặt Trợ năng", Toast.LENGTH_LONG).show()
        }
    }

    private fun requireService(): ClickerService? {
        val svc = ClickerService.instance
        if (svc == null) {
            Toast.makeText(
                this,
                "Hãy bật Minecraft Auto Clicker trong Cài đặt → Trợ năng",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings()
        }
        return svc
    }

    private fun onOverlayClicked() {
        val svc = requireService() ?: return
        saveSettings()
        if (svc.overlayVisible) {
            svc.hideOverlay()
        } else {
            svc.showOverlay()
            Toast.makeText(this, "Kéo dấu + tới vị trí cần chạm", Toast.LENGTH_LONG).show()
            moveTaskToBack(true)
        }
        refreshStatus()
    }

    private fun onStartClicked() {
        val svc = requireService() ?: return
        saveSettings()
        if (!svc.overlayVisible) {
            svc.showOverlay()
            Toast.makeText(
                this,
                "Kéo dấu + tới vị trí cần chạm rồi bấm BẮT ĐẦU trên nút nổi",
                Toast.LENGTH_LONG
            ).show()
            moveTaskToBack(true)
            return
        }
        if (svc.startClicking()) moveTaskToBack(true)
    }

    private fun refreshStatus() {
        val svc = ClickerService.instance
        val sb = StringBuilder()
        if (svc == null) {
            sb.append("Trợ năng: CHƯA BẬT\nHãy bật dịch vụ để dùng auto click.")
        } else {
            sb.append("Trợ năng: ĐÃ BẬT\n")
            if (svc.running) sb.append("Đang click: ${svc.clicksDone} lần")
            else sb.append("Đang dừng")
        }
        tvStatus.text = sb.toString()
        btnOverlay.text =
            if (svc?.overlayVisible == true) "ẨN NÚT NỔI" else "HIỆN NÚT NỔI & CHỌN VỊ TRÍ"
    }

    // ---------------- Đồng hồ bấm giờ ----------------

    private fun elapsed(): Long =
        swAccum + if (swRunning) SystemClock.elapsedRealtime() - swBase else 0L

    private fun renderTime() {
        val ms = elapsed()
        val cs = (ms / 10) % 100
        val sec = (ms / 1000) % 60
        val min = ms / 60000
        tvTime.text = String.format("%02d:%02d.%02d", min, sec, cs)
    }

    private fun saveStopwatch() {
        prefs.swRunning = swRunning
        prefs.swBase = swBase
        prefs.swAccum = swAccum
    }

    private fun swStart() {
        if (swRunning) return
        swBase = SystemClock.elapsedRealtime()
        swRunning = true
        saveStopwatch()
        ui.removeCallbacks(ticker)
        ui.post(ticker)
    }

    private fun swPause() {
        if (!swRunning) return
        swAccum += SystemClock.elapsedRealtime() - swBase
        swRunning = false
        saveStopwatch()
        renderTime()
    }

    private fun swReset() {
        swRunning = false
        swAccum = 0L
        swBase = 0L
        saveStopwatch()
        renderTime()
    }
}
