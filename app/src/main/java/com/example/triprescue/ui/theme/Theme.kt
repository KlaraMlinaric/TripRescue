package com.example.triprescue.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF04DA1A9),
    secondary = Color(0xFF7E99A3),
    tertiary = Color(0xFF2E5077),
    background = Color(0xFF28282B),
    surface = Color(0xFFC0C0C0),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF121212),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    scrim = Color(0xFF7E99A3), //for buttons homep
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4DA1A9),
    secondary = Color(0xFF2E5077),
    tertiary = Color(0xFF2E5077),
    background = Color(0xFFF6F4F0),
    surface = Color(0xFFEBEBEB),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF121212),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
    scrim = Color(0xFF7E99A3),
)


@Composable
fun TripRescueTheme(
    darkTheme: Boolean =isSystemInDarkTheme( ),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context =LocalContext.current
            if (darkTheme)dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window =(view.context as Activity).window
            window.statusBarColor =colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window,  view).isAppearanceLightStatusBars = !darkTheme
        } }

    MaterialTheme(
        colorScheme =colorScheme,
        typography = Typography,
        content = content
    )
}

