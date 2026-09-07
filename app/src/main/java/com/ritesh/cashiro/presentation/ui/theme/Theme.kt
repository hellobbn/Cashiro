package com.ritesh.cashiro.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.ritesh.cashiro.data.preferences.AppFont
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.data.preferences.AccentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBlurEffects = staticCompositionLocalOf { true }


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CashiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    isAmoledMode: Boolean = false,
    accentColor: AccentColor = AccentColor.BLUE,
    appFont: AppFont = AppFont.SYSTEM,
    blurEffects: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Read the current system resources on recomposition instead of caching a wallpaper palette.
    var colorScheme = if (themeStyle == ThemeStyle.DYNAMIC && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) getCustomDarkColorScheme(accentColor) else getCustomLightColorScheme(accentColor)
    }

    // Apply Amoled Black if enabled in Dark Mode
    if (darkTheme && isAmoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        SideEffect {
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Enforce transparent system bars for edge-to-edge on O+

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            // Control whether status bar icons should be dark or light
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val fontFamily = when (appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.SN_PRO -> SNProFontFamily
    }

    // CJK glyphs already occupy a full em; Latin tracking creates uneven Chinese text.
    val useCjkSpacing = LocalConfiguration.current.locales[0].language in setOf("zh", "ja", "ko")

    CompositionLocalProvider(LocalBlurEffects provides blurEffects) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = getTypography(fontFamily = fontFamily, useCjkSpacing = useCjkSpacing),
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}
