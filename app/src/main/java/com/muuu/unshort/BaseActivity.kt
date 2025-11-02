package com.muuu.unshort

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Base Activity that handles common edge-to-edge setup and system bars insets
 *
 * All Activities should extend this class to get automatic:
 * - Edge-to-edge display support
 * - System bars (status bar + navigation bar) padding on root layout
 * - Font scale normalization
 *
 * Override methods:
 * - isLightStatusBar(): Customize status bar icons (default: true/dark icons for white backgrounds)
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupStatusBarIcons()
        applySystemBarsInsets()
    }

    /**
     * Override to customize status bar icons appearance
     * true = dark icons (for light backgrounds) - DEFAULT
     * false = light icons (for dark backgrounds)
     * Default: true (dark icons for white appbar)
     */
    protected open fun isLightStatusBar(): Boolean = true

    private fun setupStatusBarIcons() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightStatusBar()
        }
    }

    private fun applySystemBarsInsets() {
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom
            )
            windowInsets
        }
    }
}
