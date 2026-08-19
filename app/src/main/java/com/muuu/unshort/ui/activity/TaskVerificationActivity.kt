package com.muuu.unshort.ui.activity

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.muuu.unshort.R
import com.muuu.unshort.tasklock.OnDeviceTaskVerifier
import com.muuu.unshort.tasklock.TaskLockManager
import com.muuu.unshort.tasklock.VERIFY_AI_PHOTO
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TaskVerificationActivity : BaseActivity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var complete: Button
    private var photoFile: File? = null
    private var taskId: String = ""

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) showAndVerifyPhoto() else showCaptureCancelled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_verification)
        taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        val task = TaskLockManager.getTask(this, taskId) ?: run { finish(); return }

        preview = findViewById(R.id.taskVerificationPreview)
        status = findViewById(R.id.taskVerificationStatus)
        progress = findViewById(R.id.taskVerificationProgress)
        complete = findViewById(R.id.taskVerificationComplete)
        findViewById<TextView>(R.id.taskVerificationTitle).text = task.title
        findViewById<View>(R.id.taskVerificationClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.taskVerificationRetry).setOnClickListener { launchCamera() }
        complete.setOnClickListener {
            TaskLockManager.setCompletedWithPhoto(this, taskId, "")
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.TASK_LOCK_TASK_COMPLETED,
                mapOf("verification_mode" to task.verificationMode)
            )
            photoFile?.delete()
            setResult(RESULT_OK)
            finish()
        }
        complete.isEnabled = false
        launchCamera()
    }

    private fun launchCamera() {
        val directory = File(cacheDir, "task-verification").apply { mkdirs() }
        photoFile?.delete()
        photoFile = File.createTempFile("task_", ".jpg", directory)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile!!)
        takePicture.launch(uri)
    }

    private fun showAndVerifyPhoto() {
        val file = photoFile ?: return
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return showCaptureCancelled()
        preview.setImageBitmap(bitmap)
        preview.visibility = View.VISIBLE
        val task = TaskLockManager.getTask(this, taskId) ?: return
        if (task.verificationMode != VERIFY_AI_PHOTO) {
            status.setText(R.string.task_verification_photo_ready)
            complete.isEnabled = true
            return
        }

        progress.visibility = View.VISIBLE
        status.setText(R.string.task_verification_ai_checking)
        complete.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                OnDeviceTaskVerifier.verify(bitmap, task.title)
            }
            progress.visibility = View.GONE
            TaskLockManager.recordAiResult(this@TaskVerificationActivity, result.status)
            AnalyticsManager.trackEvent(
                this@TaskVerificationActivity,
                AnalyticsEvent.TASK_LOCK_AI_RESULT,
                mapOf("result" to result.status)
            )
            when (result.status) {
                OnDeviceTaskVerifier.VERIFIED -> {
                    status.setText(R.string.task_verification_ai_verified)
                    complete.isEnabled = true
                }
                OnDeviceTaskVerifier.NOT_VERIFIED -> {
                    status.setText(R.string.task_verification_ai_not_verified)
                    complete.isEnabled = false
                }
                else -> {
                    status.setText(R.string.task_verification_ai_fallback)
                    complete.isEnabled = true
                }
            }
        }
    }

    private fun showCaptureCancelled() {
        status.setText(R.string.task_verification_capture_cancelled)
        complete.isEnabled = false
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) photoFile?.delete()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}
