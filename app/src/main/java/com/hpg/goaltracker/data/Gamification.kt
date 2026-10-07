package com.hpg.goaltracker.data

/**
 * Derives XP, levels and achievement badges from the existing goal data —
 * no extra persistence, recomputed from check-ins and completions.
 */
object Gamification {

    const val XP_PER_CHECKIN = 10
    const val XP_PER_COMPLETION = 100

    fun totalXp(goals: List<Goal>): Int =
        goals.sumOf { it.checkIns.size } * XP_PER_CHECKIN +
            goals.count { it.isCompleted } * XP_PER_COMPLETION

    /** Cumulative XP required to reach [level] (level 1 = 0 XP; each level costs 100×prev more). */
    fun cumulativeXpForLevel(level: Int): Int = 100 * (level - 1) * level / 2

    data class LevelInfo(
        val level: Int,
        val xp: Int,
        val intoLevel: Int,
        val levelSpan: Int,
    ) {
        val fraction: Float get() = if (levelSpan <= 0) 1f else (intoLevel.toFloat() / levelSpan).coerceIn(0f, 1f)
        val xpToNext: Int get() = (levelSpan - intoLevel).coerceAtLeast(0)
    }

    fun levelInfo(xp: Int): LevelInfo {
        var level = 1
        while (cumulativeXpForLevel(level + 1) <= xp) level++
        val base = cumulativeXpForLevel(level)
        val next = cumulativeXpForLevel(level + 1)
        return LevelInfo(level = level, xp = xp, intoLevel = xp - base, levelSpan = next - base)
    }

    data class Badge(
        val id: String,
        val emoji: String,
        val title: String,
        val description: String,
        val unlocked: Boolean,
    )

    fun badges(goals: List<Goal>): List<Badge> {
        val totalCheckIns = goals.sumOf { it.checkIns.size }
        val completed = goals.count { it.isCompleted }
        val bestStreak = goals.maxOfOrNull { it.bestStreak() } ?: 0
        val hasGoal = goals.isNotEmpty()

        return listOf(
            Badge("first_goal", "🎯", "Первый шаг", "Создать первую цель", hasGoal),
            Badge("first_checkin", "✅", "Поехали", "Сделать первую отметку", totalCheckIns >= 1),
            Badge("checkins_10", "🔟", "Разминка", "10 отметок", totalCheckIns >= 10),
            Badge("checkins_50", "💪", "В ритме", "50 отметок", totalCheckIns >= 50),
            Badge("checkins_100", "🚀", "Сотня", "100 отметок", totalCheckIns >= 100),
            Badge("streak_7", "🔥", "Неделя огня", "Серия 7 дней", bestStreak >= 7),
            Badge("streak_30", "🌟", "Месяц силы", "Серия 30 дней", bestStreak >= 30),
            Badge("first_win", "🏆", "Победитель", "Достичь первой цели", completed >= 1),
            Badge("wins_5", "👑", "Коллекционер", "5 достигнутых целей", completed >= 5),
        )
    }
}
