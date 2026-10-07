@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.GoalTemplate
import com.hpg.goaltracker.data.GoalTemplates
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.ui.theme.Dimens

@Composable
fun TemplatesScreen(
    isOnboarding: Boolean,
    onPick: (GoalTemplate) -> Unit,
    onBlank: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOnboarding) "С чего начать?" else "Шаблоны целей", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text(
                            if (isOnboarding) "✕" else "←",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = if (isOnboarding) "Пропустить" else "Назад"
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isOnboarding) {
                item {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        Text("🎯", fontSize = 44.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Добро пожаловать!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Выберите готовую цель или создайте свою. Отмечайте прогресс, собирайте серии 🔥 и достижения 🏆.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    "Популярные привычки",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(GoalTemplates.list) { template ->
                TemplateCard(template, onClick = { onPick(template) })
            }
            item {
                OutlinedButton(
                    onClick = onBlank,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text("✏️ Создать свою цель")
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: GoalTemplate, onClick: () -> Unit) {
    val accent = Color(template.colorArgb)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(template.emoji, fontSize = 22.sp)
            }
            GapW(12)
            Column(Modifier.weight(1f)) {
                Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    templateSubtitle(template),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("＋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

private fun templateSubtitle(t: GoalTemplate): String = when (t.type) {
    GoalType.DAILY -> "${t.category} · привычка, ${t.target} раз"
    GoalType.COUNT -> "${t.category} · цель: ${t.target}${if (t.unit.isNotBlank()) " ${t.unit}" else ""}"
}
