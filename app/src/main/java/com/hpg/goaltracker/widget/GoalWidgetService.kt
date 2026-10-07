package com.hpg.goaltracker.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.hpg.goaltracker.R
import com.hpg.goaltracker.data.Goal
import com.hpg.goaltracker.data.GoalRepository
import com.hpg.goaltracker.data.GoalType

class GoalWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        GoalRemoteViewsFactory(applicationContext)
}

private class GoalRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Goal> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = GoalRepository.get(context).snapshot().filter {
            it.type == GoalType.DAILY && !it.isCompleted && it.isScheduledToday()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val goal = items[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_goal_item)
        rv.setTextViewText(R.id.item_emoji, goal.emoji)
        rv.setTextViewText(R.id.item_title, goal.title)
        rv.setTextViewText(
            R.id.item_progress,
            "${goal.progress}/${goal.target}${if (goal.unit.isNotBlank()) " ${goal.unit}" else ""}"
        )
        rv.setTextViewText(R.id.item_check, if (goal.canCheckInNow()) "Отметить" else "✓")

        val fillIn = Intent().putExtra(CheckInReceiver.EXTRA_GOAL_ID, goal.id)
        rv.setOnClickFillInIntent(R.id.item_root, fillIn)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}
