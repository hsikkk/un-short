package com.muuu.unshort.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.muuu.unshort.util.PermissionUtils
import com.muuu.unshort.ShortsBlockService
import com.muuu.unshort.R

/**
 * 권한 체크 및 설정 관련 유틸리티
 */
object PermissionUtils {

    /**
     * 제조사 타입
     */
    enum class Manufacturer {
        SAMSUNG,
        LG,
        XIAOMI,
        HUAWEI,
        OPPO,
        VIVO,
        OTHER
    }

    /**
     * 현재 기기의 제조사 확인
     */
    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("lg") -> Manufacturer.LG
            manufacturer.contains("xiaomi") -> Manufacturer.XIAOMI
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            else -> Manufacturer.OTHER
        }
    }

    /**
     * 제조사별 추가 안내가 필요한지 확인
     * 기본 Android나 일부 제조사는 앱이 바로 보이므로 안내 불필요
     */
    fun needsAccessibilityGuide(): Boolean {
        return when (getManufacturer()) {
            Manufacturer.SAMSUNG,
            Manufacturer.LG,
            Manufacturer.XIAOMI,
            Manufacturer.HUAWEI,
            Manufacturer.OPPO,
            Manufacturer.VIVO -> true
            Manufacturer.OTHER -> false // 기본 Android는 바로 보임
        }
    }

    /**
     * 제조사별 접근성 설정 경로 안내 문구 가져오기
     */
    fun getAccessibilityGuide(context: Context): String {
        return when (getManufacturer()) {
            Manufacturer.SAMSUNG -> context.getString(R.string.permission_accessibility_guide_samsung)
            Manufacturer.LG -> context.getString(R.string.permission_accessibility_guide_lg)
            Manufacturer.XIAOMI -> context.getString(R.string.permission_accessibility_guide_xiaomi)
            Manufacturer.HUAWEI -> context.getString(R.string.permission_accessibility_guide_huawei)
            Manufacturer.OPPO -> context.getString(R.string.permission_accessibility_guide_oppo)
            Manufacturer.VIVO -> context.getString(R.string.permission_accessibility_guide_vivo)
            Manufacturer.OTHER -> context.getString(R.string.permission_accessibility_guide_default)
        }
    }

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
