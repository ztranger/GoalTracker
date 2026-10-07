package com.hpg.goaltracker.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** App-wide appearance preferences and the monthly streak-freeze budget. */
object AppSettings {

    private const val PREFS = "goal_app_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_FREEZE_MONTH = "freeze_month"
    private const val KEY_FREEZE_USED = "freeze_used"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    const val FREEZES_PER_MONTH = 3

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun currentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun usedThisMonth(context: Context): Int {
        val p = prefs(context)
        return if (p.getString(KEY_FREEZE_MONTH, "") == currentMonthKey()) {
            p.getInt(KEY_FREEZE_USED, 0)
        } else 0
    }

    fun availableFreezes(context: Context): Int =
        (FREEZES_PER_MONTH - usedThisMonth(context)).coerceAtLeast(0)

    /** Consumes one freeze if available this month; returns true on success. */
    fun consumeFreeze(context: Context): Boolean {
        val used = usedThisMonth(context)
        if (used >= FREEZES_PER_MONTH) return false
        prefs(context).edit()
            .putString(KEY_FREEZE_MONTH, currentMonthKey())
            .putInt(KEY_FREEZE_USED, used + 1)
            .apply()
        return true
    }

    fun themeMode(context: Context): Int = prefs(context).getInt(KEY_THEME, THEME_SYSTEM)

    fun dynamicColor(context: Context): Boolean = prefs(context).getBoolean(KEY_DYNAMIC, false)

    fun setTheme(context: Context, themeMode: Int, dynamicColor: Boolean) {
        prefs(context).edit()
            .putInt(KEY_THEME, themeMode)
            .putBoolean(KEY_DYNAMIC, dynamicColor)
            .apply()
    }

    fun onboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }
}
