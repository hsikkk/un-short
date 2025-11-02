package com.muuu.unshort.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.muuu.unshort.R

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
                context.getString(R.string.device_admin_explanation)
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
