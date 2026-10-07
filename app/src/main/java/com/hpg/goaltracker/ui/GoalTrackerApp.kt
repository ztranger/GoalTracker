@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hpg.goaltracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hpg.goaltracker.data.AppSettings
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.ui.theme.GoalAccentColors

private data class EditSession(val goal: Goal, val isNew: Boolean)

@Composable
fun GoalTrackerApp(vm: GoalViewModel, onThemeChanged: (Int, Boolean) -> Unit = { _, _ -> }) {
    val goals by vm.goals.collectAsState()
    val active = goals.filter { !it.isCompleted }.sortedByDescending { it.createdAt }
    val glory = goals.filter { it.isCompleted }.sortedByDescending { it.completedAt }

    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<EditSession?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    var isOnboarding by remember { mutableStateOf(false) }
    val defaultAccent = remember { GoalAccentColors.first().toArgbInt() }
    val startCreate = { editing = EditSession(GoalViewModel.newGoalTemplate(defaultAccent), isNew = true) }

    LaunchedEffect(Unit) {
        if (!AppSettings.onboardingDone(context) && goals.isEmpty()) {
            isOnboarding = true
            showTemplates = true
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun checkInWithUndo(goal: Goal) {
        if (!goal.canCheckInNow()) return
        vm.checkIn(goal)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Отмечено ✓",
                actionLabel = "Отменить",
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(goal)
        }
    }

    fun deleteWithUndo(goal: Goal) {
        vm.delete(goal)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "«${goal.title}» удалена",
                actionLabel = "Отменить",
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) vm.addOrUpdate(goal)
        }
    }

    val session = editing
    val detailGoal = detailId?.let { id -> goals.find { it.id == id } }
    if (session != null) {
        EditGoalScreen(
            initial = session.goal,
            isNew = session.isNew,
            onSave = { vm.addOrUpdate(it); editing = null },
            onCancel = { editing = null }
        )
    } else if (detailGoal != null) {
        GoalDetailScreen(
            goal = detailGoal,
            availableFreezes = vm.availableFreezes(),
            onBack = { detailId = null },
            onCheckIn = vm::checkIn,
            onChangeCount = vm::changeCount,
            onSetCount = vm::setCount,
            onEdit = { editing = EditSession(it, isNew = false) },
            onDuplicate = { editing = EditSession(vm.duplicateTemplate(it), isNew = true) },
            onToggleNotify = vm::toggleNotify,
            onFreeze = { g, day -> vm.freezeDay(g, day) },
            onSaveNote = { g, day, text, mood -> vm.setNote(g, day, text, mood) },
            onDelete = { deleteWithUndo(it); detailId = null }
        )
    } else if (showTemplates) {
        TemplatesScreen(
            isOnboarding = isOnboarding,
            onPick = {
                editing = EditSession(it.toGoal(), isNew = true)
                showTemplates = false
                AppSettings.setOnboardingDone(context)
            },
            onBlank = {
                startCreate()
                showTemplates = false
                AppSettings.setOnboardingDone(context)
            },
            onClose = {
                showTemplates = false
                AppSettings.setOnboardingDone(context)
            }
        )
    } else if (showSettings) {
        SettingsScreen(onBack = { showSettings = false }, onThemeChanged = onThemeChanged)
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (tab) {
                                0 -> "Мои цели"
                                1 -> "Статистика"
                                else -> "Зал славы"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Text(
                                "⚙️",
                                fontSize = 20.sp,
                                modifier = Modifier.clearAndSetSemantics { contentDescription = "Настройки" }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    val navSemantics = Modifier.clearAndSetSemantics { }
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Text("🎯", fontSize = 20.sp, modifier = navSemantics) },
                        label = { Text("Цели") }
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Text("📊", fontSize = 20.sp, modifier = navSemantics) },
                        label = { Text("Статистика") }
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Text("🏆", fontSize = 20.sp, modifier = navSemantics) },
                        label = { Text("Слава") }
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0) {
                    FloatingActionButton(
                        onClick = startCreate,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "＋",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "Создать цель" }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> GoalsScreen(
                    goals = active,
                    onCheckIn = ::checkInWithUndo,
                    onChangeCount = vm::changeCount,
                    onSetCount = vm::setCount,
                    onEdit = { editing = EditSession(it, isNew = false) },
                    onDuplicate = { editing = EditSession(vm.duplicateTemplate(it), isNew = true) },
                    onDelete = ::deleteWithUndo,
                    onToggleNotify = vm::toggleNotify,
                    onCreate = startCreate,
                    onTemplates = { isOnboarding = false; showTemplates = true },
                    onOpenDetail = { detailId = it.id },
                    modifier = Modifier.padding(padding)
                )

                1 -> StatsScreen(
                    goals = goals,
                    modifier = Modifier.padding(padding)
                )

                else -> GloryScreen(
                    goals = glory,
                    onDuplicate = { editing = EditSession(vm.duplicateTemplate(it), isNew = true) },
                    onDelete = ::deleteWithUndo,
                    onOpenDetail = { detailId = it.id },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    vm.celebration?.let { completed ->
        CelebrationOverlay(goal = completed, onDismiss = vm::dismissCelebration)
    }
}
