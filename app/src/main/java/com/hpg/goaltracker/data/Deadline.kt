package com.hpg.goaltracker.data

import java.util.concurrent.TimeUnit

enum class DeadlineStatus { NONE, AHEAD, BEHIND, OVERDUE, DONE }

private val DAY_MS = TimeUnit.DAYS.toMillis(1)

/** Whole days from today to the deadline day (negative = overdue), or null if no deadline. */
fun Goal.daysLeft(now: Long = System.currentTimeMillis()): Int? {
    val d = deadline ?: return null
    return ((startOfDay(d) - startOfDay(now)) / DAY_MS).toInt()
}

fun Goal.deadlineStatus(now: Long = System.currentTimeMillis()): DeadlineStatus {
    deadline ?: return DeadlineStatus.NONE
    if (isCompleted) return DeadlineStatus.DONE
    val left = daysLeft(now) ?: return DeadlineStatus.NONE
    if (left < 0) return DeadlineStatus.OVERDUE

    val total = (deadline - createdAt).coerceAtLeast(1)
    val elapsed = (now - createdAt).coerceIn(0, total)
    val expected = elapsed.toFloat() / total
    return if (fraction >= expected) DeadlineStatus.AHEAD else DeadlineStatus.BEHIND
}

/** Short badge for cards, e.g. "⏳ 5 дн." / "⚠ просрочено", or null when not applicable. */
fun Goal.deadlineBadge(now: Long = System.currentTimeMillis()): String? = when (deadlineStatus(now)) {
    DeadlineStatus.NONE, DeadlineStatus.DONE -> null
    DeadlineStatus.OVERDUE -> "⚠ просрочено"
    else -> when (val left = daysLeft(now) ?: return null) {
        0 -> "⏳ сегодня"
        else -> "⏳ $left дн."
    }
}
