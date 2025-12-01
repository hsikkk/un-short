package com.muuu.unshort

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.muuu.unshort.prefs.PreferencesManager

/**
 * POST_NOTIFICATIONS 권한 체크 및 요청을 관리하는 Helper 클래스
 *
 * MainActivity와 SettingsActivity에서 재사용
 */
class NotificationPermissionHelper(
    private val activity: AppCompatActivity,
    private val prefsManager: PreferencesManager
) {
    companion object {
        private const val TAG = "NotificationPermissionHelper"
    }

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null

    /**
     * Activity의 onCreate에서 호출하여 Launcher 등록
     *
     * @param onGranted 권한 허용 시 콜백
     * @param onDenied 권한 거부 시 콜백
     */
    fun registerLauncher(
        onGranted: (() -> Unit)? = null,
        onDenied: (() -> Unit)? = null
    ) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied

        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Notification permission granted")
                onPermissionGranted?.invoke()
            } else {
                Log.d(TAG, "Notification permission denied")
                onPermissionDenied?.invoke()
            }
        }
    }

    /**
     * POST_NOTIFICATIONS 권한 보유 여부 확인
     *
     * @return Android 13 미만은 항상 true, 그 외는 실제 권한 상태 반환
     */
    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * POST_NOTIFICATIONS 권한 요청
     *
     * @param skipIfAsked true면 이미 물어본 경우 스킵 (MainActivity용, 기본값: false)
     */
    fun requestPermission(skipIfAsked: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Android 13 미만은 권한 불필요
            onPermissionGranted?.invoke()
            return
        }

        // 이미 물어봤으면 스킵 (MainActivity용)
        if (skipIfAsked && prefsManager.hasAskedNotificationPermission) {
            Log.d(TAG, "Permission already asked, skipping")
            return
        }

        // 이미 권한 있으면 콜백만 호출
        if (hasPermission()) {
            if (skipIfAsked) {
                prefsManager.hasAskedNotificationPermission = true
            }
            onPermissionGranted?.invoke()
            return
        }

        // 권한 요청
        permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)

        // MainActivity는 플래그 저장
        if (skipIfAsked) {
            prefsManager.hasAskedNotificationPermission = true
        }
    }
}
