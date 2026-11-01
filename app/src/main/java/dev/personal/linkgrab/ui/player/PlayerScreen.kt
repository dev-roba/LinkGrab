package dev.personal.linkgrab.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.SystemClock
import android.util.Rational
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import dev.personal.linkgrab.playback.PlayerConnection
import dev.personal.linkgrab.ui.components.FetchingArrow
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerCard
import dev.personal.linkgrab.ui.theme.StickerPill
import dev.personal.linkgrab.ui.theme.sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val SLEEP_STEPS = listOf(0, 15, 30, 60)

@Composable
fun PlayerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    LaunchedEffect(Unit) { PlayerConnection.connect(context) }
    val player = PlayerConnection.player
    val ui = rememberPlayerUi(player)

    // Leave the screen if nothing is queued (e.g. the mini-player's X stopped playback).
    LaunchedEffect(ui?.hasMedia, PlayerConnection.preparing) {
        if (ui != null && !ui.hasMedia && !PlayerConnection.preparing) {
            delay(1_500L)
            if (!ui.hasMedia && !PlayerConnection.preparing) onClose()
        }
    }

    if (player == null || ui == null) {
        Box(Modifier.fillMaxSize().background(Sticker.Yellow), contentAlignment = Alignment.Center) {
            FetchingArrow()
        }
        BackHandler { onClose() }
        return
    }

    var inPip by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var interactions by remember { mutableStateOf(0) }
    var hud by remember { mutableStateOf<String?>(null) }
    var landscape by remember { mutableStateOf(false) }

    val poke: () -> Unit = {
        showControls = true
        interactions++
    }

    BackHandler { onClose() }

    // Picture-in-picture state
    DisposableEffect(activity) {
        val host = activity as? androidx.activity.ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { info -> inPip = info.isInPictureInPictureMode }
        host?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { host?.removeOnPictureInPictureModeChangedListener(listener) }
    }

    // Keep the screen awake while a video plays
    DisposableEffect(ui.isPlaying && ui.isVideo) {
        view.keepScreenOn = ui.isPlaying && ui.isVideo
        onDispose { view.keepScreenOn = false }
    }

    // Immersive full screen for video, and put orientation + brightness back on exit
    DisposableEffect(ui.isVideo, inPip) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (ui.isVideo) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { w ->
                val attrs = w.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = attrs
            }
        }
    }

    // Auto-hide the controls during video
    LaunchedEffect(showControls, ui.isPlaying, interactions, ui.isVideo) {
        if (showControls && ui.isPlaying && ui.isVideo) {
            delay(3_000L)
            showControls = false
        }
    }
    LaunchedEffect(hud) {
        if (hud != null) {
            delay(900L)
            hud = null
        }
    }

    val actions = remember(player, ui) {
        PlayerActions(
        player = player,
        ui = ui,
        onRotate = {
            landscape = !landscape
            activity?.requestedOrientation =
                if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        },
        onPip = {
            runCatching {
                activity?.enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                )
            }
        },
        poke = poke
        )
    }

    if (ui.isVideo) {
        VideoPlayer(
            ui = ui,
            actions = actions,
            inPip = inPip,
            showControls = showControls,
            hud = hud,
            onHud = { hud = it },
            onToggleControls = {
                showControls = !showControls
                interactions++
            },
            onClose = onClose
        )
    } else {
        AudioPlayer(ui = ui, actions = actions, onClose = onClose)
    }
}

