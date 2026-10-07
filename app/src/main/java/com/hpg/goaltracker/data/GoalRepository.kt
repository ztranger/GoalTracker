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
        cleanupOrphanImages()
    }

    /** Removes stored image files no longer referenced by any goal (undo-safe housekeeping). */
    private fun cleanupOrphanImages() {
        runCatching {
            val dir = File(appContext.filesDir, "images")
            if (!dir.isDirectory) return
            val referenced = _goals.value.mapNotNull { it.imagePath }.toHashSet()
            dir.listFiles()?.forEach { f ->
                if (f.absolutePath !in referenced) f.delete()
            }
        }
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
        // Image files are kept so a delete can be undone; orphans are swept on next launch.
        commit(_goals.value.filterNot { it.id == id })
    }

    /**
     * Applies a daily check-in if currently allowed; returns the updated goal,
     * or null if not eligible. Usable without a ViewModel (widget / notification).
     */
    fun checkInDaily(id: String): Goal? {
        val goal = _goals.value.firstOrNull { it.id == id } ?: return null
        if (!goal.canCheckInNow()) return null
        val now = System.currentTimeMillis()
        val newProgress = (goal.progress + 1).coerceAtMost(goal.target)
        val updated = goal.copy(
            progress = newProgress,
            checkIns = goal.checkIns + now,
            lastCheckIn = now,
            completedAt = if (newProgress >= goal.target && goal.completedAt == null) now else goal.completedAt
        )
        upsert(updated)
        return updated
    }

    /** Serializes all goals to a pretty-printed JSON string for backup/export. */
    fun exportJson(): String {
        val arr = JSONArray()
        _goals.value.forEach { arr.put(it.toJson()) }
        return arr.toString(2)
    }

    /** Parses a backup JSON string into goals (throws on malformed input). */
    fun parseJson(json: String): List<Goal> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { goalFromJson(arr.getJSONObject(it)) }
    }

    /** Replaces all goals with [list]. */
    fun replaceAll(list: List<Goal>) = commit(list)

    /** Merges [incoming] into existing goals by id (incoming wins on conflicts). */
    fun mergeAll(incoming: List<Goal>) {
        val byId = _goals.value.associateBy { it.id }.toMutableMap()
        incoming.forEach { byId[it.id] = it }
        commit(byId.values.toList())
    }

    private fun commit(list: List<Goal>) {
        _goals.value = list
        writeToDisk(list)
        runCatching { com.hpg.goaltracker.widget.GoalWidgetProvider.refresh(appContext) }
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
    put("activeDays", activeDays)
    put("deadline", deadline ?: JSONObject.NULL)
    put("category", category)
    put("progress", progress)
    put("unit", unit)
    put("createdAt", createdAt)
    put("completedAt", completedAt ?: JSONObject.NULL)
    put("lastCheckIn", lastCheckIn ?: JSONObject.NULL)
    put("checkIns", JSONArray(checkIns))
    put("frozenDays", JSONArray(frozenDays))
    put("notes", JSONArray().apply {
        notes.forEach { n ->
            put(JSONObject().apply {
                put("dayKey", n.dayKey)
                put("text", n.text)
                put("mood", n.mood)
            })
        }
    })
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
    activeDays = o.optInt("activeDays", WeekMask.ALL_DAYS),
    deadline = if (o.isNull("deadline")) null else o.optLong("deadline"),
    category = o.optString("category", ""),
    progress = o.optInt("progress", 0),
    unit = o.optString("unit", ""),
    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
    lastCheckIn = if (o.isNull("lastCheckIn")) null else o.getLong("lastCheckIn"),
    checkIns = o.optJSONArray("checkIns")?.let { arr ->
        (0 until arr.length()).map { arr.getLong(it) }
    } ?: emptyList(),
    frozenDays = o.optJSONArray("frozenDays")?.let { arr ->
        (0 until arr.length()).map { arr.getLong(it) }
    } ?: emptyList(),
    notes = o.optJSONArray("notes")?.let { arr ->
        (0 until arr.length()).map { i ->
            val n = arr.getJSONObject(i)
            DayNote(n.getLong("dayKey"), n.optString("text", ""), n.optString("mood", ""))
        }
    } ?: emptyList(),
    notify = o.optBoolean("notify", true),
)
