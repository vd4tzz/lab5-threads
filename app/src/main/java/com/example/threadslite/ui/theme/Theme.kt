package com.example.threadslite.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Threads Lite colour scheme — intentionally monochrome.
 * Only Material3 roles that are visually meaningful for this design are overridden.
 */
private val ThreadsColorScheme = lightColorScheme(
    primary            = Black,          // Primary actions (buttons, FAB)
    onPrimary          = White,          // Text/icons on primary
    primaryContainer   = Grey100,        // Chip/tag backgrounds
    onPrimaryContainer = Black,

    secondary          = Grey600,        // Secondary actions
    onSecondary        = White,
    secondaryContainer = Grey200,
    onSecondaryContainer = Black,

    background         = OffWhite,       // App background
    onBackground       = Black,

    surface            = White,          // Cards, sheets
    onSurface          = Black,
    surfaceVariant     = Grey100,        // Alternate card shade
    onSurfaceVariant   = Grey600,

    outline            = Grey200,        // Borders, dividers
    outlineVariant     = Grey400,

    error              = ErrorRed,
    onError            = White,
)

@Composable
fun ThreadsLiteTheme(
    content: @Composable () -> Unit
) {
    // Always use light mode — the design is fixed black-on-white
    val colorScheme = ThreadsColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent so Compose controls it
            window.statusBarColor = OffWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ThreadsTypography,
        content     = content
    )
}
