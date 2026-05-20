package com.muuu.unshort.util

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.muuu.unshort.timer.DisableConfirmTimerActivity
import com.muuu.unshort.ui.dialog.WarningDialog
import com.muuu.unshort.ui.activity.MainActivity
import com.muuu.unshort.ui.dialog.DisableConfirmDialog
import com.muuu.unshort.ui.activity.SettingsActivity
import com.muuu.unshort.util.ThreeStepProtectionHelper

/**
 * 3단계 보호 플로우 헬퍼
 *
 * 책임:
 * - 중요한 설정 해제 시 3단계 확인 절차 제공
 * - 0단계: WarningDialog (경고)
 * - 1단계: DisableConfirmTimerActivity (60초 타이머 + 폰 뒤집기)
 * - 2단계: DisableConfirmDialog (문구 입력)
 *
 * 사용처:
 * - MainActivity: 차단 OFF 전환
 * - SettingsActivity: 충동적 비활성화 방지 해제
 * - SettingsActivity: 앱 삭제 방지 해제
 */
class ThreeStepProtectionHelper(
    private val activity: AppCompatActivity
) {

    private var timerLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Activity Result Launcher 등록
     *
     * Activity.onCreate()에서 registerForActivityResult() 후 호출
     */
    fun registerTimerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.timerLauncher = launcher
    }

    /**
     * 3단계 보호 플로우 시작
     *
     * @param warningConfig 0단계 경고 다이얼로그 설정
     * @param confirmConfig 2단계 문구 입력 설정
     * @param finalAction 최종 실행할 액션
     * @param protectionContext 어떤 보호 흐름인지 (analytics용)
     */
    fun start(
        warningConfig: WarningConfig,
        confirmConfig: ConfirmConfig,
        finalAction: () -> Unit,
        protectionContext: String = DisableConfirmTimerActivity.CONTEXT_UNKNOWN
    ) {
        // Config와 액션 저장 (Activity Result에서 사용)
        pendingConfirmConfig = confirmConfig
        pendingFinalAction = finalAction
        pendingProtectionContext = protectionContext

        // 0단계: WarningDialog 표시
        showWarningDialog(warningConfig)
    }

    /**
     * Activity Result 콜백 처리
     *
     * Activity의 registerForActivityResult 콜백에서 호출
     */
    fun handleTimerResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            // 1단계 완료 - 2단계 문구 입력 다이얼로그 표시
            val confirmConfig = pendingConfirmConfig
                ?: throw IllegalStateException("No pending confirm config")
            val finalAction = pendingFinalAction
                ?: throw IllegalStateException("No pending final action")

            showConfirmDialog(confirmConfig, finalAction)
        } else {
            // RESULT_CANCELED: 타이머 취소 - 복원 콜백 실행
            pendingConfirmConfig?.onCancel?.invoke()
            clearPending()
        }
    }

    // Private

    private var pendingConfirmConfig: ConfirmConfig? = null
    private var pendingFinalAction: (() -> Unit)? = null
    private var pendingProtectionContext: String = DisableConfirmTimerActivity.CONTEXT_UNKNOWN

    private fun showWarningDialog(config: WarningConfig) {
        val dialog = WarningDialog(
            context = activity,
            titleResId = config.titleResId,
            messageResId = config.messageResId,
            positiveTextResId = config.positiveTextResId,
            negativeTextResId = config.negativeTextResId,
            onPositive = {
                // 1단계: DisableConfirmTimerActivity 시작
                startTimerActivity()
            },
            onNegative = {
                // 0단계 취소 - 복원 콜백 실행
                pendingConfirmConfig?.onWarningCancel?.invoke()
                clearPending()
            }
        )
        dialog.show()
    }

    private fun startTimerActivity() {
        val launcher = timerLauncher
            ?: throw IllegalStateException("Timer launcher not registered. Call registerTimerLauncher() first.")

        val intent = DisableConfirmTimerActivity.newIntent(activity, pendingProtectionContext)
        launcher.launch(intent)
    }

    private fun showConfirmDialog(config: ConfirmConfig, finalAction: () -> Unit) {
        val dialog = DisableConfirmDialog(
            context = activity,
            titleResId = config.titleResId,
            messageResId = config.messageResId,
            requiredPhraseResId = config.requiredPhraseResId,
            onConfirm = {
                // 최종 액션 실행
                finalAction()
                clearPending()
            },
            onCancel = {
                // 취소 콜백 실행 (스위치 상태 복원 등)
                config.onCancel?.invoke()
                clearPending()
            }
        )
        dialog.show()
    }

    private fun clearPending() {
        pendingConfirmConfig = null
        pendingFinalAction = null
        pendingProtectionContext = DisableConfirmTimerActivity.CONTEXT_UNKNOWN
    }
}

/**
 * 0단계 경고 다이얼로그 설정
 */
data class WarningConfig(
    val titleResId: Int,
    val messageResId: Int,
    val positiveTextResId: Int,
    val negativeTextResId: Int
)

/**
 * 2단계 문구 입력 다이얼로그 설정
 */
data class ConfirmConfig(
    val titleResId: Int,
    val messageResId: Int,
    val requiredPhraseResId: Int,
    val onCancel: (() -> Unit)? = null,
    val onWarningCancel: (() -> Unit)? = null  // 0단계 취소 시 콜백
)
