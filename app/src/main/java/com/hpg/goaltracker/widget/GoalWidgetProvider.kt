package com.hpg.goaltracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.hpg.goaltracker.MainActivity
import com.hpg.goaltracker.R

class GoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    companion object {

        private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            val serviceIntent = Intent(context, GoalWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                // Unique per widget id so each instance gets its own adapter.
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_goal_list)
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Header opens the app.
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_header,
                PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Template for list-item taps → check-in broadcast (fill-in carries the goal id).
            val checkIntent = Intent(context, CheckInReceiver::class.java)
            val template = PendingIntent.getBroadcast(
                context, 0, checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, template)
            return views
        }

        /** Rebuilds and refreshes all widget instances (call after data changes). */
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, GoalWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> mgr.updateAppWidget(id, buildViews(context, id)) }
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }
}
