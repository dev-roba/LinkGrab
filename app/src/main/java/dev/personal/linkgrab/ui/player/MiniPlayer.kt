package dev.personal.linkgrab.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import dev.personal.linkgrab.playback.PlayerConnection
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerCard

/** Sticker bar with the current title, a thin progress line, play/pause and close. Tap it to open the player. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val player = PlayerConnection.player
    val ui = rememberPlayerUi(player)
    if (player == null || ui == null || !ui.hasMedia) return

    val fraction = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val trackShape = RoundedCornerShape(4.dp)

    StickerCard(
        modifier = modifier
            .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
            .clickable(onClickLabel = "Open player", onClick = onOpen),
        shadowOffset = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EqBars(playing = ui.isPlaying, color = Sticker.Ink)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ui.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(trackShape)
                        .background(Sticker.Track)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(Sticker.Blue)
                    )
                }
            }
            StickerIconButton(
                icon = if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                description = if (ui.isPlaying) "Pause" else "Play",
                onClick = {
                    if (ui.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0, 0L)
                        player.play()
                    } else if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                },
                size = 44.dp,
                container = Sticker.Ink,
                content = Sticker.Yellow,
                radius = 12.dp,
                shadowOffset = 3.dp
            )
            StickerIconButton(
                icon = Icons.Default.Close,
                description = "Stop and close player",
                onClick = { PlayerConnection.stop() },
                size = 44.dp,
                radius = 12.dp,
                shadowOffset = 3.dp
            )
        }
    }
}
