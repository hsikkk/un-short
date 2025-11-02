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
 * - Status bar color and appearance configuration
 * - System bars (status bar + navigation bar) padding on root layout
 * - Font scale normalization
 *
 * Override methods:
 * - getStatusBarColor(): Customize status bar color (default: Black)
 * - isLightStatusBar(): Customize status bar icons (default: false/light icons)
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
        setupStatusBar()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applySystemBarsInsets()
    }

    /**
     * Override to customize status bar color
     * Default: Black
     */
    protected open fun getStatusBarColor(): Int = Color.BLACK

    /**
     * Override to customize status bar icons appearance
     * false = light icons (for dark backgrounds)
     * true = dark icons (for light backgrounds)
     * Default: false (light icons)
     */
    protected open fun isLightStatusBar(): Boolean = false

    private fun setupStatusBar() {
        window.statusBarColor = getStatusBarColor()
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
