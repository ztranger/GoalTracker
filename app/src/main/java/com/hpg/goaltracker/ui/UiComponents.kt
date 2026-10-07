package com.hpg.goaltracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.ImageStore
import com.hpg.goaltracker.data.WeekMask
import com.hpg.goaltracker.ui.theme.GoalAccentColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image as FoundationImage

fun Goal.accentColor(): Color = Color(colorArgb)

/** Avatar wrapped in an animated circular progress ring (echoes the app icon). */
@Composable
fun ProgressAvatar(goal: Goal, fraction: Float, size: Int = 56) {
    val accent = goal.accentColor()
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "ring"
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val strokePx = 4.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = accent.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f, sweepAngle = animated * 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        GoalAvatar(goal, size = size - 14)
    }
}

/** Circular goal avatar: photo if present, otherwise an emoji on the accent color. */
@Composable
fun GoalAvatar(goal: Goal, size: Int = 56) {
    val accent = goal.accentColor()
    Box(
        modifier = Modifier
            .clearAndSetSemantics { } // decorative: the title is announced instead
            .size(size.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        val path = goal.imagePath
        if (path != null) {
            val bitmap by rememberGoalImage(path)
            val bmp = bitmap
            if (bmp != null) {
                FoundationImage(
                    bitmap = bmp,
                    contentDescription = goal.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size.dp).clip(CircleShape)
                )
            } else {
                Text(goal.emoji, fontSize = (size * 0.45).sp)
            }
        } else {
            Text(goal.emoji, fontSize = (size * 0.45).sp)
        }
    }
}

@Composable
fun rememberGoalImage(path: String) = produceState<ImageBitmap?>(initialValue = null, path) {
    value = withContext(Dispatchers.IO) {
        ImageStore.loadBitmap(path)?.asImageBitmap()
    }
}

val GoalEmojis = listOf(
    "🎯", "💪", "🏃", "🏋️", "🧘", "📚", "✍️", "💻", "🎸", "🎨",
    "🧠", "💧", "🥗", "😴", "🚭", "🏊", "🚴", "⚽", "🏀", "🎹",
    "🗣️", "💰", "🧹", "🌱", "🙏", "📷", "🍎", "☕", "🧩", "⭐",
)

@Composable
fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalEmojis.forEach { emoji ->
            val isSelected = emoji == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                label = "emojiBg"
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun ColorPicker(selectedArgb: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalAccentColors.forEach { color ->
            val argb = color.toArgbInt()
            val isSelected = argb == selectedArgb
            val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, tween(200), label = "swatch")
            // 48dp touch target around a 40dp visual swatch
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Streak pill with proper contrast (gold container + dark-gold text). */
@Composable
fun StreakBadge(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "🔥 $streak",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

fun Color.toArgbInt(): Int {
    val a = (alpha * 255).toInt() shl 24
    val r = (red * 255).toInt() shl 16
    val g = (green * 255).toInt() shl 8
    val b = (blue * 255).toInt()
    return a or r or g or b
}

@Composable
fun GapW(width: Int) = Spacer(Modifier.width(width.dp))

val WeekOrder = listOf(
    Calendar.MONDAY to "Пн",
    Calendar.TUESDAY to "Вт",
    Calendar.WEDNESDAY to "Ср",
    Calendar.THURSDAY to "Чт",
    Calendar.FRIDAY to "Пт",
    Calendar.SATURDAY to "Сб",
    Calendar.SUNDAY to "Вс",
)

/** Row of toggleable weekday chips (Пн..Вс), driven by a [WeekMask] bitmask. */
@Composable
fun WeekdayChips(days: Int, enabled: Boolean, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WeekOrder.forEach { (dow, label) ->
            val selected = WeekMask.isSelected(days, dow)
            val bg = when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val fg = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .then(if (enabled) Modifier.clickable { onToggle(dow) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = fg,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** Human-readable schedule label for a daily goal's [WeekMask]. */
fun scheduleLabel(mask: Int): String = when (mask) {
    WeekMask.ALL_DAYS -> "Ежедневно"
    WeekMask.WEEKDAYS -> "По будням"
    WeekMask.WEEKENDS -> "По выходным"
    else -> WeekOrder.filter { WeekMask.isSelected(mask, it.first) }
        .joinToString(", ") { it.second }
        .ifBlank { "Нет дней" }
}
