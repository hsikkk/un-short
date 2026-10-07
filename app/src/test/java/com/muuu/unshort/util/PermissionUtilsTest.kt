package com.muuu.unshort.util

import android.app.Application
import android.content.ComponentName
import android.content.ContextWrapper
import android.provider.Settings
import com.muuu.unshort.ShortsBlockService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PermissionUtilsTest {
    // Release package permits abbreviated class names; debug uses a different app ID.
    private val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
        override fun getPackageName() = "com.muuu.unshort"
    }

    @Test fun acceptsBothComponentFormatsInAServiceList() {
        val component = ComponentName(context, ShortsBlockService::class.java)
        for (name in listOf(component.flattenToString(), component.flattenToShortString())) {
            Settings.Secure.putString(context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "other/.Service:$name")
            assertTrue(name, PermissionUtils.isAccessibilityServiceEnabled(context))
        }
    }

    @Test fun rejectsMissingAndSimilarlyNamedServices() {
        for (name in listOf(null, "", "com.muuu.unshort/com.muuu.unshort.ShortsBlockServiceOther")) {
            Settings.Secure.putString(context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, name)
            assertFalse(PermissionUtils.isAccessibilityServiceEnabled(context))
        }
    }
}
