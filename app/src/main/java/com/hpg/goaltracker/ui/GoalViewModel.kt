package com.hpg.goaltracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalRepository
import com.hpg.goaltracker.data.GoalType
import kotlinx.coroutines.flow.StateFlow

class GoalViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GoalRepository.get(app)
    val goals: StateFlow<List<Goal>> = repo.goals

    /** Set when a goal is just completed so the UI can show the celebration. */
    var celebration by mutableStateOf<Goal?>(null)
        private set

    fun addOrUpdate(goal: Goal) = repo.upsert(goal)

    fun delete(goal: Goal) = repo.delete(goal.id)

    fun toggleNotify(goal: Goal) = repo.upsert(goal.copy(notify = !goal.notify))

    /** DAILY check-in: +1 progress, once per period. */
    fun checkIn(goal: Goal) {
        if (!goal.canCheckInNow()) return
        val newProgress = (goal.progress + 1).coerceAtMost(goal.target)
        applyProgress(goal, newProgress, markCheckIn = true)
    }

    /** COUNT goal: nudge the current value by [delta]. */
    fun changeCount(goal: Goal, delta: Int) {
        val newProgress = (goal.progress + delta).coerceIn(0, goal.target)
        applyProgress(goal, newProgress, markCheckIn = false)
    }

    /** COUNT goal: set the current value directly. */
    fun setCount(goal: Goal, value: Int) {
        val newProgress = value.coerceIn(0, goal.target)
        applyProgress(goal, newProgress, markCheckIn = false)
    }

    private fun applyProgress(goal: Goal, newProgress: Int, markCheckIn: Boolean) {
        val now = System.currentTimeMillis()
        val justCompleted = newProgress >= goal.target && goal.completedAt == null
        val updated = goal.copy(
            progress = newProgress,
            lastCheckIn = if (markCheckIn) now else goal.lastCheckIn,
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

    /** A fresh, unsaved copy of a goal for the editor ("duplicate"). */
    fun duplicateTemplate(goal: Goal): Goal = goal.copy(
        id = java.util.UUID.randomUUID().toString(),
        progress = 0,
        completedAt = null,
        lastCheckIn = null,
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
