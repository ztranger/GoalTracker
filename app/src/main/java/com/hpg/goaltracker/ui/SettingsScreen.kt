@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.AppSettings
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalRepository
import com.hpg.goaltracker.data.ReminderSettings
import com.hpg.goaltracker.data.WeekMask
import com.hpg.goaltracker.notify.Reminders
import com.hpg.goaltracker.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PickerTarget { DAILY, STREAK }

@Composable
fun SettingsScreen(onBack: () -> Unit, onThemeChanged: (Int, Boolean) -> Unit = { _, _ -> }) {
    val context = LocalContext.current

    var themeMode by remember { mutableIntStateOf(AppSettings.themeMode(context)) }
    var dynamicColor by remember { mutableStateOf(AppSettings.dynamicColor(context)) }

    var dailyEnabled by remember { mutableStateOf(ReminderSettings.dailyEnabled(context)) }
    var dailyHour by remember { mutableIntStateOf(ReminderSettings.dailyHour(context)) }
    var dailyMinute by remember { mutableIntStateOf(ReminderSettings.dailyMinute(context)) }
    var dailyDays by remember { mutableIntStateOf(ReminderSettings.dailyDays(context)) }

    var streakEnabled by remember { mutableStateOf(ReminderSettings.streakEnabled(context)) }
    var streakHour by remember { mutableIntStateOf(ReminderSettings.streakHour(context)) }
    var streakMinute by remember { mutableIntStateOf(ReminderSettings.streakMinute(context)) }
    var streakDays by remember { mutableIntStateOf(ReminderSettings.streakDays(context)) }

    var picker by remember { mutableStateOf<PickerTarget?>(null) }

    val scope = rememberCoroutineScope()
    val repo = remember { GoalRepository.get(context) }
    var pendingImport by remember { mutableStateOf<List<Goal>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(repo.exportJson().toByteArray())
                    }
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) "Экспортировано ✓" else "Не удалось сохранить файл",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val parsed = text?.let { runCatching { repo.parseJson(it) }.getOrNull() }
            if (parsed != null) {
                pendingImport = parsed
            } else {
                Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveDaily() {
        ReminderSettings.setDaily(context, dailyEnabled, dailyHour, dailyMinute, dailyDays)
        Reminders.scheduleMotivation(context)
    }

    fun saveStreak() {
        ReminderSettings.setStreak(context, streakEnabled, streakHour, streakMinute, streakDays)
        Reminders.scheduleStreak(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "Назад" }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Оформление",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Тема", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("Система", themeMode == AppSettings.THEME_SYSTEM) {
                            themeMode = AppSettings.THEME_SYSTEM; onThemeChanged(themeMode, dynamicColor)
                        }
                        ThemeChip("Светлая", themeMode == AppSettings.THEME_LIGHT) {
                            themeMode = AppSettings.THEME_LIGHT; onThemeChanged(themeMode, dynamicColor)
                        }
                        ThemeChip("Тёмная", themeMode == AppSettings.THEME_DARK) {
                            themeMode = AppSettings.THEME_DARK; onThemeChanged(themeMode, dynamicColor)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Material You", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Цвета из обоев (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = dynamicColor, onCheckedChange = {
                            dynamicColor = it; onThemeChanged(themeMode, dynamicColor)
                        })
                    }
                }
            }

            Text(
                "Напоминания",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ReminderCard(
                emoji = "📣",
                title = "Ежедневное напоминание",
                description = "Лёгкий толчок к прогрессу каждый день",
                enabled = dailyEnabled,
                onEnabledChange = { dailyEnabled = it; saveDaily() },
                hour = dailyHour,
                minute = dailyMinute,
                timeLabelText = "Время напоминания",
                onTimeClick = { picker = PickerTarget.DAILY },
                days = dailyDays,
                onToggleDay = { dow -> dailyDays = WeekMask.toggle(dailyDays, dow); saveDaily() }
            )

            ReminderCard(
                emoji = "🔥",
                title = "Серия под угрозой",
                description = "Вечерний «последний звонок», если серия вот-вот прервётся",
                enabled = streakEnabled,
                onEnabledChange = { streakEnabled = it; saveStreak() },
                hour = streakHour,
                minute = streakMinute,
                timeLabelText = "Время последнего звонка",
                onTimeClick = { picker = PickerTarget.STREAK },
                days = streakDays,
                onToggleDay = { dow -> streakDays = WeekMask.toggle(streakDays, dow); saveStreak() }
            )

            Text(
                "«Серия под угрозой» приходит только если у ежедневной цели есть активная серия и сегодня ещё нет отметки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Резервная копия",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💾", fontSize = 24.sp)
                        GapW(12)
                        Column(Modifier.weight(1f)) {
                            Text("Экспорт и импорт", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Сохраните цели в файл или восстановите из него",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch(backupFileName()) },
                            modifier = Modifier.weight(1f)
                        ) { Text("⬆ Экспорт") }
                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("⬇ Импорт") }
                    }
                }
            }
            Text(
                "Данные также включаются в авто-бэкап Android, если на устройстве включено резервное копирование.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    when (picker) {
        PickerTarget.DAILY -> TimePickerDialog(
            title = "Время напоминания",
            initialHour = dailyHour,
            initialMinute = dailyMinute,
            onConfirm = { h, m -> dailyHour = h; dailyMinute = m; picker = null; saveDaily() },
            onDismiss = { picker = null }
        )

        PickerTarget.STREAK -> TimePickerDialog(
            title = "Время последнего звонка",
            initialHour = streakHour,
            initialMinute = streakMinute,
            onConfirm = { h, m -> streakHour = h; streakMinute = m; picker = null; saveStreak() },
            onDismiss = { picker = null }
        )

        null -> Unit
    }

    pendingImport?.let { incoming ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Импорт: ${incoming.size} целей") },
            text = { Text("Заменить текущие цели импортированными или объединить их (по совпадению id)?") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        repo.mergeAll(incoming); pendingImport = null
                        Toast.makeText(context, "Импортировано: ${incoming.size}", Toast.LENGTH_SHORT).show()
                    }) { Text("Объединить") }
                    TextButton(onClick = {
                        repo.replaceAll(incoming); pendingImport = null
                        Toast.makeText(context, "Заменено: ${incoming.size}", Toast.LENGTH_SHORT).show()
                    }) { Text("Заменить") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Отмена") }
            }
        )
    }
}

private fun backupFileName(): String =
    "goaltracker-backup-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json"

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ReminderCard(
    emoji: String,
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    timeLabelText: String,
    onTimeClick: () -> Unit,
    days: Int,
    onToggleDay: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 24.sp)
                GapW(12)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.clickable { onTimeClick() } else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        timeLabelText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (enabled) {
                        Text(
                            "Нажмите, чтобы изменить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    timeLabel(hour, minute),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Дни недели",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            WeekdayChips(days = days, enabled = enabled, onToggle = onToggleDay)
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun timeLabel(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)
