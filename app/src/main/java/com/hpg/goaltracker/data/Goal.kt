package com.hpg.goaltracker.data

import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

/**
 * DAILY  — a habit to repeat on a schedule (e.g. "do exercise every day").
 *          [target] = how many check-ins complete the goal (duration),
 *          [periodDays] = how often it should be done (every N days).
 * COUNT  — a numeric target to reach, not tied to a schedule
 *          (e.g. "learn to do 10 pull-ups"). [target] = the number to reach,
 *          [progress] = current value.
 */
enum class GoalType { DAILY, COUNT }

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "🎯",
    val imagePath: String? = null,
    val colorArgb: Int,
    val type: GoalType,
    val target: Int,
    val periodDays: Int = 1,
    val progress: Int = 0,
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val lastCheckIn: Long? = null,
    val notify: Boolean = true,
) {
    val isCompleted: Boolean get() = completedAt != null

    val fraction: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)

    val percent: Int get() = (fraction * 100).roundToInt()

    val remaining: Int get() = (target - progress).coerceAtLeast(0)

    /** For DAILY goals: whether a new check-in is allowed in the current period. */
    fun canCheckInNow(now: Long = System.currentTimeMillis()): Boolean {
        if (isCompleted) return false
        if (type != GoalType.DAILY) return true
        val last = lastCheckIn ?: return true
        return daysBetween(last, now) >= periodDays.coerceAtLeast(1)
    }

    companion object {
        private fun startOfDay(millis: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = millis
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        /** Whole calendar days between two instants (local time zone). */
        fun daysBetween(from: Long, to: Long): Int {
            val dayMs = 24L * 60 * 60 * 1000
            return ((startOfDay(to) - startOfDay(from)) / dayMs).toInt()
        }
    }
}
