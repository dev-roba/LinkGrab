package dev.personal.linkgrab.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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

private val StickerColorScheme = lightColorScheme(
    primary = Sticker.Ink,
    onPrimary = Sticker.Yellow,
    primaryContainer = Sticker.Blue,
    onPrimaryContainer = Sticker.White,
    secondary = Sticker.Blue,
    onSecondary = Sticker.White,
    secondaryContainer = Sticker.Blue,
    onSecondaryContainer = Sticker.White,
    tertiary = Sticker.Pink,
    onTertiary = Sticker.Ink,
    background = Sticker.Yellow,
    onBackground = Sticker.Ink,
    surface = Sticker.White,
    onSurface = Sticker.Ink,
    surfaceVariant = Sticker.White,
    onSurfaceVariant = Color(0xFF222222),
    surfaceContainerLowest = Sticker.White,
    surfaceContainerLow = Sticker.White,
    surfaceContainer = Sticker.White,
    surfaceContainerHigh = Sticker.White,
    surfaceContainerHighest = Sticker.White,
    outline = Sticker.Ink,
    outlineVariant = Sticker.Ink,
    error = Color(0xFFB3261E),
    onError = Sticker.White,
    errorContainer = Sticker.Pink,
    onErrorContainer = Sticker.Ink
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
 * The Bold Sticker look is always light (yellow ground, black ink), so dark mode and
 * dynamic color are intentionally ignored. The parameters stay so existing callers compile.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LinkGrabTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = StickerColorScheme,
        typography = LinkGrabTypography,
        shapes = StickerShapes,
        content = content
    )
}
