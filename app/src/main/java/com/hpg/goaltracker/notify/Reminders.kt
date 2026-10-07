package com.hpg.goaltracker.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hpg.goaltracker.MainActivity
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalRepository
import com.hpg.goaltracker.data.GoalType
import com.hpg.goaltracker.data.ReminderSettings
import com.hpg.goaltracker.data.WeekMask
import com.hpg.goaltracker.data.currentStreak
import com.hpg.goaltracker.widget.CheckInReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Reminders {

    const val CHANNEL_ID = "goal_reminders"

    private const val MOTIVATION_WORK = "daily_goal_reminder"
    private const val STREAK_WORK = "streak_risk_reminder"
    private const val MOTIVATION_ID = 2001
    private const val STREAK_ID = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о целях",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Мотивирующие напоминания о прогрессе и сериях"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /** Schedules both the (configurable) daily reminder and the streak-risk warning. */
    fun scheduleAll(context: Context) {
        ensureChannel(context)
        scheduleMotivation(context)
        scheduleStreak(context)
    }

    /** (Re)schedules the general daily reminder from the current user settings. */
    fun scheduleMotivation(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(MOTIVATION_WORK)
        if (!ReminderSettings.dailyEnabled(context)) return

        val delay = initialDelayMillis(
            ReminderSettings.dailyHour(context),
            ReminderSettings.dailyMinute(context)
        )
        wm.enqueueUniquePeriodicWork(
            MOTIVATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    /** (Re)schedules the streak-risk warning from the current user settings. */
    fun scheduleStreak(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(STREAK_WORK)
        if (!ReminderSettings.streakEnabled(context)) return

        val delay = initialDelayMillis(
            ReminderSettings.streakHour(context),
            ReminderSettings.streakMinute(context)
        )
        wm.enqueueUniquePeriodicWork(
            STREAK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StreakRiskWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    private fun initialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now.timeInMillis) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    fun showReminder(context: Context, goal: Goal) {
        val (title, text) = MotivationMessages.forGoal(goal)
        val actionId = if (goal.type == GoalType.DAILY && goal.canCheckInNow()) goal.id else null
        post(context, MOTIVATION_ID, title, text, actionId)
    }

    fun showStreakRisk(context: Context, goals: List<Goal>) {
        if (goals.isEmpty()) return
        val (title, text) = streakRiskMessage(goals)
        // One-tap check-in only makes sense when a single goal is at risk.
        val actionId = goals.singleOrNull()?.id
        post(context, STREAK_ID, title, text, actionId)
    }

    private fun streakRiskMessage(goals: List<Goal>): Pair<String, String> = if (goals.size == 1) {
        val g = goals.first()
        "🔥 Серия под угрозой!" to
            "«${g.title}»: серия ${g.currentStreak()} дн. прервётся, если сегодня не отметить. Ещё есть время!"
    } else {
        "🔥 Серии под угрозой!" to
            "Сегодня ещё не отмечены: ${goals.joinToString(", ") { "«${it.title}»" }}. " +
            "Отметь, чтобы сохранить серии!"
    }

    private fun post(context: Context, id: Int, title: String, text: String, actionGoalId: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)

        if (actionGoalId != null) {
            val checkIntent = Intent(context, CheckInReceiver::class.java).apply {
                putExtra(CheckInReceiver.EXTRA_GOAL_ID, actionGoalId)
                putExtra(CheckInReceiver.EXTRA_NOTIF_ID, id)
            }
            val checkPending = PendingIntent.getBroadcast(
                context, actionGoalId.hashCode(), checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Отметить ✓", checkPending)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (!WeekMask.isSelected(ReminderSettings.dailyDays(applicationContext), today)) {
            return Result.success()
        }

        val active = GoalRepository.get(applicationContext)
            .snapshot()
            .filter { !it.isCompleted && it.notify }
        if (active.isEmpty()) return Result.success()

        // Prefer a goal the user can act on today.
        val goal = active.firstOrNull { it.canCheckInNow() } ?: active.random()
        Reminders.showReminder(applicationContext, goal)
        return Result.success()
    }
}

/** Fires in the evening only when a live streak would break unless the user checks in today. */
class StreakRiskWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (!WeekMask.isSelected(ReminderSettings.streakDays(applicationContext), today)) {
            return Result.success()
        }

        val atRisk = GoalRepository.get(applicationContext)
            .snapshot()
            .filter {
                it.type == GoalType.DAILY &&
                    !it.isCompleted &&
                    it.notify &&
                    it.canCheckInNow() &&
                    it.currentStreak() > 0
            }
        if (atRisk.isNotEmpty()) {
            Reminders.showStreakRisk(applicationContext, atRisk)
        }
        return Result.success()
    }
}
