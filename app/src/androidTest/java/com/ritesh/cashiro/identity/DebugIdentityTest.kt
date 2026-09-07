package com.ritesh.cashiro.identity

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritesh.cashiro.BuildConfig
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.data.preferences.AppIcon
import com.ritesh.cashiro.utils.IconSwitchingUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugIdentityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun debugIdentityAndProvidersAreIsolated() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals("com.ritesh.cashiro.debug", context.packageName)
        assertEquals("Cashiro Debug", context.applicationInfo.loadLabel(context.packageManager).toString())
        for (suffix in listOf("fileprovider", "androidx-startup")) {
            val provider = context.packageManager.resolveContentProvider("${context.packageName}.$suffix", 0)
            assertNotNull(provider)
            assertEquals(context.packageName, provider!!.packageName)
        }
    }

    @Test fun launcherShortcutsTargetDebugNotRelease() {
        val shortcuts = context.getSystemService(ShortcutManager::class.java).manifestShortcuts
        assertEquals(3, shortcuts.size)
        shortcuts.forEach {
            val component = it.intent!!.component!!
            assertEquals(context.packageName, component.packageName)
            assertEquals(MainActivity::class.java.name, component.className)
        }
    }

    @Test fun iconSwitchUsesNamespaceClassInsideDebugPackage() {
        try {
            IconSwitchingUtils.switchAppIcon(context, AppIcon.ZENITH)
            val alias = ComponentName(context, "${MainActivity::class.java.name}Zenith")
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                context.packageManager.getComponentEnabledSetting(alias))
        } finally {
            IconSwitchingUtils.switchAppIcon(context, AppIcon.ORIGINAL)
        }
    }
}