/** Everything the buttons can do, in one place. */
private class PlayerActions(
    val player: Player,
    val ui: PlayerUi,
    val onRotate: () -> Unit,
    val onPip: () -> Unit,
    val poke: () -> Unit
) {
    fun togglePlay() {
        poke()
        if (ui.playbackState == Player.STATE_ENDED) {
            player.seekTo(0, 0L)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        poke()
        val duration = player.duration.coerceAtLeast(0L)
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
    }

    fun seekTo(ms: Long) {
        poke()
        player.seekTo(ms)
    }

    fun next() {
        poke()
        player.seekToNextMediaItem()
    }

    fun previous() {
        poke()
        // Like most players: restart the track first, go to the previous one if already near the start.
        if (player.currentPosition > 3_000L || !player.hasPreviousMediaItem()) player.seekTo(0L) else player.seekToPreviousMediaItem()
    }

    fun cycleSpeed() {
        poke()
        val index = SPEEDS.indexOfFirst { it == ui.speed }
        player.setPlaybackSpeed(SPEEDS[(index + 1).mod(SPEEDS.size)])
    }

    fun cycleRepeat() {
        poke()
        player.repeatMode = when (ui.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        poke()
        player.shuffleModeEnabled = !ui.shuffle
    }

    fun cycleSleep() {
        poke()
        val index = SLEEP_STEPS.indexOf(PlayerConnection.sleepMinutes).let { if (it < 0) 0 else it }
        PlayerConnection.setSleepTimer(SLEEP_STEPS[(index + 1) % SLEEP_STEPS.size])
    }
}

// ------------------------------------------------------------------------------------------------
// VIDEO
// ------------------------------------------------------------------------------------------------

@Composable
private fun VideoPlayer(
    ui: PlayerUi,
    actions: PlayerActions,
    inPip: Boolean,
    showControls: Boolean,
    hud: String?,
    onHud: (String?) -> Unit,
    onToggleControls: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember {
        mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var brightness by remember {
        mutableStateOf(
            (activity?.window?.attributes?.screenBrightness ?: -1f).let { if (it < 0f) 0.5f else it }
        )
    }
    var dragOnRight by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val left = offset.x < size.width / 2f
                        actions.seekBy(if (left) -10_000L else 10_000L)
                        onHud(if (left) "-10s" else "+10s")
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { start -> dragOnRight = start.x >= size.width / 2f },
                    onVerticalDrag = { _, dragAmount ->
                        val delta = -dragAmount / size.height.toFloat() * 1.3f
                        if (dragOnRight) {
                            volume = (volume + delta).coerceIn(0f, 1f)
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * max).roundToInt(), 0)
                            onHud("VOL ${(volume * 100).roundToInt()}%")
                        } else {
                            brightness = (brightness + delta).coerceIn(0.02f, 1f)
                            activity?.window?.let { w ->
                                val attrs = w.attributes
                                attrs.screenBrightness = brightness
                                w.attributes = attrs
                            }
                            onHud("BRIGHT ${(brightness * 100).roundToInt()}%")
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // The picture
        AndroidView(
            factory = { ctx -> SurfaceView(ctx) },
            update = { surface -> actions.player.setVideoSurfaceView(surface) },
            onRelease = { surface -> actions.player.clearVideoSurfaceView(surface) },
            modifier = Modifier.aspectRatio(ui.videoAspect)
        )

        if (!inPip) {
            hud?.let { text ->
                StickerPill(
                    text = text,
                    background = Sticker.Pink,
                    textColor = Sticker.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            ui.error?.let { message ->
                ErrorCard(message, hasNext = ui.hasNext, onSkip = { actions.next() }, onClose = onClose, modifier = Modifier.align(Alignment.Center))
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StickerIconButton(Icons.Default.KeyboardArrowDown, "Minimize player", onClose, size = 48.dp)
                        StickerPill(
                            text = ui.title,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        StickerIconButton(Icons.Default.PictureInPictureAlt, "Picture in picture", actions.onPip, size = 48.dp)
                        StickerIconButton(Icons.Default.ScreenRotation, "Rotate screen", actions.onRotate, size = 48.dp)
                    }

                    StickerCard(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 20.dp, bottom = 20.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SeekSection(ui, actions)
                            TransportRow(ui, actions, big = 64.dp)
                            ExtrasRow(ui, actions)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// AUDIO
// ------------------------------------------------------------------------------------------------

@Composable
private fun AudioPlayer(ui: PlayerUi, actions: PlayerActions, onClose: () -> Unit) {
    val breathe = rememberInfiniteTransition(label = "breathe")
    val pulse by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val thumbShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sticker.Yellow)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StickerIconButton(Icons.Default.KeyboardArrowDown, "Minimize player", onClose, size = 48.dp)
            StickerPill("NOW PLAYING")
            Spacer(Modifier.weight(1f))
            EqBars(playing = ui.isPlaying, color = Sticker.Ink)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(if (ui.isPlaying) pulse else 1f)
                    .rotate(-3f)
                    .sticker(Sticker.Pink, 24.dp, 8.dp)
                    .padding(Sticker.Border),
                contentAlignment = Alignment.Center
            ) {
                ArtworkOrNote(url = ui.thumbnail, modifier = Modifier.fillMaxSize().clip(thumbShape))
            }
        }

        ui.error?.let { message ->
            ErrorCard(message, hasNext = ui.hasNext, onSkip = { actions.next() }, onClose = onClose)
        }

        Text(
            text = ui.title,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(end = 6.dp)
        )

        StickerCard(modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SeekSection(ui, actions)
                TransportRow(ui, actions, big = 68.dp)
                ExtrasRow(ui, actions)
            }
        }
    }
}

@Composable
private fun ArtworkOrNote(url: String?, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) {
            url?.let { runCatching { URL(it).openStream().use(BitmapFactory::decodeStream) }.getOrNull() }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded.asImageBitmap(), "Artwork", modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Sticker.Blue), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Sticker.OnBlue,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SHARED PIECES
// ------------------------------------------------------------------------------------------------

@Composable
private fun SeekSection(ui: PlayerUi, actions: PlayerActions) {
    StickerSeekBar(
        positionMs = ui.positionMs,
        durationMs = ui.durationMs,
        onSeekTo = { actions.seekTo(it) },
        onInteract = actions.poke,
        fill = Sticker.Blue
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(ui.positionMs), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(formatTime(ui.durationMs), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransportRow(ui: PlayerUi, actions: PlayerActions, big: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StickerIconButton(Icons.Default.SkipPrevious, "Previous", { actions.previous() }, size = 48.dp, enabled = ui.hasPrevious || ui.positionMs > 3_000L)
        StickerIconButton(Icons.Default.Replay10, "Back 10 seconds", { actions.seekBy(-10_000L) }, size = 48.dp)
        StickerIconButton(
            icon = if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            description = if (ui.isPlaying) "Pause" else "Play",
            onClick = { actions.togglePlay() },
            size = big,
            container = Sticker.Ink,
            content = Sticker.Yellow,
            radius = 20.dp,
            shadowOffset = 6.dp,
            haptic = true
        )
        StickerIconButton(Icons.Default.Forward10, "Forward 10 seconds", { actions.seekBy(10_000L) }, size = 48.dp)
        StickerIconButton(Icons.Default.SkipNext, "Next", { actions.next() }, size = 48.dp, enabled = ui.hasNext)
    }
}

@Composable
private fun ExtrasRow(ui: PlayerUi, actions: PlayerActions) {
    // Re-draw the sleep countdown once a second while it runs.
    var now by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(PlayerConnection.sleepEndsAt) {
        while (PlayerConnection.sleepEndsAt > 0L) {
            now = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val sleepLabel = if (PlayerConnection.sleepEndsAt > 0L) {
        "SLEEP " + formatTime((PlayerConnection.sleepEndsAt - now).coerceAtLeast(0L))
    } else {
        "SLEEP"
    }
    val speedLabel = if (ui.speed % 1f == 0f) "${ui.speed.toInt()}x" else "${ui.speed}x"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StickerButton(
            text = speedLabel,
            onClick = { actions.cycleSpeed() },
            modifier = Modifier.weight(1f),
            container = if (ui.speed != 1f) Sticker.Blue else Sticker.White,
            content = if (ui.speed != 1f) Sticker.OnBlue else Sticker.Ink,
            height = 44.dp, radius = 12.dp, shadowOffset = 3.dp, fontSize = 14.sp, horizontalPadding = 8.dp
        )
        StickerButton(
            text = sleepLabel,
            onClick = { actions.cycleSleep() },
            modifier = Modifier.weight(1.6f),
            container = if (PlayerConnection.sleepEndsAt > 0L) Sticker.Blue else Sticker.White,
            content = if (PlayerConnection.sleepEndsAt > 0L) Sticker.OnBlue else Sticker.Ink,
            height = 44.dp, radius = 12.dp, shadowOffset = 3.dp, fontSize = 14.sp, horizontalPadding = 8.dp
        )
        StickerIconButton(
            icon = if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
            description = "Repeat mode",
            onClick = { actions.cycleRepeat() },
            size = 44.dp,
            container = if (ui.repeatMode != Player.REPEAT_MODE_OFF) Sticker.Blue else Sticker.White,
            content = if (ui.repeatMode != Player.REPEAT_MODE_OFF) Sticker.OnBlue else Sticker.Ink,
            radius = 12.dp,
            shadowOffset = 3.dp
        )
        if (ui.queueSize > 1) {
            StickerIconButton(
                icon = Icons.Default.Shuffle,
                description = "Shuffle",
                onClick = { actions.toggleShuffle() },
                size = 44.dp,
                container = if (ui.shuffle) Sticker.Blue else Sticker.White,
                content = if (ui.shuffle) Sticker.OnBlue else Sticker.Ink,
                radius = 12.dp,
                shadowOffset = 3.dp
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    hasNext: Boolean,
    onSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    StickerCard(modifier = modifier.padding(24.dp), background = Sticker.Pink) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CAN'T PLAY THIS", color = Sticker.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(message, color = Sticker.Black, fontSize = 14.sp, textAlign = TextAlign.Start)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasNext) {
                    StickerButton(
                        text = "SKIP",
                        onClick = onSkip,
                        container = Sticker.Black,
                        content = Color(0xFFFFD93B),
                        shadowColor = Sticker.Black,
                        height = 44.dp, radius = 12.dp, shadowOffset = 3.dp, fontSize = 14.sp
                    )
                }
                StickerButton(
                    text = "BACK",
                    onClick = onClose,
                    container = Color.White,
                    content = Sticker.Black,
                    shadowColor = Sticker.Black,
                    height = 44.dp, radius = 12.dp, shadowOffset = 3.dp, fontSize = 14.sp
                )
            }
        }
    }
}
