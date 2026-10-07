package com.hpg.goaltracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType

@Composable
fun GoalsScreen(
    goals: List<Goal>,
    onCheckIn: (Goal) -> Unit,
    onChangeCount: (Goal, Int) -> Unit,
    onSetCount: (Goal, Int) -> Unit,
    onEdit: (Goal) -> Unit,
    onDuplicate: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    onToggleNotify: (Goal) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) {
        EmptyState(
            emoji = "🎯",
            title = "Пока нет целей",
            subtitle = "Нажмите «+», чтобы поставить первую цель и начать свой путь к победам.",
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(goals, key = { it.id }) { goal ->
            GoalCard(
                goal = goal,
                onCheckIn = { onCheckIn(goal) },
                onChangeCount = { delta -> onChangeCount(goal, delta) },
                onSetCount = { value -> onSetCount(goal, value) },
                onEdit = { onEdit(goal) },
                onDuplicate = { onDuplicate(goal) },
                onDelete = { onDelete(goal) },
                onToggleNotify = { onToggleNotify(goal) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    onCheckIn: () -> Unit,
    onChangeCount: (Int) -> Unit,
    onSetCount: (Int) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleNotify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = goal.accentColor()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCountDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalAvatar(goal)
                GapW(12)
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        goal.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GoalMenu(
                    notify = goal.notify,
                    onEdit = onEdit,
                    onDuplicate = onDuplicate,
                    onToggleNotify = onToggleNotify,
                    onDelete = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "${goal.progress} / ${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${goal.percent}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(fraction = goal.fraction, color = accent)

            Spacer(Modifier.height(14.dp))

            when (goal.type) {
                GoalType.DAILY -> DailyAction(goal = goal, onCheckIn = onCheckIn)
                GoalType.COUNT -> CountAction(
                    goal = goal,
                    onChangeCount = onChangeCount,
                    onOpenInput = { showCountDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = goal.title,
            onConfirm = { showDeleteDialog = false; onDelete() },
            onDismiss = { showDeleteDialog = false }
        )
    }
    if (showCountDialog) {
        CountInputDialog(
            current = goal.progress,
            target = goal.target,
            unit = goal.unit,
            onConfirm = { value -> showCountDialog = false; onSetCount(value) },
            onDismiss = { showCountDialog = false }
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(600), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.15f),
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@Composable
private fun DailyAction(goal: Goal, onCheckIn: () -> Unit) {
    val canCheckIn = goal.canCheckInNow()
    FilledTonalButton(
        onClick = onCheckIn,
        enabled = canCheckIn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (canCheckIn) "Отметить сегодня  ✓" else "Готово на сегодня  🎉")
    }
}

@Composable
private fun CountAction(goal: Goal, onChangeCount: (Int) -> Unit, onOpenInput: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton("−", enabled = goal.progress > 0) { onChangeCount(-1) }
        StepButton("+", enabled = goal.progress < goal.target) { onChangeCount(1) }
        OutlinedButton(onClick = onOpenInput, modifier = Modifier.weight(1f)) {
            Text("Ввести значение")
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GoalMenu(
    notify: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleNotify: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Text("⋮", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("✏️  Изменить") },
                onClick = { expanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("📑  Дублировать") },
                onClick = { expanded = false; onDuplicate() }
            )
            DropdownMenuItem(
                text = { Text(if (notify) "🔕  Выключить напоминания" else "🔔  Включить напоминания") },
                onClick = { expanded = false; onToggleNotify() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("🗑️  Удалить", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun CountInputDialog(
    current: Int,
    target: Int,
    unit: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Текущее значение") },
        text = {
            Column {
                Text(
                    "Укажите, сколько уже достигнуто (цель: $target${if (unit.isNotBlank()) " $unit" else ""}).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(7) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { if (unit.isNotBlank()) Text(unit) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: current) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить цель?") },
        text = { Text("Цель «$title» будет удалена безвозвратно.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun Goal.subtitle(): String = when (type) {
    GoalType.DAILY -> if (periodDays <= 1) "Ежедневная привычка" else "Каждые $periodDays дн."
    GoalType.COUNT -> "Цель на количество"
}
