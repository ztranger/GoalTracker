@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.hpg.goaltracker.data.DeadlineStatus
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.currentStreak
import com.hpg.goaltracker.data.deadlineBadge
import com.hpg.goaltracker.data.deadlineStatus
import com.hpg.goaltracker.ui.theme.Dimens

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
    onCreate: () -> Unit,
    onTemplates: () -> Unit,
    onOpenDetail: (Goal) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) {
        EmptyState(
            emoji = "🎯",
            title = "Пока нет целей",
            subtitle = "Поставьте первую цель — и начните свой путь к победам.",
            actionLabel = "Создать первую цель",
            onAction = onCreate,
            secondaryLabel = "Выбрать из шаблонов",
            onSecondary = onTemplates,
            modifier = modifier
        )
        return
    }
    var todayOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SortMode.CREATED) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    val categories = remember(goals) { goals.mapNotNull { it.category.ifBlank { null } }.distinct() }
    val displayed = remember(goals, todayOnly, sortMode, categoryFilter) {
        goals.filter { !todayOnly || it.type != GoalType.DAILY || it.isScheduledToday() }
            .filter { categoryFilter == null || it.category == categoryFilter }
            .let { sortGoals(it, sortMode) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.s16, Dimens.s12, Dimens.s16, 96.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.s12)
    ) {
        item {
            GoalsHeader(
                todayOnly = todayOnly,
                onTodayChange = { todayOnly = it },
                sortMode = sortMode,
                onSortChange = { sortMode = it },
                categories = categories,
                categoryFilter = categoryFilter,
                onCategoryChange = { categoryFilter = it }
            )
        }
        if (displayed.isEmpty()) {
            item {
                Text(
                    "Сегодня нет целей по расписанию 😌",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
        items(displayed, key = { it.id }) { goal ->
            SwipeGoalCard(
                goal = goal,
                onCheckIn = { onCheckIn(goal) },
                onChangeCount = { delta -> onChangeCount(goal, delta) },
                onSetCount = { value -> onSetCount(goal, value) },
                onEdit = { onEdit(goal) },
                onDuplicate = { onDuplicate(goal) },
                onDelete = { onDelete(goal) },
                onToggleNotify = { onToggleNotify(goal) },
                onOpenDetail = { onOpenDetail(goal) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

enum class SortMode { CREATED, PROGRESS, STREAK, NAME }

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.CREATED -> "Новые"
    SortMode.PROGRESS -> "Прогресс"
    SortMode.STREAK -> "Серия"
    SortMode.NAME -> "Название"
}

private fun sortGoals(list: List<Goal>, mode: SortMode): List<Goal> = when (mode) {
    SortMode.CREATED -> list.sortedByDescending { it.createdAt }
    SortMode.PROGRESS -> list.sortedByDescending { it.fraction }
    SortMode.STREAK -> list.sortedByDescending { if (it.type == GoalType.DAILY) it.currentStreak() else 0 }
    SortMode.NAME -> list.sortedBy { it.title.lowercase() }
}

@Composable
private fun GoalsHeader(
    todayOnly: Boolean,
    onTodayChange: (Boolean) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit,
    categories: List<String>,
    categoryFilter: String?,
    onCategoryChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !todayOnly, onClick = { onTodayChange(false) }, label = { Text("Все") })
            GapW(8)
            FilterChip(selected = todayOnly, onClick = { onTodayChange(true) }, label = { Text("Сегодня") })
            Spacer(Modifier.weight(1f))
            var open by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { open = true }) { Text("⇅ ${sortLabel(sortMode)}") }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(mode)) },
                            onClick = { onSortChange(mode); open = false }
                        )
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("Все категории") }
                )
                categories.forEach { c ->
                    FilterChip(
                        selected = categoryFilter == c,
                        onClick = { onCategoryChange(c) },
                        label = { Text(c) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeGoalCard(
    goal: Goal,
    onCheckIn: () -> Unit,
    onChangeCount: (Int) -> Unit,
    onSetCount: (Int) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleNotify: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    when (goal.type) {
                        GoalType.DAILY -> if (goal.canCheckInNow()) onCheckIn()
                        GoalType.COUNT -> if (goal.progress < goal.target) onChangeCount(1)
                    }
                    false // snap back, keep the card
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) }
    ) {
        GoalCard(
            goal = goal,
            onCheckIn = onCheckIn,
            onChangeCount = onChangeCount,
            onSetCount = onSetCount,
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onToggleNotify = onToggleNotify,
            onOpenDetail = onOpenDetail
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        else -> Color.Transparent
    }
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        else -> Alignment.CenterEnd
    }
    val label = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> "✓ Отметить"
        SwipeToDismissBoxValue.EndToStart -> "Удалить 🗑"
        else -> ""
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Dimens.CardRadius))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (label.isNotEmpty()) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
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
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = goal.accentColor()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCountDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onOpenDetail,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.05f).compositeOver(MaterialTheme.colorScheme.surface)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(Dimens.s16)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressAvatar(goal, goal.fraction)
                GapW(12)
                val streak = if (goal.type == GoalType.DAILY) goal.currentStreak() else 0
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            goal.subtitle(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        val badge = goal.deadlineBadge()
                        if (badge != null) {
                            GapW(8)
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (goal.deadlineStatus()) {
                                    DeadlineStatus.OVERDUE -> MaterialTheme.colorScheme.error
                                    DeadlineStatus.BEHIND -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    }
                }
                if (streak > 0) {
                    GapW(8)
                    StreakBadge(streak)
                }
                GoalMenu(
                    notify = goal.notify,
                    onEdit = onEdit,
                    onDuplicate = onDuplicate,
                    onToggleNotify = onToggleNotify,
                    onDelete = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(Dimens.s16))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "${goal.progress} / ${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val animatedPercent by animateIntAsState(goal.percent, tween(700), label = "pct")
                Text(
                    "$animatedPercent%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent
                )
            }

            Spacer(Modifier.height(Dimens.s16))

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
private fun DailyAction(goal: Goal, onCheckIn: () -> Unit) {
    val scheduledToday = goal.isScheduledToday()
    val canCheckIn = goal.canCheckInNow()
    val label = when {
        !scheduledToday -> "Сегодня отдых  😌"
        canCheckIn -> "Отметить сегодня  ✓"
        else -> "Готово на сегодня  🎉"
    }
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "checkInPress")

    FilledTonalButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onCheckIn()
        },
        enabled = canCheckIn,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Text(label)
    }
}

@Composable
private fun CountAction(goal: Goal, onChangeCount: (Int) -> Unit, onOpenInput: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.s8),
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
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(Dimens.MinTouch)
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
            Text(
                "⋮",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Действия с целью" }
            )
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
                Spacer(Modifier.height(Dimens.s12))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(7) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(Dimens.s16))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(Dimens.s8))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Dimens.s24))
                Button(onClick = onAction) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(Dimens.s8))
                OutlinedButton(onClick = onSecondary) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}

private fun Goal.subtitle(): String = when (type) {
    GoalType.DAILY -> scheduleLabel(activeDays)
    GoalType.COUNT -> "Цель на количество"
}
