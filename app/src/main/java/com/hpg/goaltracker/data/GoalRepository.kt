package com.hpg.goaltracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Single source of truth for goals, persisted as a JSON file in internal storage.
 * No external persistence library is used so the project stays build-stable.
 */
class GoalRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "goals.json")

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    init {
        _goals.value = readFromDisk()
    }

    fun snapshot(): List<Goal> = _goals.value

    /** Insert or replace a goal by id, keeping stable ordering. */
    fun upsert(goal: Goal) {
        val current = _goals.value
        val index = current.indexOfFirst { it.id == goal.id }
        val updated = if (index >= 0) {
            current.toMutableList().also { it[index] = goal }
        } else {
            current + goal
        }
        commit(updated)
    }

    fun delete(id: String) {
        val target = _goals.value.firstOrNull { it.id == id }
        target?.imagePath?.let { runCatching { File(it).delete() } }
        commit(_goals.value.filterNot { it.id == id })
    }

    private fun commit(list: List<Goal>) {
        _goals.value = list
        writeToDisk(list)
    }

    private fun writeToDisk(list: List<Goal>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        }
    }

    private fun readFromDisk(): List<Goal> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { goalFromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    companion object {
        @Volatile
        private var instance: GoalRepository? = null

        fun get(context: Context): GoalRepository =
            instance ?: synchronized(this) {
                instance ?: GoalRepository(context).also { instance = it }
            }
    }
}

private fun Goal.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("emoji", emoji)
    put("imagePath", imagePath ?: JSONObject.NULL)
    put("colorArgb", colorArgb)
    put("type", type.name)
    put("target", target)
    put("periodDays", periodDays)
    put("progress", progress)
    put("unit", unit)
    put("createdAt", createdAt)
    put("completedAt", completedAt ?: JSONObject.NULL)
    put("lastCheckIn", lastCheckIn ?: JSONObject.NULL)
    put("notify", notify)
}

private fun goalFromJson(o: JSONObject): Goal = Goal(
    id = o.getString("id"),
    title = o.getString("title"),
    emoji = o.optString("emoji", "🎯"),
    imagePath = if (o.isNull("imagePath")) null else o.getString("imagePath"),
    colorArgb = o.getInt("colorArgb"),
    type = runCatching { GoalType.valueOf(o.getString("type")) }.getOrDefault(GoalType.DAILY),
    target = o.getInt("target"),
    periodDays = o.optInt("periodDays", 1),
    progress = o.optInt("progress", 0),
    unit = o.optString("unit", ""),
    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
    lastCheckIn = if (o.isNull("lastCheckIn")) null else o.getLong("lastCheckIn"),
    notify = o.optBoolean("notify", true),
)
