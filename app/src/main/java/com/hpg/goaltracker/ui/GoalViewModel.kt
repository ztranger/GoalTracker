package com.hpg.goaltracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.hpg.goaltracker.data.AppSettings
import com.hpg.goaltracker.data.DayNote
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalRepository
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.startOfDay
import kotlinx.coroutines.flow.StateFlow

class GoalViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GoalRepository.get(app)
    val goals: StateFlow<List<Goal>> = repo.goals

    /** Set when a goal is just completed so the UI can show the celebration. */
    var celebration by mutableStateOf<Goal?>(null)
        private set

    fun addOrUpdate(goal: Goal) = repo.upsert(goal)

    fun delete(goal: Goal) = repo.delete(goal.id)

    /** Restores a goal to a previous snapshot (used for undo). */
    fun restore(goal: Goal) = repo.upsert(goal)

    fun toggleNotify(goal: Goal) = repo.upsert(goal.copy(notify = !goal.notify))

    /** DAILY check-in: +1 progress, once per period (delegates to the repository). */
    fun checkIn(goal: Goal) {
        val updated = repo.checkInDaily(goal.id) ?: return
        if (goal.completedAt == null && updated.completedAt != null) celebration = updated
    }

    /** COUNT goal: nudge the current value by [delta]. */
    fun changeCount(goal: Goal, delta: Int) {
        val newProgress = (goal.progress + delta).coerceIn(0, goal.target)
        applyProgress(goal, newProgress, recordActivity = newProgress > goal.progress)
    }

    /** COUNT goal: set the current value directly. */
    fun setCount(goal: Goal, value: Int) {
        val newProgress = value.coerceIn(0, goal.target)
        applyProgress(goal, newProgress, recordActivity = newProgress > goal.progress)
    }

    private fun applyProgress(goal: Goal, newProgress: Int, recordActivity: Boolean) {
        val now = System.currentTimeMillis()
        val justCompleted = newProgress >= goal.target && goal.completedAt == null
        val updated = goal.copy(
            progress = newProgress,
            checkIns = if (recordActivity) goal.checkIns + now else goal.checkIns,
            lastCheckIn = if (recordActivity) now else goal.lastCheckIn,
            completedAt = when {
                justCompleted -> now
                newProgress < goal.target -> null // reopened if user lowered the count
                else -> goal.completedAt
            }
        )
        repo.upsert(updated)
        if (justCompleted) celebration = updated
    }

    fun dismissCelebration() {
        celebration = null
    }

    /** Streak freezes left this month. */
    fun availableFreezes(): Int = AppSettings.availableFreezes(getApplication())

    /** Protects a missed scheduled [dayKey] for [goal] with a freeze, if any remain. */
    fun freezeDay(goal: Goal, dayKey: Long): Boolean {
        val key = startOfDay(dayKey)
        if (goal.frozenDays.any { startOfDay(it) == key }) return true // already frozen
        if (!AppSettings.consumeFreeze(getApplication())) return false
        repo.upsert(goal.copy(frozenDays = goal.frozenDays + key))
        return true
    }

    /** Sets (or clears, when empty) a note/mood for [goal] on [dayKey]. */
    fun setNote(goal: Goal, dayKey: Long, text: String, mood: String) {
        val key = startOfDay(dayKey)
        val without = goal.notes.filterNot { startOfDay(it.dayKey) == key }
        val updated = if (text.isBlank() && mood.isBlank()) without
        else without + DayNote(key, text.trim(), mood)
        repo.upsert(goal.copy(notes = updated))
    }

    /** A fresh, unsaved copy of a goal for the editor ("duplicate"). */
    fun duplicateTemplate(goal: Goal): Goal = goal.copy(
        id = java.util.UUID.randomUUID().toString(),
        progress = 0,
        completedAt = null,
        lastCheckIn = null,
        checkIns = emptyList(),
        createdAt = System.currentTimeMillis()
    )

    companion object {
        fun newGoalTemplate(accentArgb: Int): Goal = Goal(
            title = "",
            emoji = "🎯",
            colorArgb = accentArgb,
            type = GoalType.DAILY,
            target = 30,
            periodDays = 1,
            progress = 0,
            unit = ""
        )
    }
}
