package com.mcclicker.white

import android.content.Context
import android.content.SharedPreferences

/** Lưu cài đặt đơn giản bằng SharedPreferences. */
class ClickPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("mc_clicker", Context.MODE_PRIVATE)

    var intervalMs: Long
        get() = sp.getLong("interval", 100L)
        set(v) = sp.edit().putLong("interval", v).apply()

    var limited: Boolean
        get() = sp.getBoolean("limited", false)
        set(v) = sp.edit().putBoolean("limited", v).apply()

    var count: Int
        get() = sp.getInt("count", 100)
        set(v) = sp.edit().putInt("count", v).apply()

    var keepAwake: Boolean
        get() = sp.getBoolean("keepAwake", false)
        set(v) = sp.edit().putBoolean("keepAwake", v).apply()

    // Trạng thái đồng hồ bấm giờ (dùng elapsedRealtime nên không bị sai khi app xuống nền)
    var swRunning: Boolean
        get() = sp.getBoolean("swRunning", false)
        set(v) = sp.edit().putBoolean("swRunning", v).apply()

    var swBase: Long
        get() = sp.getLong("swBase", 0L)
        set(v) = sp.edit().putLong("swBase", v).apply()

    var swAccum: Long
        get() = sp.getLong("swAccum", 0L)
        set(v) = sp.edit().putLong("swAccum", v).apply()
}
