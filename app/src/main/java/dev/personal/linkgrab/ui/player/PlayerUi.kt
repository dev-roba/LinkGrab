package dev.personal.linkgrab.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** Compose-friendly view of a [Player]: every field is snapshot state, so the UI updates itself. */
@Stable
class PlayerUi internal constructor(private val player: Player) {
    var isPlaying by mutableStateOf(false)
        private set
    var playbackState by mutableStateOf(Player.STATE_IDLE)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var mediaId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var thumbnail by mutableStateOf<String?>(null)
        private set
    var isVideo by mutableStateOf(false)
        private set
    var videoAspect by mutableStateOf(16f / 9f)
        private set
    var speed by mutableStateOf(1f)
        private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var hasNext by mutableStateOf(false)
        private set
    var hasPrevious by mutableStateOf(false)
        private set
    var hasMedia by mutableStateOf(false)
        private set
    var queueSize by mutableStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        val item = player.currentMediaItem
        val meta = player.mediaMetadata
        isPlaying = player.isPlaying
        playbackState = player.playbackState
        mediaId = item?.mediaId
        title = meta.title?.toString() ?: item?.mediaId?.substringAfterLast('/') ?: ""
        thumbnail = meta.artworkUri?.toString()
        isVideo = meta.extras?.getBoolean("video", false) ?: false
        val size = player.videoSize
        videoAspect = if (size.width > 0 && size.height > 0) {
            (size.width * size.pixelWidthHeightRatio) / size.height
        } else {
            16f / 9f
        }
        speed = player.playbackParameters.speed
        repeatMode = player.repeatMode
        shuffle = player.shuffleModeEnabled
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()
        hasMedia = item != null
        queueSize = player.mediaItemCount
        tickPosition()
    }

    fun tickPosition() {
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.coerceAtLeast(0L)
    }
}

/** Returns a live [PlayerUi] for [player] (null until the player is connected). */
@Composable
fun rememberPlayerUi(player: Player?): PlayerUi? {
    val ui = remember(player) { if (player != null) PlayerUi(player) else null }

    DisposableEffect(ui) {
        if (ui == null || player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    ui.refresh()
                }

                override fun onPlayerError(error: PlaybackException) {
                    ui.error = "This file can't be played. It may have been moved or deleted."
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    ui.error = null
                }
            }
            player.addListener(listener)
            ui.refresh()
            onDispose { player.removeListener(listener) }
        }
    }

    LaunchedEffect(ui) {
        while (ui != null) {
            ui.tickPosition()
            delay(if (ui.isPlaying) 250L else 1_000L)
        }
    }
    return ui
}
