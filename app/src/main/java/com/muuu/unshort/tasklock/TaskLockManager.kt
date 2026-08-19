package com.muuu.unshort.tasklock

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class TaskLockItem(
    val id: String,
    val title: String,
    val isLocking: Boolean,
    val isCompleted: Boolean,
    val date: String,
    val completedAt: Long? = null,
    val repeatMode: String = REPEAT_ONCE,
    val verificationMode: String = VERIFY_DIRECT,
    val lastPhotoUri: String? = null,
    val startMinutes: Int? = null
)

data class TaskLockEvent(
    val type: String,
    val timestamp: Long,
    val verificationMode: String? = null,
    val result: String? = null
)

const val REPEAT_ONCE = "once"
const val REPEAT_DAILY = "daily"
const val REPEAT_WEEKDAYS = "weekdays"
const val VERIFY_DIRECT = "direct"
const val VERIFY_PHOTO = "photo"
const val VERIFY_AI_PHOTO = "ai_photo"
const val EVENT_CREATED = "created"
const val EVENT_COMPLETED = "completed"
const val EVENT_OVERLAY_SHOWN = "overlay_shown"
const val EVENT_POSTPONED = "postponed"
const val EVENT_CANCELLED = "cancelled"
const val EVENT_AI_RESULT = "ai_result"

object TaskLockManager {
    private const val PREFS_NAME = "task_lock_preferences"
    private const val KEY_TASKS = "tasks"
    private const val KEY_EVENTS = "events"
    private const val KEY_POSTPONE_UNTIL = "postpone_until"
    private const val KEY_POSTPONE_DATE = "postpone_date"
    private const val KEY_POSTPONE_COUNT = "postpone_count"

    const val POSTPONE_DURATION_MS = 30L * 60L * 1000L
    const val DAILY_POSTPONE_LIMIT = 2

    fun getTodayTasks(context: Context): List<TaskLockItem> {
        val today = LocalDate.now().toString()
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val tasks = readTasks(context)
        val refreshed = tasks.map { task ->
            if (task.repeatMode != REPEAT_ONCE && task.date != today) {
                task.copy(date = today, isCompleted = false, completedAt = null, lastPhotoUri = null)
            } else task
        }
        if (refreshed != tasks) writeTasks(context, refreshed)
        return refreshed.filter {
            when (it.repeatMode) {
                REPEAT_DAILY -> true
                REPEAT_WEEKDAYS -> dayOfWeek in 1..5
                else -> it.date == today
            }
        }
    }

    fun getPendingLockTasks(context: Context): List<TaskLockItem> =
        getTodayTasks(context).filter { it.isLocking && !it.isCompleted && isTaskActive(it) }

    fun isTaskActive(task: TaskLockItem, now: LocalTime = LocalTime.now()): Boolean {
        val start = task.startMinutes ?: return true
        return now.hour * 60 + now.minute >= start
    }

    fun isLockActive(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        getPendingLockTasks(context).isNotEmpty() && !isPostponed(context, now)

    fun addTask(
        context: Context,
        title: String,
        isLocking: Boolean,
        repeatMode: String = REPEAT_ONCE,
        verificationMode: String = VERIFY_DIRECT,
        startMinutes: Int? = null
    ): TaskLockItem {
        val item = TaskLockItem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            isLocking = isLocking,
            isCompleted = false,
            date = LocalDate.now().toString(),
            repeatMode = repeatMode,
            verificationMode = verificationMode,
            startMinutes = startMinutes
        )
        writeTasks(context, readTasks(context) + item)
        recordEvent(context, EVENT_CREATED, verificationMode)
        return item
    }

    fun setCompleted(context: Context, id: String, completed: Boolean) {
        val now = System.currentTimeMillis()
        writeTasks(
            context,
            readTasks(context).map {
                if (it.id == id) {
                    it.copy(isCompleted = completed, completedAt = if (completed) now else null)
                } else it
            }
        )
        if (completed) {
            getTask(context, id)?.let { recordEvent(context, EVENT_COMPLETED, it.verificationMode) }
        }
    }

    fun updateTask(
        context: Context,
        id: String,
        title: String,
        isLocking: Boolean,
        repeatMode: String,
        verificationMode: String,
        startMinutes: Int?
    ) {
        writeTasks(context, readTasks(context).map { task ->
            if (task.id == id) task.copy(
                title = title.trim(),
                isLocking = isLocking,
                repeatMode = repeatMode,
                verificationMode = verificationMode,
                startMinutes = startMinutes
            ) else task
        })
    }

    fun setCompletedWithPhoto(context: Context, id: String, photoUri: String) {
        val now = System.currentTimeMillis()
        writeTasks(context, readTasks(context).map {
            if (it.id == id) it.copy(
                isCompleted = true,
                completedAt = now,
                lastPhotoUri = photoUri.takeIf(String::isNotBlank)
            )
            else it
        })
        getTask(context, id)?.let { recordEvent(context, EVENT_COMPLETED, it.verificationMode) }
    }

