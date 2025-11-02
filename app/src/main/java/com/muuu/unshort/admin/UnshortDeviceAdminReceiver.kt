package com.muuu.unshort.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.muuu.unshort.R

/**
 * Device Admin Receiver for preventing app uninstallation
 *
 * This receiver handles Device Admin events and provides protection
 * against impulsive app uninstallation.
 */
class UnshortDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
        // Toast는 Activity에서 처리
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device Admin disable requested")
        return context.getString(R.string.device_admin_receiver_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
        // Toast는 사용자가 설정 화면으로 돌아왔을 때 자동으로 OFF 표시됨
    }

    companion object {
        private const val TAG = "UnshortDeviceAdmin"
    }
}
