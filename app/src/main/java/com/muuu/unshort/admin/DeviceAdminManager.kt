package com.muuu.unshort.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Manager class for Device Admin operations
 *
 * This class provides methods to check and request Device Admin permissions
 * for app uninstall protection.
 */
class DeviceAdminManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName =
        ComponentName(context, UnshortDeviceAdminReceiver::class.java)

    /**
     * Check if Device Admin is currently active
     */
    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    /**
     * Request user to activate Device Admin
     *
     * This will show a system dialog explaining the Device Admin permission.
     */
    fun requestActivation(activity: Activity, requestCode: Int = REQUEST_CODE_ENABLE_ADMIN) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "un:short는 Device Admin 권한을 사용해 충동적인 앱 삭제를 방지합니다.\n\n" +
                "이 기능은 의지가 약해지는 순간에 도움이 됩니다.\n\n" +
                "언제든지 설정 > 보안 > 기기 관리 앱에서 비활성화할 수 있습니다."
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Remove Device Admin programmatically
     *
     * Note: This is typically used for testing/development.
     * Users should deactivate through Settings > Security.
     */
    fun removeAdmin() {
        if (isDeviceAdminActive()) {
            devicePolicyManager.removeActiveAdmin(adminComponent)
        }
    }

    companion object {
        const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }
}
