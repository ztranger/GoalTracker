@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.WeekMask
import com.hpg.goaltracker.data.bestStreak
import com.hpg.goaltracker.data.currentStreak
import com.hpg.goaltracker.data.deadlineBadge
import com.hpg.goaltracker.data.startOfDay
import com.hpg.goaltracker.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GoalDetailScreen(
    goal: Goal,
    availableFreezes: Int,
    onBack: () -> Unit,
    onCheckIn: (Goal) -> Unit,
    onChangeCount: (Goal, Int) -> Unit,
    onSetCount: (Goal, Int) -> Unit,
    onEdit: (Goal) -> Unit,
    onDuplicate: (Goal) -> Unit,
    onToggleNotify: (Goal) -> Unit,
    onFreeze: (Goal, Long) -> Boolean,
    onSaveNote: (Goal, Long, String, String) -> Unit,
    onDelete: (Goal) -> Unit,
) {
    val accent = goal.accentColor()
    var showDelete by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal.title, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "Назад" }
                        )
                    }
                },
                actions = {
                    DetailMenu(
                        notify = goal.notify,
                        completed = goal.isCompleted,
                        onEdit = { onEdit(goal) },
                        onDuplicate = { onDuplicate(goal) },
                        onToggleNotify = { onToggleNotify(goal) },
                        onDelete = { showDelete = true }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoalAvatar(goal, size = 96)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (goal.type) {
                        GoalType.DAILY -> scheduleLabel(goal.activeDays)
                        GoalType.COUNT -> "Цель на количество"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress
            DetailCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "${goal.percent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent
                    )
                    Text(
                        "${goal.progress} / ${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                val animated by animateFloatAsState(goal.fraction, tween(600), label = "detailProgress")
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )

                if (goal.type == GoalType.DAILY) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoPill("🔥 Серия ${goal.currentStreak()}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        InfoPill("🏅 Рекорд ${goal.bestStreak()}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // Action (active goals only)
            if (!goal.isCompleted) {
                when (goal.type) {
                    GoalType.DAILY -> {
                        val scheduledToday = goal.isScheduledToday()
                        val canCheckIn = goal.canCheckInNow()
                        FilledTonalButton(
                            onClick = { onCheckIn(goal) },
                            enabled = canCheckIn,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                when {
                                    !scheduledToday -> "Сегодня отдых  😌"
                                    canCheckIn -> "Отметить сегодня  ✓"
                                    else -> "Готово на сегодня  🎉"
                                }
                            )
                        }
                    }
                    GoalType.COUNT -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailStep("−", goal.progress > 0) { onChangeCount(goal, -1) }
                            Text(
                                "${goal.progress}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = accent,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            DetailStep("+", goal.progress < goal.target) { onChangeCount(goal, 1) }
                        }
                    }
                }
            } else {
                DetailCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 28.sp)
                        GapW(12)
                        Column(Modifier.weight(1f)) {
                            Text("Цель достигнута", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Достигнута ${formatDay(goal.completedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onDuplicate(goal) }) { Text("📑 Повторить цель") }
                }
            }

            // Calendar
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Отметки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (goal.type == GoalType.DAILY && !goal.isCompleted) {
                    InfoPill("❄ $availableFreezes", FrozenColor.copy(alpha = 0.18f), FrozenColor)
                }
            }
            MonthCalendar(goal = goal, accent = accent, onDayClick = { selectedDay = it })
            if (goal.type == GoalType.DAILY && !goal.isCompleted) {
                Text(
                    "Нажмите на день: отмеченный — добавить заметку; пропущенный — заморозить серию (${availableFreezes} осталось в этом месяце).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Notes
            if (goal.notes.isNotEmpty()) {
                Text(
                    "Заметки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                DetailCard {
                    goal.notes.sortedByDescending { it.dayKey }.forEachIndexed { i, note ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row {
                            if (note.mood.isNotBlank()) {
                                Text(note.mood, fontSize = 20.sp)
                                GapW(10)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    formatDay(note.dayKey),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (note.text.isNotBlank()) {
                                    Text(note.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Facts
            DetailCard {
                FactRow("Всего отметок", goal.checkIns.size.toString())
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                FactRow("Создана", formatDay(goal.createdAt))
                if (goal.category.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    FactRow("Категория", goal.category)
                }
                goal.deadline?.let { d ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    val badge = goal.deadlineBadge()
                    FactRow("Срок", formatDay(d) + if (badge != null) "  ·  $badge" else "")
                }
                if (goal.type == GoalType.DAILY) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    FactRow("Выполнено дней", "${goal.progress} из ${goal.target}")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = goal.title,
            onConfirm = { showDelete = false; onDelete(goal) },
            onDismiss = { showDelete = false }
        )
    }

    selectedDay?.let { day ->
        DayDialog(
            goal = goal,
            dayKey = day,
            availableFreezes = availableFreezes,
            onFreeze = { onFreeze(goal, day) },
            onSaveNote = { text, mood -> onSaveNote(goal, day, text, mood) },
            onDismiss = { selectedDay = null }
        )
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun InfoPill(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailStep(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(52.dp)
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailMenu(
    notify: Boolean,
    completed: Boolean,
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
            if (!completed) {
                DropdownMenuItem(text = { Text("✏️  Изменить") }, onClick = { expanded = false; onEdit() })
                DropdownMenuItem(
                    text = { Text(if (notify) "🔕  Выключить напоминания" else "🔔  Включить напоминания") },
                    onClick = { expanded = false; onToggleNotify() }
                )
            }
            DropdownMenuItem(text = { Text("📑  Дублировать") }, onClick = { expanded = false; onDuplicate() })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("🗑️  Удалить", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun MonthCalendar(goal: Goal, accent: Color, onDayClick: (Long) -> Unit) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val checkDays = remember(goal.checkIns) { goal.checkIns.map { startOfDay(it) }.toHashSet() }
    val frozenDays = remember(goal.frozenDays) { goal.frozenDays.map { startOfDay(it) }.toHashSet() }
    val todayKey = remember { startOfDay(System.currentTimeMillis()) }

    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = monthCal.get(Calendar.DAY_OF_WEEK)
    val leadingBlanks = (firstDow - Calendar.MONDAY + 7) % 7
    val monthLabel = remember(monthOffset) {
        SimpleDateFormat("LLLL yyyy", RU).format(monthCal.time)
            .replaceFirstChar { it.titlecase(RU) }
    }

    DetailCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Предыдущий месяц" })
            }
            Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { if (monthOffset < 0) monthOffset++ }, enabled = monthOffset < 0) {
                Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Следующий месяц" })
            }
        }

        Row(Modifier.fillMaxWidth()) {
            WeekOrder.forEach { (_, label) ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - leadingBlanks + 1
                    if (dayNum in 1..daysInMonth) {
                        val dayCal = (monthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNum) }
                        val key = dayCal.timeInMillis
                        val dow = dayCal.get(Calendar.DAY_OF_WEEK)
                        val isFuture = key > todayKey
                        DayCell(
                            day = dayNum,
                            hasCheck = checkDays.contains(key),
                            isToday = key == todayKey,
                            isFrozen = frozenDays.contains(key),
                            isMissed = goal.type == GoalType.DAILY && key < todayKey &&
                                WeekMask.isSelected(goal.activeDays, dow) &&
                                !checkDays.contains(key) && !frozenDays.contains(key),
                            isFuture = isFuture,
                            accent = accent,
                            onClick = if (!isFuture) ({ onDayClick(key) }) else null,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(Modifier.weight(1f).height(40.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(accent, "отмечено")
            LegendDot(accent.copy(alpha = 0.14f), "пропущено")
            LegendDot(FrozenColor, "заморожено")
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hasCheck: Boolean,
    isToday: Boolean,
    isFrozen: Boolean,
    isMissed: Boolean,
    isFuture: Boolean,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(40.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val bg = when {
            hasCheck -> accent
            isFrozen -> FrozenColor.copy(alpha = 0.30f)
            isMissed -> accent.copy(alpha = 0.14f)
            else -> Color.Transparent
        }
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bg)
                .then(if (isToday && !hasCheck) Modifier.border(2.dp, accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isFrozen) "❄" else day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasCheck || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    hasCheck -> Color.White
                    isFrozen -> FrozenColor
                    isToday -> accent
                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private val FrozenColor = Color(0xFF4D96FF)

@Composable
private fun DayDialog(
    goal: Goal,
    dayKey: Long,
    availableFreezes: Int,
    onFreeze: () -> Boolean,
    onSaveNote: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val key = startOfDay(dayKey)
    val checked = goal.checkIns.any { startOfDay(it) == key }
    val frozen = goal.frozenDays.any { startOfDay(it) == key }
    val dow = Calendar.getInstance().apply { timeInMillis = key }.get(Calendar.DAY_OF_WEEK)
    val scheduledMissed = goal.type == GoalType.DAILY && !checked && !frozen &&
        key < startOfDay(System.currentTimeMillis()) && WeekMask.isSelected(goal.activeDays, dow)
    val canFreeze = scheduledMissed && availableFreezes > 0

    val existing = goal.notes.firstOrNull { startOfDay(it.dayKey) == key }
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var mood by remember { mutableStateOf(existing?.mood ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDay(key)) },
        text = {
            Column {
                when {
                    checked -> {
                        Text("Как прошло?", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        MoodRow(mood) { mood = it }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it.take(160) },
                            placeholder = { Text("Заметка (необязательно)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    frozen -> Text("❄ Этот день заморожен — серия сохранена.")
                    scheduledMissed && availableFreezes > 0 ->
                        Text("Пропущенный день. Заморозить серию, чтобы не потерять прогресс? Осталось заморозок в этом месяце: $availableFreezes.")
                    scheduledMissed ->
                        Text("Пропущенный день. Заморозки на этот месяц закончились.")
                    else -> Text("В этот день отметок не было.")
                }
            }
        },
        confirmButton = {
            when {
                checked -> TextButton(onClick = { onSaveNote(text, mood); onDismiss() }) { Text("Сохранить") }
                canFreeze -> TextButton(onClick = { onFreeze(); onDismiss() }) { Text("❄ Заморозить") }
                else -> TextButton(onClick = onDismiss) { Text("Ок") }
            }
        },
        dismissButton = {
            if (checked || canFreeze) TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private val MOODS = listOf("😞", "😐", "🙂", "😄", "🔥")

@Composable
private fun MoodRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MOODS.forEach { m ->
            val isSel = m == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(if (isSel) "" else m) },
                contentAlignment = Alignment.Center
            ) {
                Text(m, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        GapW(4)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val RU: Locale = Locale.forLanguageTag("ru")

private fun formatDay(millis: Long?): String {
    if (millis == null) return "—"
    return SimpleDateFormat("d MMMM yyyy", RU).format(Date(millis))
}
