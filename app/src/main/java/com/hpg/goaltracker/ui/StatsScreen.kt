package com.hpg.goaltracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.AppStats
import com.hpg.goaltracker.data.DayActivity
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.Gamification
import com.hpg.goaltracker.data.bestStreak
import com.hpg.goaltracker.data.computeStats
import com.hpg.goaltracker.data.currentStreak
import com.hpg.goaltracker.ui.theme.Dimens

@Composable
fun StatsScreen(goals: List<Goal>, modifier: Modifier = Modifier) {
    if (goals.isEmpty()) {
        EmptyState(
            emoji = "📊",
            title = "Пока нет статистики",
            subtitle = "Создайте цель и начните отмечать прогресс — здесь появятся серии и достижения.",
            modifier = modifier
        )
        return
    }
    val stats = remember(goals) { computeStats(goals) }
    val level = remember(goals) { Gamification.levelInfo(Gamification.totalXp(goals)) }
    val badges = remember(goals) { Gamification.badges(goals) }
    val ordered = remember(goals) {
        goals.sortedWith(compareBy({ it.isCompleted }, { -it.createdAt }))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { LevelCard(level) }
        item { HeroGrid(stats) }
        item { CompletionCard(stats) }
        item { WeeklyActivityCard(stats.last7Days) }
        item { BadgesCard(badges) }
        item {
            Text(
                "По целям",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(ordered, key = { it.id }) { goal ->
            GoalStatRow(goal)
        }
    }
}

@Composable
private fun LevelCard(level: Gamification.LevelInfo) {
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "⭐ Уровень ${level.level}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${level.xp} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(10.dp))
            val animated by animateFloatAsState(level.fraction, tween(600), label = "xp")
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "До уровня ${level.level + 1}: ${level.xpToNext} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun BadgesCard(badges: List<Gamification.Badge>) {
    val unlocked = badges.count { it.unlocked }
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Достижения", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "$unlocked / ${badges.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            badges.chunked(4).forEach { rowBadges ->
                Row(Modifier.fillMaxWidth()) {
                    rowBadges.forEach { badge ->
                        BadgeCell(badge, Modifier.weight(1f))
                    }
                    repeat(4 - rowBadges.size) { Box(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BadgeCell(badge: Gamification.Badge, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (badge.unlocked) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (badge.unlocked) badge.emoji else "🔒",
                fontSize = 24.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            badge.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HeroGrid(stats: AppStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("🔥", stats.bestCurrentStreak.toString(), "Серия сейчас", Modifier.weight(1f))
            StatCard("🏅", stats.longestStreak.toString(), "Рекорд серии", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("✅", stats.totalCheckIns.toString(), "Всего отметок", Modifier.weight(1f))
            StatCard("🏆", "${stats.completedGoals}/${stats.totalGoals}", "Целей достигнуто", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletionCard(stats: AppStats) {
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Выполнено целей", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${stats.completionRate}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stats.completionRate / 100f },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Активных: ${stats.activeGoals} • Достигнуто: ${stats.completedGoals}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(days: List<DayActivity>) {
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Активность за неделю", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    val intensity = if (day.count == 0) 0f else 0.35f + 0.65f * (day.count.toFloat() / maxCount)
                    val cellColor = if (day.count == 0)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.count > 0) {
                                Text(
                                    day.count.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            day.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStatRow(goal: Goal) {
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalAvatar(goal, size = 44)
            GapW(12)
            Column(Modifier.weight(1f)) {
                Text(
                    goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    goalStatLine(goal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (goal.isCompleted) "🏆" else "${goal.percent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (goal.isCompleted) MaterialTheme.colorScheme.tertiary else goal.accentColor()
            )
        }
    }
}

private fun goalStatLine(goal: Goal): String = when (goal.type) {
    GoalType.DAILY -> "🔥 Серия: ${goal.currentStreak()} • Рекорд: ${goal.bestStreak()} • Отметок: ${goal.checkIns.size}"
    GoalType.COUNT -> "${goal.progress} из ${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}"
}
