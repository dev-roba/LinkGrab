package dev.personal.linkgrab.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * To use Space Grotesk later: add the .ttf files to res/font and replace this with
 * FontFamily(Font(R.font.space_grotesk_medium, FontWeight.Medium), Font(R.font.space_grotesk_bold, FontWeight.Bold)).
 */
val LinkGrabFont: FontFamily = FontFamily.SansSerif

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Remembers the user's light/dark choice. Loaded in LinkGrabApp.onCreate (and again, harmlessly, by the theme). */
object ThemePrefs {
    private const val FILE = "linkgrab_ui"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val stored = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, null)
        mode = runCatching { ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, value: ThemeMode) {
        loaded = true
        mode = value
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value.name)
            .apply()
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFD93B),
    primaryContainer = Color(0xFF2F5BFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF2F5BFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2F5BFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF6B9D),
    onTertiary = Color(0xFF000000),
    background = Color(0xFFFFD93B),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF222222),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF6B9D),
    onErrorContainer = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFD93B),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2F5BFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF2F5BFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2F5BFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF6B9D),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFD93B),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFD93B),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFFFE680),
    surfaceContainerLowest = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    outline = Color(0xFFFFD93B),
    outlineVariant = Color(0xFFFFD93B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFFF6B9D),
    onErrorContainer = Color(0xFF000000)
)

private val BaseTypography = Typography()

private val LinkGrabTypography = Typography(
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp
    ),
    titleLarge = BaseTypography.titleLarge.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Bold
    ),
    titleMedium = BaseTypography.titleMedium.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Bold
    ),
    bodyLarge = BaseTypography.bodyLarge.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Medium
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Medium
    ),
    bodySmall = BaseTypography.bodySmall.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Medium
    ),
    labelLarge = BaseTypography.labelLarge.copy(
        fontFamily = LinkGrabFont, fontWeight = FontWeight.Bold
    )
)

private val StickerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

/**
 * Light (yellow ground) or dark (black ground) Bold Sticker theme.
 * [darkTheme] null means "follow the user's Settings choice" (Auto / Light / Dark).
 * [dynamicColor] is ignored on purpose; the parameter stays so older callers compile.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LinkGrabTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { ThemePrefs.ensureLoaded(context) }

    val dark = darkTheme ?: when (ThemePrefs.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalStickerPalette provides (if (dark) DarkStickerPalette else LightStickerPalette)
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = LinkGrabTypography,
            shapes = StickerShapes,
            content = content
        )
    }
}
