package com.ritesh.cashiro.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
    var colorScheme =
        when {
            themeStyle == ThemeStyle.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            themeStyle == ThemeStyle.DEFAULT -> {
                if (darkTheme) getCustomDarkColorScheme(accentColor)
                else getCustomLightColorScheme(accentColor)
            }
            // Fallback
             dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
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
            shapes = Shapes,
            content = content
        )
    }
}


fun getCustomLightColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = when (accent) {
        AccentColor.ROSEWATER -> Latte_Rosewater
        AccentColor.FLAMINGO -> Latte_Flamingo
        AccentColor.PINK -> Latte_Pink
        AccentColor.MAUVE -> Latte_Mauve
        AccentColor.RED -> Latte_Red
        AccentColor.PEACH -> Latte_Peach
        AccentColor.YELLOW -> Latte_Yellow
        AccentColor.GREEN -> Latte_Green
        AccentColor.TEAL -> Latte_Teal
        AccentColor.SAPPHIRE -> Latte_Sapphire
        AccentColor.BLUE -> Latte_Blue
        AccentColor.LAVENDER -> Latte_Lavender
        AccentColor.PINE_ROSE -> Dawn_Rose
        AccentColor.PINE_IRIS -> Dawn_Iris
        AccentColor.PINE_PINE -> Dawn_Pine
        AccentColor.PINE_GOLD -> Dawn_Gold
        AccentColor.PINE_LOVE -> Dawn_Love
        AccentColor.PINE_FOAM -> Dawn_Foam
        AccentColor.PINE_MUTED -> Dawn_Muted
        AccentColor.PINE_SUBTLE -> Dawn_Subtle
        AccentColor.PINE_TEXT -> Dawn_Text
        AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight
        AccentColor.PINE_SURFACE -> Dawn_Surface
        AccentColor.PINE_OVERLAY -> Dawn_Overlay
    }
    val secondaryColor = when (accent) {
        AccentColor.ROSEWATER -> Latte_Rosewater_secondary
        AccentColor.FLAMINGO -> Latte_Flamingo_secondary
        AccentColor.PINK -> Latte_Pink_secondary
        AccentColor.MAUVE -> Latte_Mauve_secondary
        AccentColor.RED -> Latte_Red_secondary
        AccentColor.PEACH -> Latte_Peach_secondary
        AccentColor.YELLOW -> Latte_Yellow_secondary
        AccentColor.GREEN -> Latte_Green_secondary
        AccentColor.TEAL -> Latte_Teal_secondary
        AccentColor.SAPPHIRE -> Latte_Sapphire_secondary
        AccentColor.BLUE -> Latte_Blue_secondary
        AccentColor.LAVENDER -> Latte_Lavender_secondary
        AccentColor.PINE_ROSE -> Dawn_Rose_secondary
        AccentColor.PINE_IRIS -> Dawn_Iris_secondary
        AccentColor.PINE_PINE -> Dawn_Pine_secondary
        AccentColor.PINE_GOLD -> Dawn_Gold_secondary
        AccentColor.PINE_LOVE -> Dawn_Love_secondary
        AccentColor.PINE_FOAM -> Dawn_Foam_secondary
        AccentColor.PINE_MUTED -> Dawn_Muted_secondary
        AccentColor.PINE_SUBTLE -> Dawn_Subtle_secondary
        AccentColor.PINE_TEXT -> Dawn_Text_secondary
        AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight_secondary
        AccentColor.PINE_SURFACE -> Dawn_Surface_secondary
        AccentColor.PINE_OVERLAY -> Dawn_Overlay_secondary
    }

    val tertiaryColor = when (accent) {
        AccentColor.ROSEWATER -> Latte_Rosewater_tertiary
        AccentColor.FLAMINGO -> Latte_Flamingo_tertiary
        AccentColor.PINK -> Latte_Pink_tertiary
        AccentColor.MAUVE -> Latte_Mauve_tertiary
        AccentColor.RED -> Latte_Red_tertiary
        AccentColor.PEACH -> Latte_Peach_tertiary
        AccentColor.YELLOW -> Latte_Yellow_tertiary
        AccentColor.GREEN -> Latte_Green_tertiary
        AccentColor.TEAL -> Latte_Teal_tertiary
        AccentColor.SAPPHIRE -> Latte_Sapphire_tertiary
        AccentColor.BLUE -> Latte_Blue_tertiary
        AccentColor.LAVENDER -> Latte_Lavender_tertiary
        AccentColor.PINE_ROSE -> Dawn_Rose_tertiary
        AccentColor.PINE_IRIS -> Dawn_Iris_tertiary
        AccentColor.PINE_PINE -> Dawn_Pine_tertiary
        AccentColor.PINE_GOLD -> Dawn_Gold_tertiary
        AccentColor.PINE_LOVE -> Dawn_Love_tertiary
        AccentColor.PINE_FOAM -> Dawn_Foam_tertiary
        AccentColor.PINE_MUTED -> Dawn_Muted_tertiary
        AccentColor.PINE_SUBTLE -> Dawn_Subtle_tertiary
        AccentColor.PINE_TEXT -> Dawn_Text_tertiary
        AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight_tertiary
        AccentColor.PINE_SURFACE -> Dawn_Surface_tertiary
        AccentColor.PINE_OVERLAY -> Dawn_Overlay_tertiary
    }

    val onPrimaryColor = when (accent) {
        AccentColor.YELLOW -> Color(0xFF1a1b20)

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE,
        AccentColor.PINE_LOVE, AccentColor.PINE_FOAM, AccentColor.PINE_MUTED, AccentColor.PINE_SUBTLE,
        AccentColor.PINE_TEXT,  AccentColor.PINE_SURFACE,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base

        AccentColor.PINE_ROSE, AccentColor.PINE_GOLD,
        AccentColor.PINE_HIGHLIGHT-> Dawn_OnBackground
        else -> Color(0xFFffffff)
    }

    val onSecondaryColor = when (accent) {
        AccentColor.YELLOW -> Color(0xFF1a1b20)
        AccentColor.PINE_ROSE,  AccentColor.PINE_GOLD,
        AccentColor.PINE_LOVE -> Dawn_OnBackground

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE, AccentColor.PINE_MUTED,
        AccentColor.PINE_SUBTLE, AccentColor.PINE_TEXT, AccentColor.PINE_FOAM,
        AccentColor.PINE_HIGHLIGHT, AccentColor.PINE_SURFACE,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base
        else -> Color(0xFFffffff)
    }

    val onTertiaryColor = when (accent) {
        AccentColor.YELLOW, AccentColor.ROSEWATER, AccentColor.FLAMINGO -> Color(0xFF1a1b20)

        AccentColor.PINE_ROSE, AccentColor.PINE_HIGHLIGHT, -> Dawn_OnBackground

        AccentColor.PINE_IRIS, AccentColor.PINE_PINE,   AccentColor.PINE_GOLD,
        AccentColor.PINE_MUTED, AccentColor.PINE_LOVE,
        AccentColor.PINE_SUBTLE, AccentColor.PINE_FOAM,
        AccentColor.PINE_SURFACE, AccentColor.PINE_TEXT,
        AccentColor.PINE_OVERLAY -> Dawn_Surface_Base
        else -> Color(0xFFffffff)
    }

    val isPine = accent.name.startsWith("PINE_")

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryContainer = primaryColor,
        onPrimaryContainer = onPrimaryColor,
        inversePrimary = Color(0xFF000000),
        secondary = secondaryColor,
        onSecondary = onSecondaryColor,
        secondaryContainer = secondaryColor,
        onSecondaryContainer = onSecondaryColor,
        tertiary = tertiaryColor,
        onTertiary = onTertiaryColor,
        tertiaryContainer = tertiaryColor,
        onTertiaryContainer = onTertiaryColor,
        background = if (isPine) Dawn_Background else Color(0xFFe2e2e9),
        onBackground = if (isPine) Dawn_OnBackground else Color(0xFF1a1b20),
        surface = if (isPine) Dawn_Background else Color(0xFFE5E5EA),
        onSurface = if (isPine) Dawn_OnSurface else Color(0xFF1a1b20),
        surfaceVariant = if (isPine) Dawn_SurfaceVariant else Color(0xFFc4c6d0),
        onSurfaceVariant = if (isPine) Dawn_OnSurfaceVariant else Color(0xFF44474f),
        inverseSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        inverseOnSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFf0f0f7),
        error = if (isPine) Dawn_Love else Latte_Red,
        onError = Color(0xFFffffff),
        errorContainer = if (isPine) Dawn_Love else Latte_Red,
        onErrorContainer = Color(0xFFffffff),
        surfaceBright = if (isPine) Dawn_Surface_Base else Color(0xFFE8E9EC),
        surfaceDim = if (isPine) Dawn_SurfaceVariant else Color(0xFFd9d9e0),
        surfaceContainer = if (isPine) Dawn_Background else Color(0xFFf9f9ff),
        surfaceContainerHigh = if (isPine) Dawn_SurfaceVariant else Color(0xFFe8e7ee),
        surfaceContainerHighest = if (isPine) Color(0xFFE6DDD5) else Color(0xFFe2e2e9),
        surfaceContainerLow = if (isPine) Dawn_Surface_Base else Color(0xFFffffff),
        surfaceContainerLowest = Color(0xFFf9f9ff)
    )
}

