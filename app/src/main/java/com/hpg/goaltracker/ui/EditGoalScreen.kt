@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditGoalScreen(
    initial: Goal,
    isNew: Boolean,
    onSave: (Goal) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initial.title) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    var colorArgb by remember { mutableStateOf(initial.colorArgb) }
    var type by remember { mutableStateOf(initial.type) }
    var targetText by remember { mutableStateOf(initial.target.toString()) }
    var periodText by remember { mutableStateOf(initial.periodDays.toString()) }
    var unit by remember { mutableStateOf(initial.unit) }
    var notify by remember { mutableStateOf(initial.notify) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { ImageStore.saveFromUri(context, uri) }
                if (path != null) imagePath = path
            }
        }
    }

    val preview = initial.copy(
        title = title.ifBlank { "Моя цель" },
        emoji = emoji,
        imagePath = imagePath,
        colorArgb = colorArgb,
        type = type
    )

    val targetValue = targetText.toIntOrNull()?.coerceAtLeast(1) ?: 0
    val isValid = title.isNotBlank() && targetValue >= 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новая цель" else "Изменить цель") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Live preview
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GoalAvatar(preview, size = 88)
            }

            FieldLabel("Название цели")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                placeholder = { Text("Например: Делать зарядку") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel("Иконка")
            EmojiPicker(selected = emoji, onSelect = { emoji = it })

            FieldLabel("Фото (необязательно)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text(if (imagePath == null) "📷 Выбрать фото" else "🔁 Заменить фото")
                }
                if (imagePath != null) {
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { imagePath = null }) {
                        Text("Убрать", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            FieldLabel("Цвет")
            ColorPicker(selectedArgb = colorArgb, onSelect = { colorArgb = it })

            FieldLabel("Тип цели")
            TypeSelector(selected = type, onSelect = { type = it })

            // Target
            FieldLabel(if (type == GoalType.DAILY) "Сколько раз всего выполнить" else "Целевое значение")
            NumberField(
                value = targetText,
                onValueChange = { targetText = it },
                placeholder = if (type == GoalType.DAILY) "30" else "10"
            )

            if (type == GoalType.DAILY) {
                FieldLabel("Как часто (каждые N дней)")
                NumberField(
                    value = periodText,
                    onValueChange = { periodText = it },
                    placeholder = "1"
                )
            }

            FieldLabel("Единица измерения (необязательно)")
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it.take(16) },
                placeholder = { Text("раз, км, страниц…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Напоминания", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Мотивирующие уведомления о прогрессе",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = notify, onCheckedChange = { notify = it })
            }

            Button(
                onClick = {
                    val period = periodText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            emoji = emoji,
                            imagePath = imagePath,
                            colorArgb = colorArgb,
                            type = type,
                            target = targetValue,
                            periodDays = if (type == GoalType.DAILY) period else 1,
                            unit = unit.trim(),
                            notify = notify,
                            // keep progress within new target bounds
                            progress = initial.progress.coerceAtMost(targetValue)
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isNew) "Создать цель" else "Сохранить", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(7)) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(140.dp)
    )
}

@Composable
private fun TypeSelector(selected: GoalType, onSelect: (GoalType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TypeOption(
            emoji = "🔁",
            title = "Ежедневная",
            desc = "Привычка по расписанию",
            isSelected = selected == GoalType.DAILY,
            onClick = { onSelect(GoalType.DAILY) },
            modifier = Modifier.weight(1f)
        )
        TypeOption(
            emoji = "📈",
            title = "На количество",
            desc = "Достичь числа",
            isSelected = selected == GoalType.COUNT,
            onClick = { onSelect(GoalType.COUNT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TypeOption(
    emoji: String,
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
