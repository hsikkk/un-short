package com.muuu.unshort.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.muuu.unshort.R
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.service.blocking.AppBlockingConfig
import com.muuu.unshort.service.blocking.AppBlockingRegistry

/**
 * Dialog for managing per-app blocking settings
 */
class AppBlockingSettingsDialog(
    private val context: Context,
    private val onSettingsChanged: () -> Unit = {}
) {
    private val prefsManager = PreferencesManager(context)

    fun show() {
        val installedConfigs = AppBlockingRegistry.ALL_CONFIGS
            .filter { isAppInstalled(it.packageName) }

        if (installedConfigs.isEmpty()) {
            // No apps installed - show message
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.app_blocking_settings_title)
                .setMessage(R.string.app_blocking_settings_no_apps)
                .setPositiveButton(R.string.app_blocking_settings_close, null)
                .show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_app_blocking_settings, null
        )

        val container = dialogView.findViewById<LinearLayout>(R.id.appsContainer)

        installedConfigs.forEach { config ->
            val itemView = createSwitchItem(config)
            container.addView(itemView)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.app_blocking_settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.app_blocking_settings_close) { dialog, _ ->
                onSettingsChanged()
                dialog.dismiss()
            }
            .show()
    }

    private fun createSwitchItem(config: AppBlockingConfig): LinearLayout {
        val itemView = LayoutInflater.from(context).inflate(
            R.layout.item_app_blocking_switch, null
        ) as LinearLayout

        val appIcon = itemView.findViewById<ImageView>(R.id.appIcon)
        val appName = itemView.findViewById<TextView>(R.id.appName)
        val appSwitch = itemView.findViewById<SwitchMaterial>(R.id.appSwitch)

        // Get actual app icon from PackageManager
        val icon = try {
            context.packageManager.getApplicationIcon(config.packageName)
        } catch (e: Exception) {
            ResourcesCompat.getDrawable(context.resources, config.iconResId, null)
        }

        appIcon.setImageDrawable(icon)

        // Get actual app name from PackageManager
        appName.text = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(config.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            config.displayName
        }

        appSwitch.isChecked = prefsManager.isAppBlockingEnabled(config.packageName)

        // Handle toggle
        itemView.setOnClickListener {
            val newState = !appSwitch.isChecked
            appSwitch.isChecked = newState
            prefsManager.setAppBlockingEnabled(config.packageName, newState)
        }

        return itemView
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