    fun getTask(context: Context, id: String): TaskLockItem? =
        readTasks(context).firstOrNull { it.id == id }

    fun getTasksSince(context: Context, earliestDate: LocalDate): List<TaskLockItem> =
        readTasks(context).filter { runCatching { LocalDate.parse(it.date) >= earliestDate }.getOrDefault(false) }

    fun deleteTask(context: Context, id: String) {
        recordEvent(context, EVENT_CANCELLED, getTask(context, id)?.verificationMode)
        writeTasks(context, readTasks(context).filterNot { it.id == id })
    }

    fun getRemainingPostpones(context: Context): Int {
        ensurePostponeDay(context)
        val used = prefs(context).getInt(KEY_POSTPONE_COUNT, 0)
        return (DAILY_POSTPONE_LIMIT - used).coerceAtLeast(0)
    }

    fun postpone(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        ensurePostponeDay(context)
        val remaining = getRemainingPostpones(context)
        if (remaining <= 0 || isPostponed(context, now)) return false
        prefs(context).edit()
            .putLong(KEY_POSTPONE_UNTIL, now + POSTPONE_DURATION_MS)
            .putInt(KEY_POSTPONE_COUNT, DAILY_POSTPONE_LIMIT - remaining + 1)
            .apply()
        recordEvent(context, EVENT_POSTPONED)
        return true
    }

    fun recordOverlayShown(context: Context) = recordEvent(context, EVENT_OVERLAY_SHOWN)

    fun recordAiResult(context: Context, result: String) =
        recordEvent(context, EVENT_AI_RESULT, VERIFY_AI_PHOTO, result)

    fun getEventsSince(context: Context, sinceMillis: Long): List<TaskLockEvent> =
        readEvents(context).filter { it.timestamp >= sinceMillis }

    private fun recordEvent(
        context: Context,
        type: String,
        verificationMode: String? = null,
        result: String? = null
    ) {
        val cutoff = System.currentTimeMillis() - 90L * 24L * 60L * 60L * 1000L
        val events = readEvents(context).filter { it.timestamp >= cutoff } + TaskLockEvent(
            type = type,
            timestamp = System.currentTimeMillis(),
            verificationMode = verificationMode,
            result = result
        )
        val array = JSONArray()
        events.takeLast(1000).forEach { event ->
            array.put(JSONObject().apply {
                put("type", event.type)
                put("timestamp", event.timestamp)
                event.verificationMode?.let { put("verificationMode", it) }
                event.result?.let { put("result", it) }
            })
        }
        prefs(context).edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun readEvents(context: Context): List<TaskLockEvent> {
        val raw = prefs(context).getString(KEY_EVENTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(TaskLockEvent(
                        type = json.getString("type"),
                        timestamp = json.getLong("timestamp"),
                        verificationMode = json.optString("verificationMode").takeIf(String::isNotBlank),
                        result = json.optString("result").takeIf(String::isNotBlank)
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun isPostponed(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        ensurePostponeDay(context)
        return prefs(context).getLong(KEY_POSTPONE_UNTIL, 0L) > now
    }

    fun getPostponeUntil(context: Context): Long {
        ensurePostponeDay(context)
        return prefs(context).getLong(KEY_POSTPONE_UNTIL, 0L)
    }

    private fun ensurePostponeDay(context: Context) {
        val today = LocalDate.now().toString()
        val preferences = prefs(context)
        if (preferences.getString(KEY_POSTPONE_DATE, null) != today) {
            preferences.edit()
                .putString(KEY_POSTPONE_DATE, today)
                .putInt(KEY_POSTPONE_COUNT, 0)
                .putLong(KEY_POSTPONE_UNTIL, 0L)
                .apply()
        }
    }

    private fun readTasks(context: Context): List<TaskLockItem> {
        val raw = prefs(context).getString(KEY_TASKS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        TaskLockItem(
                            id = json.getString("id"),
                            title = json.getString("title"),
                            isLocking = json.optBoolean("isLocking", true),
                            isCompleted = json.optBoolean("isCompleted", false),
                            date = json.getString("date"),
                            completedAt = json.optLong("completedAt").takeIf { it > 0L },
                            repeatMode = json.optString("repeatMode", REPEAT_ONCE),
                            verificationMode = json.optString("verificationMode", VERIFY_DIRECT),
                            lastPhotoUri = json.optString("lastPhotoUri").takeIf { it.isNotEmpty() },
                            startMinutes = json.optInt("startMinutes", -1).takeIf { it >= 0 }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeTasks(context: Context, tasks: List<TaskLockItem>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("isLocking", task.isLocking)
                    put("isCompleted", task.isCompleted)
                    put("date", task.date)
                    task.completedAt?.let { put("completedAt", it) }
                    put("repeatMode", task.repeatMode)
                    put("verificationMode", task.verificationMode)
                    task.lastPhotoUri?.let { put("lastPhotoUri", it) }
                    task.startMinutes?.let { put("startMinutes", it) }
                }
            )
        }
        prefs(context).edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
