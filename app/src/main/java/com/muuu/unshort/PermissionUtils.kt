package com.muuu.unshort

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * 권한 체크 및 설정 관련 유틸리티
 */
object PermissionUtils {

    /**
     * 접근성 서비스 활성화 여부 확인
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains("${context.packageName}/${ShortsBlockService::class.java.name}") == true
        }

        return false
    }

    /**
     * 오버레이 권한 활성화 여부 확인
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 모든 필수 권한이 활성화되어 있는지 확인
     */
    fun hasAllPermissions(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context) && canDrawOverlays(context)
    }

    /**
     * 접근성 설정 화면 열기
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * 오버레이 권한 설정 화면 열기
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}
