@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.ui.theme.GoalAccentColors

private data class EditSession(val goal: Goal, val isNew: Boolean)

@Composable
fun GoalTrackerApp(vm: GoalViewModel) {
    val goals by vm.goals.collectAsState()
    val active = goals.filter { !it.isCompleted }.sortedByDescending { it.createdAt }
    val glory = goals.filter { it.isCompleted }.sortedByDescending { it.completedAt }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<EditSession?>(null) }
    val defaultAccent = remember { GoalAccentColors.first().toArgbInt() }

    val session = editing
    if (session != null) {
        EditGoalScreen(
            initial = session.goal,
            isNew = session.isNew,
            onSave = { vm.addOrUpdate(it); editing = null },
            onCancel = { editing = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (tab == 0) "Мои цели" else "Зал славы",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Text("🎯", fontSize = 20.sp) },
                        label = { Text("Цели") }
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Text("🏆", fontSize = 20.sp) },
                        label = { Text("Слава") }
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0) {
                    FloatingActionButton(
                        onClick = {
                            editing = EditSession(
                                GoalViewModel.newGoalTemplate(defaultAccent),
                                isNew = true
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("＋", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> GoalsScreen(
                    goals = active,
                    onCheckIn = vm::checkIn,
                    onChangeCount = vm::changeCount,
                    onSetCount = vm::setCount,
                    onEdit = { editing = EditSession(it, isNew = false) },
                    onDuplicate = { editing = EditSession(vm.duplicateTemplate(it), isNew = true) },
                    onDelete = vm::delete,
                    onToggleNotify = vm::toggleNotify,
                    modifier = Modifier.padding(padding)
                )

                else -> GloryScreen(
                    goals = glory,
                    onDuplicate = { editing = EditSession(vm.duplicateTemplate(it), isNew = true) },
                    onDelete = vm::delete,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    vm.celebration?.let { completed ->
        CelebrationOverlay(goal = completed, onDismiss = vm::dismissCelebration)
    }
}
