package dev.personal.linkgrab.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "Bold Sticker" design tokens. See docs/DESIGN.md. */
object Sticker {
    val Yellow = Color(0xFFFFD93B)
    val Ink = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Pink = Color(0xFFFF6B9D)
    val Blue = Color(0xFF2F5BFF)
    val DisabledFill = Color(0xFFD9D9D9)
    val DisabledText = Color(0xFF4D4D4D)
    val Track = Color(0xFFE6E6E6)
    val Border = 3.dp
}

/** Hard, un-blurred offset shadow (Android elevation shadows are always blurred). */
fun Modifier.stickerShadow(
    radius: Dp = 16.dp,
    offset: Dp = 4.dp,
    color: Color = Sticker.Ink
): Modifier = this.drawBehind {
    val o = offset.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(o, o),
        size = size,
        cornerRadius = CornerRadius(radius.toPx())
    )
}

/** Fill + 3dp black border + hard shadow. Apply before padding. */
fun Modifier.sticker(
    background: Color = Sticker.White,
    radius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = Sticker.Ink
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .stickerShadow(radius, shadowOffset, shadowColor)
        .background(background, shape)
        .border(Sticker.Border, Sticker.Ink, shape)
}

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
    shadowColor: Color = Sticker.Ink,
    enabled: Boolean = true,
    fontSize: TextUnit = 18.sp,
    horizontalPadding: Dp = 18.dp,
    maxLines: Int = 1,
    selected: Boolean? = null
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .heightIn(min = height)
            .sticker(
                background = if (enabled) container else Sticker.DisabledFill,
                radius = radius,
                shadowOffset = shadowOffset,
                shadowColor = shadowColor
            )
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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

/** Selectable toggle built on StickerButton: blue when selected, white otherwise. */
@Composable
fun StickerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Sticker.Blue,
    selectedContent: Color = Sticker.White,
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

/** Small white status pill with a border and no shadow. */
@Composable
fun StickerPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Sticker.Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .sticker(Sticker.White, 20.dp, 0.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
