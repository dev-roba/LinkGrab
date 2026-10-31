package dev.personal.linkgrab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerCard
import dev.personal.linkgrab.ui.theme.sticker

/** "Link found. GRAB?" banner shown when the clipboard holds a link. */
@Composable
fun ClipboardBanner(
    link: String,
    onGrab: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pop = remember { Animatable(0.85f) }
    LaunchedEffect(link) {
        pop.snapTo(0.85f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    StickerCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(pop.value),
        background = Sticker.Blue
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("LINK FOUND. GRAB?", color = Sticker.OnBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = shortLink(link),
                    color = Sticker.OnBlue,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StickerButton(
                text = "GRAB",
                onClick = onGrab,
                container = Sticker.Black,
                content = Color(0xFFFFD93B),
                height = 44.dp,
                radius = 12.dp,
                shadowOffset = 3.dp,
                shadowColor = Sticker.Black,
                fontSize = 15.sp,
                horizontalPadding = 14.dp,
                haptic = true
            )
            StickerButton(
                text = "X",
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Dismiss" },
                container = Color.White,
                content = Sticker.Black,
                height = 44.dp,
                radius = 12.dp,
                shadowOffset = 3.dp,
                shadowColor = Sticker.Black,
                fontSize = 15.sp,
                horizontalPadding = 12.dp
            )
        }
    }
}

private fun shortLink(link: String): String =
    link.removePrefix("https://").removePrefix("http://").removePrefix("www.").take(48)

/** Playful loader: an arrow that bounces down onto a tray, with the text FETCHING… */
@Composable
fun FetchingArrow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fetching")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val ink = Sticker.Ink
    val tray = Sticker.Pink

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val w = size.width
            val h = size.height
            val dy = bounce * h * 0.18f
            val arrow = Path().apply {
                moveTo(w * 0.40f, h * 0.08f + dy)
                lineTo(w * 0.60f, h * 0.08f + dy)
                lineTo(w * 0.60f, h * 0.42f + dy)
                lineTo(w * 0.78f, h * 0.42f + dy)
                lineTo(w * 0.50f, h * 0.74f + dy)
                lineTo(w * 0.22f, h * 0.42f + dy)
                lineTo(w * 0.40f, h * 0.42f + dy)
                close()
            }
            drawPath(arrow, color = ink)
            drawLine(
                color = tray,
                start = Offset(w * 0.18f, h * 0.9f),
                end = Offset(w * 0.82f, h * 0.9f),
                strokeWidth = h * 0.08f,
                cap = StrokeCap.Round
            )
        }
        Text("FETCHING…", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    }
}

/** Big tilted SAVED! sticker that pops in the middle of the screen while [visible]. */
@Composable
fun SavedSticker(visible: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            scale.snapTo(0.3f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            scale.animateTo(0f, tween(durationMillis = 150))
        }
    }
    if (scale.value > 0.01f) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "SAVED!",
                color = Sticker.Black,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .scale(scale.value)
                    .rotate(-8f)
                    .sticker(Sticker.Pink, 20.dp, 6.dp, Sticker.Black)
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            )
        }
    }
}
