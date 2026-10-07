package com.hpg.goaltracker.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.ui.theme.Dimens
import com.hpg.goaltracker.ui.theme.Gold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GloryScreen(
    goals: List<Goal>,
    onDuplicate: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    onOpenDetail: (Goal) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) {
        EmptyState(
            emoji = "🏆",
            title = "Зал славы пуст",
            subtitle = "Достигайте целей — и они будут появляться здесь как ваши награды.",
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "🏆 Достигнуто целей: ${goals.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(goals, key = { it.id }) { goal ->
            GloryCard(
                goal = goal,
                onDuplicate = { onDuplicate(goal) },
                onDelete = { onDelete(goal) },
                onOpenDetail = { onOpenDetail(goal) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun GloryCard(
    goal: Goal,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDelete by remember { mutableStateOf(false) }
    Card(
        onClick = onOpenDetail,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    GoalAvatar(goal)
                    Text("🏆", fontSize = 20.sp)
                }
                GapW(12)
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        "Достигнута ${formatDate(goal.completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Результат: ${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDelete = true }) {
                    Text("🗑️ Удалить", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDuplicate) {
                    Text("📑 Повторить цель")
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = goal.title,
            onConfirm = { showDelete = false; onDelete() },
            onDismiss = { showDelete = false }
        )
    }
}

private fun formatDate(millis: Long?): String {
    if (millis == null) return "—"
    val fmt = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
    return fmt.format(Date(millis))
}
