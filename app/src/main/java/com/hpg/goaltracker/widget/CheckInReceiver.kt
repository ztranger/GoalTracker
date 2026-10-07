package com.hpg.goaltracker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.hpg.goaltracker.data.GoalRepository

/** Performs a daily check-in triggered from the home-screen widget or a notification action. */
class CheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        GoalRepository.get(context).checkInDaily(goalId)

        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            runCatching { NotificationManagerCompat.from(context).cancel(notifId) }
        }
        GoalWidgetProvider.refresh(context)
    }

    companion object {
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}
