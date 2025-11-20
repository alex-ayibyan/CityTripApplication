package edu.ap.citytripapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import edu.ap.citytripapplication.ui.theme.Orange100

private val DarkColorScheme = darkColorScheme(
    primary = Turquoise300,
    onPrimary = Turquoise900,
    primaryContainer = Turquoise700,
    onPrimaryContainer = Turquoise100,
    
    secondary = Orange300,
    onSecondary = Orange900,
    secondaryContainer = Orange700,
    onSecondaryContainer = Orange100,
    
    tertiary = Purple300,
    onTertiary = Purple900,
    tertiaryContainer = Purple700,
    onTertiaryContainer = Purple100,
    
    error = DeepOrange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

private val LightColorScheme = lightColorScheme(
    primary = Turquoise600,
    onPrimary = Color.White,
    primaryContainer = Turquoise100,
    onPrimaryContainer = Turquoise900,
    
    secondary = Orange600,
    onSecondary = Color.White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange900,
    
    tertiary = Purple600,
    onTertiary = Color.White,
    tertiaryContainer = Purple100,
    onTertiaryContainer = Purple900,
    
    error = DeepOrange,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Turquoise50,
)

@Composable
fun CityTripApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to use our vibrant colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}