package com.muuu.unshort.feedblock.settings

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.muuu.unshort.R
import com.muuu.unshort.feedblock.FeedBlockService
import com.muuu.unshort.feedblock.prefs.FeedBlockPreferences

/**
 * 피드 차단 (베타) 설정을 BottomSheet로 표시.
 *
 * 권한 흐름:
 * - 토글 OFF → ON 시도 시 권한 없으면 권한 다이얼로그 → 시스템 접근성 진입
 * - 시스템 접근성에서 권한 활성화 후 앱 복귀 → ActivityLifecycleCallbacks로 onResume 감지
 *   → 권한 재확인 → prefs 동기화 + UI 갱신
 * - 권한 안 주고 돌아오면 prefs OFF 유지
 */
object AdvancedSettingsBottomSheet {

    fun show(activity: Activity) {
        val prefs = FeedBlockPreferences(activity)

        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.bottom_sheet_advanced_settings, null)

        val betaToggle = view.findViewById<SwitchMaterial>(R.id.betaToggle)
        val instagramToggle = view.findViewById<SwitchMaterial>(R.id.instagramToggle)
        val youtubeToggle = view.findViewById<SwitchMaterial>(R.id.youtubeToggle)
        val threadsToggle = view.findViewById<SwitchMaterial>(R.id.threadsToggle)
        val facebookToggle = view.findViewById<SwitchMaterial>(R.id.facebookToggle)
        val targetAppsSection = view.findViewById<LinearLayout>(R.id.targetAppsSection)

        // 시스템 접근성 진입 시 사용자 의도 ON을 prefs에 낙관적으로 반영. 시스템 접근성에서
        // 권한을 실제로 부여하지 않고 돌아오면 onResume 콜백에서 검증 후 OFF로 되돌린다.
        var awaitingSystemPermissionGrant = false

        fun renderState() {
            val activeNow = prefs.isBetaEnabled

            betaToggle.setOnCheckedChangeListener(null)
            betaToggle.isChecked = activeNow
            betaToggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (isFeedBlockServiceEnabled(activity)) {
                        prefs.isBetaEnabled = true
                        renderState()
                    } else {
                        showPermissionDialog(activity) {
                            awaitingSystemPermissionGrant = true
                            prefs.isBetaEnabled = true
                            ensureFeedBlockServiceComponentEnabled(activity)
                            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        renderState()
                    }
                } else {
                    prefs.isBetaEnabled = false
                    renderState()
                }
            }

            targetAppsSection.visibility = if (activeNow) View.VISIBLE else View.GONE

            instagramToggle.setOnCheckedChangeListener(null)
            instagramToggle.isChecked = prefs.isInstagramEnabled
            instagramToggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isInstagramEnabled = isChecked
            }

            youtubeToggle.setOnCheckedChangeListener(null)
            youtubeToggle.isChecked = prefs.isYoutubeEnabled
            youtubeToggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isYoutubeEnabled = isChecked
            }

            threadsToggle.setOnCheckedChangeListener(null)
            threadsToggle.isChecked = prefs.isThreadsEnabled
            threadsToggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isThreadsEnabled = isChecked
            }

            facebookToggle.setOnCheckedChangeListener(null)
            facebookToggle.isChecked = prefs.isFacebookEnabled
            facebookToggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isFacebookEnabled = isChecked
            }
        }

        renderState()

        // 시스템 접근성 갔다가 돌아왔을 때 prefs/토글 동기화를 위해 lifecycle 등록
        val app = activity.application
        val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(resumedActivity: Activity) {
                if (resumedActivity !== activity) return
                if (!awaitingSystemPermissionGrant) {
                    renderState()
                    return
                }
                awaitingSystemPermissionGrant = false
                if (!isFeedBlockServiceEnabled(activity)) {
                    prefs.isBetaEnabled = false
                }
                renderState()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        dialog.setOnDismissListener {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showPermissionDialog(
        activity: Activity,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.feed_block_permission_dialog_title)
            .setMessage(R.string.feed_block_permission_dialog_message)
            .setPositiveButton(R.string.feed_block_open_accessibility_settings) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.feed_block_onboarding_cancel, null)
            .show()
    }

    /**
     * 서비스가 사용자 동의로 활성화되어 실제로 동작 중인지 확인.
     *
     * 두 신호를 모두 만족해야 활성:
     * 1. `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`에 우리 서비스가 명시적으로 있음
     * 2. `FeedBlockService.instance != null` (`onServiceConnected` 호출됨)
     */
    private fun isFeedBlockServiceEnabled(activity: Activity): Boolean {
        val expected = "${activity.packageName}/${FeedBlockService::class.java.name}"
        val enabledList = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val inEnabledList = enabledList.split(":").any { it.equals(expected, ignoreCase = true) }
        return inEnabledList && FeedBlockService.instance != null
    }

    /**
     * 컴포넌트 상태를 ENABLED로 복구만 한다 (DISABLED 전환은 절대 하지 않는다).
     *
     * setComponentEnabledSetting(DISABLED)는 시스템이 service를 "not installed"로
     * 판정하게 만들어 시스템 접근성 UI에서 영구히 사라지게 한다. 사용자가 활성화를
     * 다시 시도해도 OS가 bind를 거부한다.
     */
    private fun ensureFeedBlockServiceComponentEnabled(activity: Activity) {
        try {
            val component = ComponentName(activity, FeedBlockService::class.java)
            val current = activity.packageManager.getComponentEnabledSetting(component)
            if (current == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                current == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                return
            }
            activity.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {
        }
    }
}
