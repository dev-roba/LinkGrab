package dev.personal.linkgrab.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** One thing to play. [mediaUri] is the saved file's address (also used as its id). */
data class PlayItem(
    val mediaUri: String,
    val title: String,
    val thumbnailUrl: String?,
    val isVideo: Boolean
)

/**
 * App-wide handle to the player running inside [PlaybackService]. Compose reads [player], [preparing]
 * and the sleep-timer fields directly because they are snapshot state.
 */
@androidx.annotation.OptIn(UnstableApi::class)
object PlayerConnection {

    /** The controller (a Player) once connected, else null. */
    var player by mutableStateOf<Player?>(null)
        private set

    /** True from "tap play" until the queue has been handed to the player. */
    var preparing by mutableStateOf(false)
        private set

    var sleepMinutes by mutableStateOf(0)
        private set

    /** Wall time (SystemClock.elapsedRealtime) when the sleep timer fires, or 0. */
    var sleepEndsAt by mutableStateOf(0L)
        private set

    private var future: ListenableFuture<MediaController>? = null
    private val pending = mutableListOf<(Player) -> Unit>()
    private val handler = Handler(Looper.getMainLooper())
    private var sleepRunnable: Runnable? = null

    fun connect(context: Context, onReady: ((Player) -> Unit)? = null) {
        val ready = player
        if (ready != null) {
            onReady?.invoke(ready)
            return
        }
        if (onReady != null) pending.add(onReady)
        if (future != null) return

        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    player = null
                    future = null
                }
            })
            .buildAsync()
        future = f
        f.addListener({
            val controller = runCatching { f.get() }.getOrNull()
            if (controller == null) {
                future = null
                pending.clear()
            } else {
                player = controller
                val actions = pending.toList()
                pending.clear()
                actions.forEach { it(controller) }
            }
        }, ContextCompat.getMainExecutor(app))
    }

    /** Starts playing [items] from [startIndex], resuming where that file was left off. */
    fun playQueue(context: Context, items: List<PlayItem>, startIndex: Int) {
        if (items.isEmpty()) return
        val index = startIndex.coerceIn(0, items.lastIndex)
        val resumeAt = PositionStore.get(context, items[index].mediaUri)
        preparing = true
        connect(context) { p ->
            p.setMediaItems(items.map { it.toMediaItem() }, index, resumeAt)
            p.prepare()
            p.play()
            preparing = false
        }
        // Safety net so "preparing" can never get stuck.
        handler.postDelayed({ preparing = false }, 4_000L)
    }

    /** Stops playback and clears the queue (hides the mini-player). */
    fun stop() {
        setSleepTimer(0)
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
    }

    /** minutes <= 0 turns the timer off. When it fires, playback pauses. */
    fun setSleepTimer(minutes: Int) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        sleepMinutes = minutes.coerceAtLeast(0)
        if (minutes <= 0) {
            sleepEndsAt = 0L
            return
        }
        val delayMs = minutes * 60_000L
        sleepEndsAt = SystemClock.elapsedRealtime() + delayMs
        val runnable = Runnable {
            player?.pause()
            sleepMinutes = 0
            sleepEndsAt = 0L
            sleepRunnable = null
        }
        sleepRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun PlayItem.toMediaItem(): MediaItem {
        val uri = Uri.parse(mediaUri)
        val extras = Bundle().apply { putBoolean("video", isVideo) }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("LinkGrab")
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaUri)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .build()
    }
}
