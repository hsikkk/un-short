package com.muuu.unshort.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.RadioGroup
import android.widget.TimePicker
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.muuu.unshort.R
import com.muuu.unshort.tasklock.TaskLockItem
import com.muuu.unshort.tasklock.TaskLockManager
import com.muuu.unshort.tasklock.REPEAT_DAILY
import com.muuu.unshort.tasklock.REPEAT_ONCE
import com.muuu.unshort.tasklock.REPEAT_WEEKDAYS
import com.muuu.unshort.tasklock.VERIFY_AI_PHOTO
import com.muuu.unshort.tasklock.VERIFY_DIRECT
import com.muuu.unshort.tasklock.VERIFY_PHOTO
import android.content.Intent
import android.os.CountDownTimer
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.DateFormat
import java.util.Calendar

class TaskLockActivity : BaseActivity() {

    private lateinit var taskContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var summaryView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_lock)

        taskContainer = findViewById(R.id.taskLockList)
        emptyView = findViewById(R.id.taskLockEmpty)
        summaryView = findViewById(R.id.taskLockSummary)

        findViewById<TextView>(R.id.headerTitle).setText(R.string.task_lock_title)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.rightButton).apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_add)
            contentDescription = getString(R.string.task_lock_add_title)
            setOnClickListener { showTaskSheet() }
        }
        findViewById<View>(R.id.taskLockReportLink).setOnClickListener {
            startActivity(Intent(this, TaskLockReportActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderTasks()
    }

    private fun renderTasks() {
        val tasks = TaskLockManager.getTodayTasks(this)
        val pendingLockCount = tasks.count { it.isLocking && !it.isCompleted && TaskLockManager.isTaskActive(it) }
        summaryView.text = resources.getQuantityString(
            R.plurals.task_lock_pending_summary,
            pendingLockCount,
            pendingLockCount
        )
        emptyView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        taskContainer.removeAllViews()
        tasks.forEach { task -> taskContainer.addView(createTaskRow(task)) }
    }

    private fun createTaskRow(task: TaskLockItem): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_task_lock, taskContainer, false)
        val checkbox = row.findViewById<CheckBox>(R.id.taskLockCheck)
        val title = row.findViewById<TextView>(R.id.taskLockTitle)
        val badge = row.findViewById<TextView>(R.id.taskLockBadge)
        val delete = row.findViewById<ImageButton>(R.id.taskLockDelete)

        checkbox.isChecked = task.isCompleted
        val isActive = TaskLockManager.isTaskActive(task)
        checkbox.isEnabled = isActive
        title.text = task.title
        title.alpha = if (task.isCompleted) 0.45f else 1f
        badge.visibility = if (task.isLocking) View.VISIBLE else View.GONE
        badge.text = when (task.repeatMode) {
            REPEAT_DAILY -> "${getString(R.string.task_lock_badge)} · ${getString(R.string.task_lock_repeat_daily)}"
            REPEAT_WEEKDAYS -> "${getString(R.string.task_lock_badge)} · ${getString(R.string.task_lock_repeat_weekdays)}"
            else -> getString(R.string.task_lock_badge)
        }
        if (!isActive && task.startMinutes != null) {
            badge.visibility = View.VISIBLE
            badge.text = getString(R.string.task_lock_starts_at, formatTime(task.startMinutes))
            row.alpha = 0.62f
        }
        checkbox.setOnClickListener {
            if (checkbox.isChecked && task.verificationMode != VERIFY_DIRECT) {
                checkbox.isChecked = false
                startActivity(Intent(this, TaskVerificationActivity::class.java).apply {
                    putExtra(TaskVerificationActivity.EXTRA_TASK_ID, task.id)
                })
            } else {
                TaskLockManager.setCompleted(this, task.id, checkbox.isChecked)
                if (checkbox.isChecked) AnalyticsManager.trackEvent(
                    this,
                    AnalyticsEvent.TASK_LOCK_TASK_COMPLETED,
                    mapOf("verification_mode" to task.verificationMode)
                )
                renderTasks()
            }
        }
        row.setOnClickListener { showTaskSheet(task) }
        delete.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.task_lock_delete_title)
                .setMessage(if (task.isLocking && !task.isCompleted) R.string.task_lock_delete_friction else R.string.task_lock_delete_normal)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.task_lock_delete) { _, _ ->
                    TaskLockManager.deleteTask(this, task.id)
                    AnalyticsManager.trackEvent(this, AnalyticsEvent.TASK_LOCK_CANCELLED)
                    renderTasks()
                }
                .show()
            if (task.isLocking && !task.isCompleted) {
                val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        positive.isEnabled = false
                        positive.text = getString(R.string.task_lock_delete_countdown, (millisUntilFinished / 1000) + 1)
                    }
                    override fun onFinish() {
                        positive.isEnabled = true
                        positive.setText(R.string.task_lock_delete)
                    }
                }.start()
            }
        }
        return row
    }

    private fun showTaskSheet(task: TaskLockItem? = null) {
        val content = layoutInflater.inflate(R.layout.dialog_add_task_lock, null)
        val sheetTitle = content.findViewById<TextView>(R.id.taskLockSheetTitle)
        val titleInput = content.findViewById<EditText>(R.id.taskLockTitleInput)
        val lockingCheck = content.findViewById<SwitchMaterial>(R.id.taskLockEnabledInput)
        val repeatInput = content.findViewById<Spinner>(R.id.taskLockRepeatInput)
        val verificationInput = content.findViewById<Spinner>(R.id.taskLockVerificationInput)
        val startMode = content.findViewById<RadioGroup>(R.id.taskLockStartModeInput)
        val startTime = content.findViewById<TimePicker>(R.id.taskLockStartTimeInput).apply {
            setIs24HourView(android.text.format.DateFormat.is24HourFormat(this@TaskLockActivity))
        }
        val save = content.findViewById<MaterialButton>(R.id.taskLockSaveButton)
        val dialog = BottomSheetDialog(this).apply { setContentView(content) }
        if (task != null) {
            sheetTitle.setText(R.string.task_lock_edit_title)
            save.setText(R.string.task_lock_update)
            titleInput.setText(task.title)
            lockingCheck.isChecked = task.isLocking
            repeatInput.setSelection(when (task.repeatMode) {
                REPEAT_DAILY -> 1
                REPEAT_WEEKDAYS -> 2
                else -> 0
            })
            verificationInput.setSelection(when (task.verificationMode) {
                VERIFY_PHOTO -> 1
                VERIFY_AI_PHOTO -> 2
                else -> 0
            })
            task.startMinutes?.let { minutes ->
                startMode.check(R.id.taskLockStartScheduled)
                startTime.visibility = View.VISIBLE
                startTime.hour = minutes / 60
                startTime.minute = minutes % 60
            }
        }
        startMode.setOnCheckedChangeListener { _, checkedId ->
            startTime.visibility = if (checkedId == R.id.taskLockStartScheduled) View.VISIBLE else View.GONE
        }
        save.setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    titleInput.error = getString(R.string.task_lock_title_required)
                } else {
                    val repeatMode = when (repeatInput.selectedItemPosition) {
                        1 -> REPEAT_DAILY
                        2 -> REPEAT_WEEKDAYS
                        else -> REPEAT_ONCE
                    }
                    val verificationMode = when (verificationInput.selectedItemPosition) {
                        1 -> VERIFY_PHOTO
                        2 -> VERIFY_AI_PHOTO
                        else -> VERIFY_DIRECT
                    }
                    val startMinutes = if (startMode.checkedRadioButtonId == R.id.taskLockStartScheduled) {
                        startTime.hour * 60 + startTime.minute
                    } else null
                    if (task == null) {
                        TaskLockManager.addTask(
                            this,
                            title,
                            lockingCheck.isChecked,
                            repeatMode,
                            verificationMode,
                            startMinutes
                        )
                        AnalyticsManager.trackEvent(
                            this,
                            AnalyticsEvent.TASK_LOCK_TASK_CREATED,
                            mapOf(
                                "repeat_mode" to repeatMode,
                                "verification_mode" to verificationMode,
                                "has_scheduled_start" to (startMinutes != null),
                                "is_locking" to lockingCheck.isChecked
                            )
                        )
                    } else {
                        TaskLockManager.updateTask(
                            this,
                            task.id,
                            title,
                            lockingCheck.isChecked,
                            repeatMode,
                            verificationMode,
                            startMinutes
                        )
                    }
                    dialog.dismiss()
                    renderTasks()
                }
        }
        dialog.show()
    }

    private fun formatTime(minutes: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }
}
