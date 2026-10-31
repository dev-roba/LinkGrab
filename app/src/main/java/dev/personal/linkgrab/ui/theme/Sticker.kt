package dev.personal.linkgrab.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bold Sticker tokens, theme-aware (light = yellow ground / black ink, dark = black ground / yellow ink).
 * Names kept from v1: Yellow = screen ground, Ink = borders and text, White = card fill.
 * See docs/DESIGN.md.
 */
@Immutable
data class StickerPalette(
    val ground: Color,
    val ink: Color,
    val card: Color,
    val shadow: Color,
    val track: Color,
    val disabledFill: Color,
    val disabledText: Color
)

val LightStickerPalette = StickerPalette(
    ground = Color(0xFFFFD93B),
    ink = Color(0xFF000000),
    card = Color(0xFFFFFFFF),
    shadow = Color(0xFF000000),
    track = Color(0xFFE6E6E6),
    disabledFill = Color(0xFFD9D9D9),
    disabledText = Color(0xFF4D4D4D)
)

val DarkStickerPalette = StickerPalette(
    ground = Color(0xFF121212),
    ink = Color(0xFFFFD93B),
    card = Color(0xFF1E1E1E),
    shadow = Color(0xFFFF6B9D),
    track = Color(0xFF3A3A3A),
    disabledFill = Color(0xFF2A2A2A),
    disabledText = Color(0xFFA6A6A6)
)

val LocalStickerPalette = staticCompositionLocalOf { LightStickerPalette }

object Sticker {
    val Yellow: Color
        @Composable get() = LocalStickerPalette.current.ground
    val Ink: Color
        @Composable get() = LocalStickerPalette.current.ink
    val White: Color
        @Composable get() = LocalStickerPalette.current.card
    val Track: Color
        @Composable get() = LocalStickerPalette.current.track
    val DisabledFill: Color
        @Composable get() = LocalStickerPalette.current.disabledFill
    val DisabledText: Color
        @Composable get() = LocalStickerPalette.current.disabledText

    // Accent colors never change between themes, so text drawn on them is fixed too.
    val Pink = Color(0xFFFF6B9D)
    val Blue = Color(0xFF2F5BFF)
    val Black = Color(0xFF000000)
    val OnBlue = Color(0xFFFFFFFF)
    val Border = 3.dp
}

/** Hard, un-blurred offset shadow (Android elevation shadows are always blurred). */
fun Modifier.stickerShadow(
    radius: Dp = 16.dp,
    offset: Dp = 4.dp,
    color: Color = Color.Unspecified
): Modifier = composed {
    val shadowColor = if (color == Color.Unspecified) LocalStickerPalette.current.shadow else color
    Modifier.drawBehind {
        val o = offset.toPx()
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(o, o),
            size = size,
            cornerRadius = CornerRadius(radius.toPx())
        )
    }
}

/** Fill + 3dp border + hard shadow. Apply before padding. Unspecified colors follow the theme. */
fun Modifier.sticker(
    background: Color = Color.Unspecified,
    radius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = Color.Unspecified
): Modifier = composed {
    val palette = LocalStickerPalette.current
    val shape = RoundedCornerShape(radius)
    Modifier
        .stickerShadow(radius, shadowOffset, shadowColor)
        .background(if (background == Color.Unspecified) palette.card else background, shape)
        .border(Sticker.Border, palette.ink, shape)
}

/**
 * Main button. Press feel: while pressed the button slides down-right onto its shadow
 * (the shadow shrinks to nothing), like pushing a physical key.
 */
@Composable
fun StickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Sticker.Ink,
    content: Color = Sticker.Yellow,
    height: Dp = 56.dp,
    radius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    fontSize: TextUnit = 18.sp,
    horizontalPadding: Dp = 18.dp,
    maxLines: Int = 1,
    selected: Boolean? = null,
    haptic: Boolean = false
) {
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed && enabled) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 60),
        label = "sticker-press"
    )
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .heightIn(min = height)
            .offset { IntOffset(sink.roundToPx(), sink.roundToPx()) }
            .sticker(
                background = if (enabled) container else Sticker.DisabledFill,
                radius = radius,
                shadowOffset = shadowOffset - sink,
                shadowColor = shadowColor
            )
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button
            ) {
                if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { if (selected != null) this.selected = selected }
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) content else Sticker.DisabledText,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Selectable toggle built on StickerButton: blue when selected, card color otherwise. */
@Composable
fun StickerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Sticker.Blue,
    selectedContent: Color = Sticker.OnBlue,
    height: Dp = 48.dp,
    fontSize: TextUnit = 15.sp
) {
    StickerButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        container = if (selected) selectedColor else Sticker.White,
        content = if (selected) selectedContent else Sticker.Ink,
        height = height,
        radius = 14.dp,
        shadowOffset = 3.dp,
        fontSize = fontSize,
        horizontalPadding = 14.dp,
        selected = selected
    )
}

/** Small status pill with a border and no shadow. */
@Composable
fun StickerPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified
) {
    Text(
        text = text,
        color = if (textColor == Color.Unspecified) Sticker.Ink else textColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .sticker(background, 20.dp, 0.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Pill that pops in (spring) whenever [text] changes while [highlight] is true. */
@Composable
fun PopPill(text: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(text, highlight) {
        if (highlight) {
            scale.snapTo(0.6f)
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        } else {
            scale.snapTo(1f)
        }
    }
    StickerPill(
        text = text,
        modifier = modifier.scale(scale.value),
        background = if (highlight) Sticker.Pink else Color.Unspecified,
        textColor = if (highlight) Sticker.Black else Color.Unspecified
    )
}

/** Bordered card with the hard shadow. */
@Composable
fun StickerCard(
    modifier: Modifier = Modifier,
    background: Color = Color.Unspecified,
    radius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sticker(background, radius, shadowOffset),
        content = content
    )
}

@Composable
fun StickerSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.5.sp,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
fun stickerSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Sticker.Yellow,
    checkedTrackColor = Sticker.Ink,
    checkedBorderColor = Sticker.Ink,
    uncheckedThumbColor = Sticker.Ink,
    uncheckedTrackColor = Sticker.White,
    uncheckedBorderColor = Sticker.Ink
)
