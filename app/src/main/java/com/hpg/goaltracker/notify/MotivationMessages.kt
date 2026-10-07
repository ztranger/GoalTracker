package com.hpg.goaltracker.notify

import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType

/** Builds short, motivating notification copy tailored to a goal's progress. */
object MotivationMessages {

    private val titles = listOf(
        "Время двигаться к цели! 🚀",
        "Твой прогресс ждёт тебя 💪",
        "Один шаг сегодня 🔥",
        "Не теряй темп! ⭐",
        "Сделай это сегодня ✨",
    )

    fun forGoal(goal: Goal): Pair<String, String> {
        val title = titles.random()
        val name = "«${goal.title}»"

        val body = when {
            goal.percent >= 80 ->
                "Ты уже на ${goal.percent}% к цели $name! Осталось совсем чуть-чуть — финишная прямая. Не останавливайся!"

            goal.type == GoalType.DAILY && goal.canCheckInNow() ->
                "Отметь сегодня $name — и прогресс пойдёт вверх. Уже ${goal.progress} из ${goal.target}. Маленький шаг сегодня = большая победа завтра!"

            goal.type == GoalType.COUNT ->
                "Цель $name: ${goal.progress} из ${goal.target}${unit(goal)}. Ещё ${goal.remaining}${unit(goal)} — и ты у вершины. Ты можешь больше!"

            else ->
                "Вернись к цели $name. Уже пройдено ${goal.percent}%. Каждый день приближает тебя к результату — продолжай!"
        }
        return title to body
    }

    private fun unit(goal: Goal): String =
        if (goal.unit.isBlank()) "" else " ${goal.unit}"
}
