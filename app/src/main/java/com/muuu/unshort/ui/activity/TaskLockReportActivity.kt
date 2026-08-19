package com.muuu.unshort.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.muuu.unshort.R
import com.muuu.unshort.tasklock.EVENT_AI_RESULT
import com.muuu.unshort.tasklock.EVENT_COMPLETED
import com.muuu.unshort.tasklock.EVENT_OVERLAY_SHOWN
import com.muuu.unshort.tasklock.EVENT_POSTPONED
import com.muuu.unshort.tasklock.TaskLockManager
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class TaskLockReportActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_lock_report)
        findViewById<View>(R.id.taskLockReportBack).setOnClickListener { finish() }
        render()
    }

    private fun render() {
        val start = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = TaskLockManager.getEventsSince(this, start)
        findViewById<TextView>(R.id.taskLockReportCompleted).text = events.count { it.type == EVENT_COMPLETED }.toString()
        findViewById<TextView>(R.id.taskLockReportBlocked).text = events.count { it.type == EVENT_OVERLAY_SHOWN }.toString()
        findViewById<TextView>(R.id.taskLockReportPostponed).text = events.count { it.type == EVENT_POSTPONED }.toString()
        findViewById<TextView>(R.id.taskLockReportAi).text = events.count { it.type == EVENT_AI_RESULT }.toString()

        val history = findViewById<LinearLayout>(R.id.taskLockReportHistory)
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM)
        (0L..6L).forEach { offset ->
            val day = LocalDate.now().minusDays(offset)
            val dayStart = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val completed = events.count { it.type == EVENT_COMPLETED && it.timestamp in dayStart until dayEnd }
            val blocked = events.count { it.type == EVENT_OVERLAY_SHOWN && it.timestamp in dayStart until dayEnd }
            history.addView(TextView(this).apply {
                setPadding(0, 14, 0, 14)
                textSize = 14f
                setTextColor(0xFF444444.toInt())
                text = getString(
                    R.string.task_lock_report_day_row,
                    formatter.format(Date(dayStart)),
                    completed,
                    blocked
                )
            })
        }
    }
}
