package com.muuu.unshort.ui.dialog

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.muuu.unshort.R
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.service.blocking.AppBlockingConfig
import com.muuu.unshort.service.blocking.AppBlockingRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog for managing per-app blocking settings
 */
class AppBlockingSettingsDialog(
    private val activity: Activity,
    private val onSettingsChanged: () -> Unit = {},
    private val onRequestTimer: (String) -> Unit = {}
) {
    private val prefsManager = PreferencesManager(activity)
    private val statisticsRepository = StatisticsRepository(activity)
    private var currentDialog: androidx.appcompat.app.AlertDialog? = null
    private val switchesByPackage = mutableMapOf<String, SwitchMaterial>()

    fun show() {
        val installedConfigs = AppBlockingRegistry.ALL_CONFIGS
            .filter { isAppInstalled(it.packageName) }

        if (installedConfigs.isEmpty()) {
            // No apps installed - show message
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.app_blocking_settings_title)
                .setMessage(R.string.app_blocking_settings_no_apps)
                .setPositiveButton(R.string.app_blocking_settings_close, null)
                .show()
            return
        }

        val dialogView = LayoutInflater.from(activity).inflate(
            R.layout.dialog_app_blocking_settings, null
        )

        val container = dialogView.findViewById<LinearLayout>(R.id.appsContainer)

        installedConfigs.forEach { config ->
            val itemView = createSwitchItem(config)
            container.addView(itemView)
        }

        currentDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.app_blocking_settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.app_blocking_settings_close) { dialog, _ ->
                onSettingsChanged()
                dialog.dismiss()
            }
            .show()
    }

    private fun createSwitchItem(config: AppBlockingConfig): LinearLayout {
        val itemView = LayoutInflater.from(activity).inflate(
            R.layout.item_app_blocking_switch, null
        ) as LinearLayout

        val appIcon = itemView.findViewById<ImageView>(R.id.appIcon)
        val appName = itemView.findViewById<TextView>(R.id.appName)
        val appSwitch = itemView.findViewById<SwitchMaterial>(R.id.appSwitch)

        // Get actual app icon from PackageManager
        val icon = try {
            activity.packageManager.getApplicationIcon(config.packageName)
        } catch (e: Exception) {
            ResourcesCompat.getDrawable(activity.resources, config.iconResId, null)
        }

        appIcon.setImageDrawable(icon)

        // Get actual app name from PackageManager
        appName.text = try {
            activity.packageManager.getApplicationLabel(
                activity.packageManager.getApplicationInfo(config.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            config.displayName
        }

        appSwitch.isChecked = prefsManager.isAppBlockingEnabled(config.packageName)

        // 스위치를 맵에 저장 (나중에 갱신용)
        switchesByPackage[config.packageName] = appSwitch

        // Handle toggle
        itemView.setOnClickListener {
            val currentState = appSwitch.isChecked
            val newState = !currentState

            // ON → OFF 변경 시에만 검증
            if (currentState && !newState) {
                handleDisableAttempt(config, appSwitch)
            } else {
                // OFF → ON은 즉시 허용
                appSwitch.isChecked = newState
                prefsManager.setAppBlockingEnabled(config.packageName, newState)
            }
        }

        return itemView
    }

    /**
     * 차단 OFF 시도 처리
     * 최근 10분 이내 차단 시도가 있었다면 경고 다이얼로그 표시
     */
    private fun handleDisableAttempt(config: AppBlockingConfig, switch: SwitchMaterial) {
        (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
            try {
                // 최근 10분 이내 차단 시도 확인
                val recentAttempts = statisticsRepository.getRecentSessionCountByApp(
                    packageName = config.packageName,
                    minutes = 10
                )

                if (recentAttempts > 0) {
                    // 차단 시도 있음 → 경고 다이얼로그 표시
                    withContext(Dispatchers.Main) {
                        showRecentAttemptWarning(config, switch)
                    }
                } else {
                    // 차단 시도 없음 → 즉시 차단 해제
                    withContext(Dispatchers.Main) {
                        switch.isChecked = false
                        prefsManager.setAppBlockingEnabled(config.packageName, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppBlockingSettings", "Failed to check recent attempts", e)
                // 에러 발생 시 안전하게 허용 (사용자 불편 최소화)
                withContext(Dispatchers.Main) {
                    switch.isChecked = false
                    prefsManager.setAppBlockingEnabled(config.packageName, false)
                }
            }
        }
    }

    /**
     * 최근 차단 시도 경고 다이얼로그 표시
     */
    private fun showRecentAttemptWarning(
        config: AppBlockingConfig,
        switch: SwitchMaterial
    ) {
        // 앱 이름 가져오기
        val appName = try {
            activity.packageManager.getApplicationLabel(
                activity.packageManager.getApplicationInfo(config.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            config.displayName
        }

        WarningDialog(
            context = activity,
            titleResId = R.string.app_disable_warning_title,
            messageResId = R.string.app_disable_warning_message,
            messageArgs = arrayOf(appName),
            positiveTextResId = R.string.app_disable_confirm,
            negativeTextResId = R.string.app_disable_cancel,
            onPositive = {
                // 사용자 확인 → 타이머 시작 요청
                onRequestTimer(config.packageName)
            },
            onNegative = {
                // 사용자 취소 → 스위치 ON 상태 유지 (아무 작업 안함)
            }
        ).show()
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            activity.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 모든 스위치 상태를 갱신
     * 타이머 완료 후 MainActivity에서 호출됨
     */
    fun refreshSwitches() {
        switchesByPackage.forEach { (packageName, switch) ->
            val isEnabled = prefsManager.isAppBlockingEnabled(packageName)
            if (switch.isChecked != isEnabled) {
                switch.isChecked = isEnabled
                Log.d("AppBlockingSettings", "Switch updated: $packageName = $isEnabled")
            }
        }
    }
}
