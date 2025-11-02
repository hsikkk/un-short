package com.muuu.unshort.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
        Toast.makeText(
            context,
            R.string.device_admin_receiver_enabled,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device Admin disable requested")
        return context.getString(R.string.device_admin_receiver_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
        Toast.makeText(
            context,
            R.string.device_admin_receiver_disabled,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val TAG = "UnshortDeviceAdmin"
    }
}