fun getCustomDarkColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = when (accent) {
        AccentColor.PINE_ROSE -> RosePine_Rose
        AccentColor.PINE_IRIS -> RosePine_Iris
        AccentColor.PINE_PINE -> RosePine_Pine
        AccentColor.PINE_GOLD -> RosePine_Gold
        AccentColor.PINE_LOVE -> RosePine_Love
        AccentColor.PINE_FOAM -> RosePine_Foam
        AccentColor.PINE_MUTED -> RosePine_Muted
        AccentColor.PINE_SUBTLE -> RosePine_Subtle
        AccentColor.PINE_TEXT -> RosePine_Text
        AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight
        AccentColor.PINE_SURFACE -> RosePine_Surface
        AccentColor.PINE_OVERLAY -> RosePine_Overlay
        AccentColor.ROSEWATER -> Macchiato_Rosewater_dim
        AccentColor.FLAMINGO -> Macchiato_Flamingo_dim
        AccentColor.PINK -> Macchiato_Pink_dim
        AccentColor.MAUVE -> Macchiato_Mauve_dim
        AccentColor.RED -> Macchiato_Red_dim
        AccentColor.PEACH -> Macchiato_Peach_dim
        AccentColor.YELLOW -> Macchiato_Yellow_dim
        AccentColor.GREEN -> Macchiato_Green_dim
        AccentColor.TEAL -> Macchiato_Teal_dim
        AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim
        AccentColor.BLUE -> Macchiato_Blue_dim
        AccentColor.LAVENDER -> Macchiato_Lavender_dim
    }

    val secondaryColor = when (accent) {
        AccentColor.PINE_ROSE -> RosePine_Rose_secondary
        AccentColor.PINE_IRIS -> RosePine_Iris_secondary
        AccentColor.PINE_PINE -> RosePine_Pine_secondary
        AccentColor.PINE_GOLD -> RosePine_Gold_secondary
        AccentColor.PINE_LOVE -> RosePine_Love_secondary
        AccentColor.PINE_FOAM -> RosePine_Foam_secondary
        AccentColor.PINE_MUTED -> RosePine_Muted_secondary
        AccentColor.PINE_SUBTLE -> RosePine_Subtle_secondary
        AccentColor.PINE_TEXT -> RosePine_Text_secondary
        AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight_secondary
        AccentColor.PINE_SURFACE -> RosePine_Surface_secondary
        AccentColor.PINE_OVERLAY -> RosePine_Overlay_secondary
        AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_secondary
        AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_secondary
        AccentColor.PINK -> Macchiato_Pink_dim_secondary
        AccentColor.MAUVE -> Macchiato_Mauve_dim_secondary
        AccentColor.RED -> Macchiato_Red_dim_secondary
        AccentColor.PEACH -> Macchiato_Peach_dim_secondary
        AccentColor.YELLOW -> Macchiato_Yellow_dim_secondary
        AccentColor.GREEN -> Macchiato_Green_dim_secondary
        AccentColor.TEAL -> Macchiato_Teal_dim_secondary
        AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_secondary
        AccentColor.BLUE -> Macchiato_Blue_dim_secondary
        AccentColor.LAVENDER -> Macchiato_Lavender_dim_secondary
    }

    val tertiaryColor = when (accent) {
        AccentColor.PINE_ROSE -> RosePine_Rose_tertiary
        AccentColor.PINE_IRIS -> RosePine_Iris_tertiary
        AccentColor.PINE_PINE -> RosePine_Pine_tertiary
        AccentColor.PINE_GOLD -> RosePine_Gold_tertiary
        AccentColor.PINE_LOVE -> RosePine_Love_tertiary
        AccentColor.PINE_FOAM -> RosePine_Foam_tertiary
        AccentColor.PINE_MUTED -> RosePine_Muted_tertiary
        AccentColor.PINE_SUBTLE -> RosePine_Subtle_tertiary
        AccentColor.PINE_TEXT -> RosePine_Text_tertiary
        AccentColor.PINE_HIGHLIGHT -> RosePine_Highlight_tertiary
        AccentColor.PINE_SURFACE -> RosePine_Surface_tertiary
        AccentColor.PINE_OVERLAY -> RosePine_Overlay_tertiary
        AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_tertiary
        AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_tertiary
        AccentColor.PINK -> Macchiato_Pink_dim_tertiary
        AccentColor.MAUVE -> Macchiato_Mauve_dim_tertiary
        AccentColor.RED -> Macchiato_Red_dim_tertiary
        AccentColor.PEACH -> Macchiato_Peach_dim_tertiary
        AccentColor.YELLOW -> Macchiato_Yellow_dim_tertiary
        AccentColor.GREEN -> Macchiato_Green_dim_tertiary
        AccentColor.TEAL -> Macchiato_Teal_dim_tertiary
        AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_tertiary
        AccentColor.BLUE -> Macchiato_Blue_dim_tertiary
        AccentColor.LAVENDER -> Macchiato_Lavender_dim_tertiary
    }

    val onPrimaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color.White
        else -> Color(0xFF1a1b20) // Dark text for bright pastel accents
    }

    val onSecondaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_TEXT, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color(0xFF1a1b20) // Pine secondary is Foam (bright)
        else -> Color(0xFF1a1b20) 
    }

    val onTertiaryColor = when (accent) {
        AccentColor.BLUE -> Color.White
        AccentColor.PINE_PINE, AccentColor.PINE_MUTED, AccentColor.PINE_SURFACE, AccentColor.PINE_OVERLAY -> Color.White
        else -> Color(0xFF1a1b20) 
    }

    val isPine = accent.name.startsWith("PINE_")

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryContainer = primaryColor,
        onPrimaryContainer = onPrimaryColor,
        inversePrimary = Color(0xFFFFFFFF),
        secondary = secondaryColor,
        onSecondary = onSecondaryColor,
        secondaryContainer = secondaryColor,
        onSecondaryContainer = onSecondaryColor,
        tertiary = tertiaryColor,
        onTertiary = onTertiaryColor,
        tertiaryContainer = tertiaryColor,
        onTertiaryContainer = onTertiaryColor,
        background = if (isPine) RosePine_Background else Color(0xFF111318),
        onBackground = if (isPine) RosePine_OnBackground else Color(0xFFe2e2e9),
        surface = if (isPine) RosePine_Background else Color(0xFF111318),
        onSurface = if (isPine) RosePine_OnSurface else Color(0xFFe2e2e9),
        surfaceVariant = if (isPine) RosePine_SurfaceVariant else Color(0xFF1e1f25),
        onSurfaceVariant = if (isPine) RosePine_OnSurfaceVariant else Color(0xFFc4c6d0),
        inverseSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFe2e2e9),
        inverseOnSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        error = if (isPine) RosePine_Love else Macchiato_Red_dim,
        onError = Color(0xFFffffff),
        errorContainer = if (isPine) RosePine_Love else Macchiato_Red_dim,
        onErrorContainer = Color(0xFFffffff),
        surfaceBright = if (isPine) RosePine_SurfaceVariant else Color(0xFF37393e),
        surfaceDim = if (isPine) RosePine_Background else Color(0xFF0c0e13),
        surfaceContainer = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerHigh = if (isPine) RosePine_SurfaceVariant else Color(0xFF282a2f),
        surfaceContainerHighest = if (isPine) Color(0xFF403D52) else Color(0xFF33353a),
        surfaceContainerLow = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerLowest = if (isPine) RosePine_Background else Color(0xFF1a1b20)
    )
}
