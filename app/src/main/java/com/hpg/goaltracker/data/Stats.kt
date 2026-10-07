package com.hpg.goaltracker.data

import java.util.Calendar

private const val DAY_MS = 24L * 60 * 60 * 1000

/** Local midnight of the day containing [millis]. */
fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Distinct days (as local-midnight keys) on which this goal had any activity. */
private fun Goal.activityDayKeys(): HashSet<Long> =
    checkIns.map { startOfDay(it) }.toHashSet()

private fun dayStart(cal: Calendar) {
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
}

/**
 * Consecutive **scheduled** days the user kept the habit, counting back from today.
 * Rest days (weekdays not in [Goal.activeDays]) are skipped and never break a streak;
 * a missed past scheduled day does. Today being unfilled doesn't break it yet.
 */
fun Goal.currentStreak(now: Long = System.currentTimeMillis()): Int {
    if (type != GoalType.DAILY) return 0
    val days = activityDayKeys()
    if (days.isEmpty()) return 0
    val frozen = frozenDays.map { startOfDay(it) }.toHashSet()

    val todayKey = startOfDay(now)
    val earliest = days.min()
    val cal = Calendar.getInstance().apply { timeInMillis = todayKey }

    var streak = 0
    while (cal.timeInMillis >= earliest) {
        val key = cal.timeInMillis
        if (WeekMask.isSelected(activeDays, cal.get(Calendar.DAY_OF_WEEK))) {
            if (days.contains(key)) {
                streak++
            } else if (frozen.contains(key)) {
                // frozen day: bridges the streak without adding to it
            } else if (key != todayKey) {
                break // a past scheduled day was missed
            }
            // key == todayKey and not done yet: still open, keep counting earlier days
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dayStart(cal)
    }
    return streak
}

/** The longest run of consecutive scheduled days ever completed on this goal. */
fun Goal.bestStreak(now: Long = System.currentTimeMillis()): Int {
    if (type != GoalType.DAILY) return 0
    val days = activityDayKeys()
    if (days.isEmpty()) return 0
    val frozen = frozenDays.map { startOfDay(it) }.toHashSet()

    val todayKey = startOfDay(now)
    val cal = Calendar.getInstance().apply { timeInMillis = days.min() }

    var best = 0
    var run = 0
    while (cal.timeInMillis <= todayKey) {
        val key = cal.timeInMillis
        if (WeekMask.isSelected(activeDays, cal.get(Calendar.DAY_OF_WEEK))) {
            if (days.contains(key)) {
                run++
                if (run > best) best = run
            } else if (frozen.contains(key)) {
                // frozen day: bridges without breaking or incrementing
            } else if (key != todayKey) {
                run = 0 // missed a past scheduled day
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        dayStart(cal)
    }
    return best
}

data class DayActivity(val label: String, val count: Int, val isToday: Boolean)

data class AppStats(
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val totalCheckIns: Int,
    val completionRate: Int,
    val bestCurrentStreak: Int,
    val longestStreak: Int,
    val last7Days: List<DayActivity>,
)

private val WEEKDAY_SHORT = mapOf(
    Calendar.MONDAY to "Пн",
    Calendar.TUESDAY to "Вт",
    Calendar.WEDNESDAY to "Ср",
    Calendar.THURSDAY to "Чт",
    Calendar.FRIDAY to "Пт",
    Calendar.SATURDAY to "Сб",
    Calendar.SUNDAY to "Вс",
)

private fun weekdayLabel(dayKey: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = dayKey }
    return WEEKDAY_SHORT[c.get(Calendar.DAY_OF_WEEK)] ?: "?"
}

fun computeStats(goals: List<Goal>, now: Long = System.currentTimeMillis()): AppStats {
    val total = goals.size
    val completed = goals.count { it.isCompleted }
    val active = total - completed
    val totalCheckIns = goals.sumOf { it.checkIns.size }
    val rate = if (total == 0) 0 else completed * 100 / total
    val bestCurrent = goals.filter { !it.isCompleted }.maxOfOrNull { it.currentStreak(now) } ?: 0
    val longest = goals.maxOfOrNull { it.bestStreak() } ?: 0

    val todayKey = startOfDay(now)
    val last7 = (6 downTo 0).map { back ->
        val key = todayKey - back * DAY_MS
        val count = goals.sumOf { g -> g.checkIns.count { startOfDay(it) == key } }
        DayActivity(label = weekdayLabel(key), count = count, isToday = back == 0)
    }

    return AppStats(
        totalGoals = total,
        activeGoals = active,
        completedGoals = completed,
        totalCheckIns = totalCheckIns,
        completionRate = rate,
        bestCurrentStreak = bestCurrent,
        longestStreak = longest,
        last7Days = last7,
    )
}
