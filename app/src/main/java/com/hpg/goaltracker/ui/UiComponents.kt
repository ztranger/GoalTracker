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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.ImageStore
import com.hpg.goaltracker.ui.theme.GoalAccentColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image as FoundationImage

fun Goal.accentColor(): Color = Color(colorArgb)

/** Circular goal avatar: photo if present, otherwise an emoji on the accent color. */
@Composable
fun GoalAvatar(goal: Goal, size: Int = 56) {
    val accent = goal.accentColor()
    Box(
        modifier = Modifier
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalAccentColors.forEach { color ->
            val argb = color.toArgbInt()
            val isSelected = argb == selectedArgb
            val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, tween(200), label = "swatch")
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
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
