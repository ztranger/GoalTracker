package com.hpg.goaltracker.data

/** A ready-made goal the user can start from (and then tweak in the editor). */
data class GoalTemplate(
    val emoji: String,
    val title: String,
    val type: GoalType,
    val target: Int,
    val unit: String,
    val activeDays: Int,
    val category: String,
    val colorArgb: Int,
) {
    fun toGoal(): Goal = Goal(
        title = title,
        emoji = emoji,
        colorArgb = colorArgb,
        type = type,
        target = target,
        periodDays = 1,
        activeDays = activeDays,
        category = category,
        unit = unit,
    )
}

object GoalTemplates {
    val list: List<GoalTemplate> = listOf(
        GoalTemplate("💪", "Делать зарядку", GoalType.DAILY, 30, "", WeekMask.ALL_DAYS, "Здоровье", 0xFF6C4CF1.toInt()),
        GoalTemplate("💧", "Пить воду", GoalType.DAILY, 30, "", WeekMask.ALL_DAYS, "Здоровье", 0xFF00B8D4.toInt()),
        GoalTemplate("📚", "Читать каждый день", GoalType.DAILY, 30, "", WeekMask.ALL_DAYS, "Развитие", 0xFFFF8A3D.toInt()),
        GoalTemplate("🧘", "Медитация", GoalType.DAILY, 21, "", WeekMask.ALL_DAYS, "Здоровье", 0xFF2ECC71.toInt()),
        GoalTemplate("🗣️", "Учить английский", GoalType.DAILY, 30, "", WeekMask.ALL_DAYS, "Развитие", 0xFF4D96FF.toInt()),
        GoalTemplate("🏃", "Пробежка", GoalType.DAILY, 20, "", WeekMask.WEEKDAYS, "Спорт", 0xFFE84393.toInt()),
        GoalTemplate("☀️", "Ранний подъём", GoalType.DAILY, 21, "", WeekMask.ALL_DAYS, "Привычки", 0xFFFFB300.toInt()),
        GoalTemplate("🚭", "Без сахара", GoalType.DAILY, 30, "", WeekMask.ALL_DAYS, "Здоровье", 0xFFFF6B6B.toInt()),
        GoalTemplate("🏋️", "Подтягивания 10 раз", GoalType.COUNT, 10, "раз", WeekMask.ALL_DAYS, "Спорт", 0xFF8E7CFF.toInt()),
    )
}
