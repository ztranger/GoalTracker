package com.hpg.goaltracker.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.ui.theme.GoalAccentColors
import kotlin.math.sin
import kotlin.random.Random

private data class Confetto(
    val x: Float,
    val baseY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val sway: Float,
    val phase: Float,
    val rotateSpeed: Float,
)

@Composable
fun CelebrationOverlay(goal: Goal, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            ConfettiLayer()

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.6f,
                animationSpec = tween(450, easing = FastOutSlowInEasing),
                label = "cardScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(300),
                label = "cardAlpha"
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(32.dp)
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale; this.alpha = alpha
                    }
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulsingTrophy()
                    Text(
                        "Цель достигнута!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${goal.emoji}  ${goal.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Поздравляем! Эта цель отправляется в Зал славы 🏆",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text("Забрать награду")
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingTrophy() {
    val transition = rememberInfiniteTransition(label = "trophy")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "trophyScale"
    )
    Text(
        "🏆",
        fontSize = 72.sp,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

@Composable
private fun ConfettiLayer() {
    val confetti = remember {
        List(90) {
            Confetto(
                x = Random.nextFloat(),
                baseY = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.9f,
                size = 6f + Random.nextFloat() * 10f,
                color = GoalAccentColors[Random.nextInt(GoalAccentColors.size)],
                sway = 10f + Random.nextFloat() * 40f,
                phase = Random.nextFloat() * 6.28f,
                rotateSpeed = (if (Random.nextBoolean()) 1f else -1f) * (1f + Random.nextFloat() * 3f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiT"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        confetti.forEach { c ->
            val yProgress = (c.baseY + t * c.speed) % 1.2f
            val y = yProgress * h - c.size
            val x = c.x * w + sin((t * 6.28f * c.rotateSpeed) + c.phase) * c.sway
            val half = c.size / 2f
            rotate(degrees = (t * 360f * c.rotateSpeed) % 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = c.color,
                    topLeft = Offset(x - half, y - half),
                    size = androidx.compose.ui.geometry.Size(c.size, c.size * 0.6f)
                )
            }
        }
    }
}
