package com.hpg.goaltracker.data

import android.content.Context

/** Persistent user preferences for reminders (SharedPreferences-backed). */
object ReminderSettings {

    private const val PREFS = "goal_reminder_settings"

    private const val KEY_STREAK_ENABLED = "streak_enabled"
    private const val KEY_STREAK_HOUR = "streak_hour"
    private const val KEY_STREAK_MINUTE = "streak_minute"
    private const val KEY_STREAK_DAYS = "streak_days"

    private const val KEY_DAILY_ENABLED = "daily_enabled"
    private const val KEY_DAILY_HOUR = "daily_hour"
    private const val KEY_DAILY_MINUTE = "daily_minute"
    private const val KEY_DAILY_DAYS = "daily_days"

    const val DEFAULT_STREAK_HOUR = 21
    const val DEFAULT_DAILY_HOUR = 19
    const val DEFAULT_MINUTE = 0

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Evening "streak at risk" reminder
    fun streakEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STREAK_ENABLED, true)

    fun streakHour(context: Context): Int =
        prefs(context).getInt(KEY_STREAK_HOUR, DEFAULT_STREAK_HOUR)

    fun streakMinute(context: Context): Int =
        prefs(context).getInt(KEY_STREAK_MINUTE, DEFAULT_MINUTE)

    fun streakDays(context: Context): Int =
        prefs(context).getInt(KEY_STREAK_DAYS, WeekMask.ALL_DAYS)

    fun setStreak(context: Context, enabled: Boolean, hour: Int, minute: Int, days: Int) {
        prefs(context).edit()
            .putBoolean(KEY_STREAK_ENABLED, enabled)
            .putInt(KEY_STREAK_HOUR, hour)
            .putInt(KEY_STREAK_MINUTE, minute)
            .putInt(KEY_STREAK_DAYS, days)
            .apply()
    }

    // General daily motivational reminder
    fun dailyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DAILY_ENABLED, true)

    fun dailyHour(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_HOUR, DEFAULT_DAILY_HOUR)

    fun dailyMinute(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_MINUTE, DEFAULT_MINUTE)

    fun dailyDays(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_DAYS, WeekMask.ALL_DAYS)

    fun setDaily(context: Context, enabled: Boolean, hour: Int, minute: Int, days: Int) {
        prefs(context).edit()
            .putBoolean(KEY_DAILY_ENABLED, enabled)
            .putInt(KEY_DAILY_HOUR, hour)
            .putInt(KEY_DAILY_MINUTE, minute)
            .putInt(KEY_DAILY_DAYS, days)
            .apply()
    }
}
