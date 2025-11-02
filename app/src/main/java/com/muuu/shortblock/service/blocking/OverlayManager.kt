package com.muuu.shortblock.service.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.muuu.unshort.AppConstants
import com.muuu.unshort.BlockOverlay
import com.muuu.unshort.OverlayType

/**
 * 오버레이 생명주기 관리자
 *
 * 앱별로 독립적인 오버레이 관리
 */
class OverlayManager(
    private val context: Context,
    private val onTimerCompleted: () -> Unit,
    private val onSkip: () -> Unit,
    private val onWatch: () -> Unit
) {
    private val TAG = "OverlayManager"

    // 앱별 오버레이 저장
    private val overlayByPackage = mutableMapOf<String, BlockOverlay>()
    private val sessionIdByPackage = mutableMapOf<String, String>()
    private val lastShownSessionIdByPackage = mutableMapOf<String, String>()

    // 타이머 완료 브로드캐스트 수신
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AppConstants.ACTION_TIMER_COMPLETED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    Log.d(TAG, "Timer completed broadcast: $sessionId")

                    // 모든 앱의 세션 ID 체크
                    sessionIdByPackage.forEach { (pkg, currentId) ->
                        val lastShownId = lastShownSessionIdByPackage[pkg]
                        if (sessionId == currentId || sessionId == lastShownId) {
                            Log.d(TAG, "[$pkg] Session matches - calling callback")
                            onTimerCompleted()
                        }
                    }
                }
                AppConstants.ACTION_CLOSE_OVERLAY -> {
                    Log.d(TAG, "Close overlay broadcast")
                    // 모든 오버레이 닫기
                    overlayByPackage.keys.toList().forEach { pkg ->
                        hideOverlay(pkg)
                    }
                }
            }
        }
    }

    private var isReceiverRegistered = false

    fun initialize() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_TIMER_COMPLETED)
                addAction(AppConstants.ACTION_CLOSE_OVERLAY)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(timerReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Timer receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver", e)
            }
        }
    }

    /**
     * 오버레이 표시
     */
    fun showOverlay(
        packageName: String,
        sessionId: String,
        overlayType: OverlayType = OverlayType.INITIAL
    ): String {
        // 기존 오버레이 있으면 제거
        overlayByPackage[packageName]?.let { existing ->
            if (existing.isShowing()) {
                Log.d(TAG, "[$packageName] Dismissing existing overlay to show new type: $overlayType")
                existing.dismiss()
            }
        }

        sessionIdByPackage[packageName] = sessionId
        lastShownSessionIdByPackage[packageName] = sessionId

        try {
            val overlay = BlockOverlay(context)

            overlay.show(
                onDismiss = {
                    Log.d(TAG, "[$packageName] Overlay dismissed")
                    overlayByPackage.remove(packageName)
                },
                onComplete = {
                    Log.d(TAG, "[$packageName] Timer completed")
                    onTimerCompleted()
                },
                onSkip = {
                    Log.d(TAG, "[$packageName] Skip button pressed")
                    hideOverlay(packageName)
                    onSkip()
                },
                onWatch = {
                    Log.d(TAG, "[$packageName] Watch button pressed")
                    hideOverlay(packageName)
                    onWatch()
                },
                sessionId = sessionId,
                sourcePackage = packageName,
                overlayType = overlayType
            )

            overlayByPackage[packageName] = overlay
            Log.d(TAG, "[$packageName] Overlay shown: session=$sessionId, type=$overlayType")
        } catch (e: Exception) {
            Log.e(TAG, "[$packageName] Failed to show overlay", e)
        }

        return sessionId
    }

    /**
     * 오버레이 숨김
     */
    fun hideOverlay(packageName: String) {
        overlayByPackage[packageName]?.let { overlay ->
            try {
                if (overlay.isShowing()) {
                    overlay.dismiss()
                    Log.d(TAG, "[$packageName] Overlay dismissed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$packageName] Error dismissing overlay", e)
            }
        }

        overlayByPackage.remove(packageName)
    }

    /**
     * 오버레이 표시 여부
     */
    fun isOverlayVisible(packageName: String): Boolean {
        return overlayByPackage[packageName]?.isShowing() == true
    }

    /**
     * 정리
     */
    fun cleanup() {
        // 모든 오버레이 제거
        overlayByPackage.keys.toList().forEach { pkg ->
            hideOverlay(pkg)
        }

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(timerReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Timer receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }

        sessionIdByPackage.clear()
        lastShownSessionIdByPackage.clear()
        Log.d(TAG, "OverlayManager cleaned up")
    }
}
