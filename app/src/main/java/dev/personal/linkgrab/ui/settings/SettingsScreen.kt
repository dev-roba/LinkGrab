package dev.personal.linkgrab.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.yausername.youtubedl_android.YoutubeDL
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.data.InstagramCookieStore
import dev.personal.linkgrab.data.YtDlpUpdater
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerCard
import dev.personal.linkgrab.ui.theme.StickerChip
import dev.personal.linkgrab.ui.theme.StickerSectionTitle
import dev.personal.linkgrab.ui.theme.ThemeMode
import dev.personal.linkgrab.ui.theme.ThemePrefs
import dev.personal.linkgrab.ui.theme.stickerSwitchColors
import dev.personal.linkgrab.util.DiagnosticsExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val RELEASES_URL = "https://github.com/dev-roba/LinkGrab/releases/latest"

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as LinkGrabApp).settingsRepository
    val settings by repository.settings.collectAsState(initial = dev.personal.linkgrab.model.AppSettings())
    val scope = rememberCoroutineScope()

    var updateMessage by remember { mutableStateOf<String?>(null) }
    var engineVersion by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var instagramCookies by remember { mutableStateOf(InstagramCookieStore.load(context)) }
    var cookiesSaved by remember { mutableStateOf(false) }
    var showInstagram by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    LaunchedEffect(Unit) {
        engineVersion = withContext(Dispatchers.IO) {
            runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("SETTINGS", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
        }

        // ---------- DOWNLOADS ----------
        item { StickerSectionTitle("Downloads") }
        item {
            StickerCard {
                SettingRow("Default audio format", settings.audioFormat.name)
                ChipRow {
                    ChipOption("M4A", settings.audioFormat == AudioFormat.M4A) {
                        scope.launch { repository.setAudioFormat(AudioFormat.M4A) }
                    }
                    ChipOption("MP3", settings.audioFormat == AudioFormat.MP3) {
                        scope.launch { repository.setAudioFormat(AudioFormat.MP3) }
                    }
                }
                RowDivider()
                SettingRow("Video quality", "Best up to ${settings.qualityCeiling}p")
                ChipRow {
                    ChipOption("720p", settings.qualityCeiling == 720) {
                        scope.launch { repository.setQuality(720) }
                    }
                    ChipOption("1080p", settings.qualityCeiling == 1080) {
                        scope.launch { repository.setQuality(1080) }
                    }
                }
            }
        }

        // ---------- APPEARANCE ----------
        item { StickerSectionTitle("Appearance") }
        item {
            StickerCard {
                SettingRow("Theme", "Light is yellow, dark is black with yellow ink")
                ChipRow {
                    ChipOption("AUTO", ThemePrefs.mode == ThemeMode.SYSTEM) { ThemePrefs.set(context, ThemeMode.SYSTEM) }
                    ChipOption("LIGHT", ThemePrefs.mode == ThemeMode.LIGHT) { ThemePrefs.set(context, ThemeMode.LIGHT) }
                    ChipOption("DARK", ThemePrefs.mode == ThemeMode.DARK) { ThemePrefs.set(context, ThemeMode.DARK) }
                }
            }
        }

        // ---------- ENGINE ----------
        item { StickerSectionTitle("Engine") }
        item {
            StickerCard {
                SettingRow(
                    title = "Automatically update yt-dlp",
                    subtitle = "Checks when you open the app and weekly in the background",
                    trailing = {
                        Switch(
                            checked = settings.autoUpdateYtDlp,
                            onCheckedChange = { scope.launch { repository.setAutoYtDlpUpdates(it) } },
                            colors = stickerSwitchColors()
                        )
                    }
                )
                RowDivider()
                SettingRow(
                    title = "Update yt-dlp now",
                    subtitle = updateMessage ?: "Engine version: ${engineVersion ?: "unknown"}",
                    trailing = {
                        SmallButton(if (updating) "WORKING" else "UPDATE", enabled = !updating) {
                            scope.launch {
                                updating = true
                                updateMessage = when (val result = YtDlpUpdater.update(context)) {
                                    YtDlpUpdater.Result.Updated -> "yt-dlp updated"
                                    YtDlpUpdater.Result.Current -> "yt-dlp is already current"
                                    is YtDlpUpdater.Result.Failed -> result.message
                                }
                                engineVersion = withContext(Dispatchers.IO) {
                                    runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull()
                                }
                                updating = false
                            }
                        }
                    }
                )
            }
        }

        // ---------- INSTAGRAM ----------
        item { StickerSectionTitle("Instagram") }
        item {
            StickerCard(background = Sticker.Pink) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Instagram session", color = Sticker.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Instagram blocks anonymous downloads. Paste your browser's instagram.com cookie header " +
                            "(sessionid, csrftoken) so yt-dlp can authenticate. Keep it secret: it grants access to your account.",
                        color = Sticker.Black,
                        fontSize = 13.sp
                    )
                    if (!showInstagram) {
                        SmallButton(
                            text = if (instagramCookies.isBlank()) "SET UP" else "EDIT",
                            container = Sticker.Black,
                            content = Color.White
                        ) { showInstagram = true }
                    } else {
                        OutlinedTextField(
                            value = instagramCookies,
                            onValueChange = { instagramCookies = it; cookiesSaved = false },
                            label = { Text("Instagram cookies (name=value; …)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Sticker.Black,
                                unfocusedTextColor = Sticker.Black,
                                focusedBorderColor = Sticker.Black,
                                unfocusedBorderColor = Sticker.Black,
                                focusedLabelColor = Sticker.Black,
                                unfocusedLabelColor = Sticker.Black,
                                cursorColor = Sticker.Black
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallButton("SAVE", container = Sticker.Black, content = Color.White) {
                                InstagramCookieStore.save(context, instagramCookies)
                                cookiesSaved = true
                            }
                            SmallButton("CLEAR", container = Color.White, content = Sticker.Black) {
                                InstagramCookieStore.clear(context)
                                instagramCookies = ""
                                cookiesSaved = false
                            }
                            if (cookiesSaved) Text("Saved", color = Sticker.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ---------- SHARING & UPDATES ----------
        item { StickerSectionTitle("Share and updates") }
        item {
            StickerCard {
                SettingRow(
                    title = "Check for app updates",
                    subtitle = "Opens the latest release on GitHub",
                    trailing = {
                        SmallButton("OPEN") {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))) }
                        }
                    }
                )
                RowDivider()
                SettingRow(
                    title = "Send this app",
                    subtitle = "Share the LinkGrab APK over Quick Share or Bluetooth",
                    trailing = {
                        SmallButton("SHARE") {
                            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
                            val apk = File(shareDir, "LinkGrab.apk")
                            context.applicationInfo.sourceDir.let { File(it).copyTo(apk, overwrite = true) }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .setType("application/vnd.android.package-archive")
                                        .putExtra(Intent.EXTRA_STREAM, uri)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    "Send LinkGrab"
                                )
                            )
                        }
                    }
                )
            }
        }

        // ---------- HELP & ABOUT ----------
        item { StickerSectionTitle("Help and about") }
        item {
            StickerCard {
                SettingRow(
                    title = "Export diagnostics",
                    subtitle = "Shares redacted technical details; URLs and personal data are excluded",
                    trailing = {
                        SmallButton("EXPORT") {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, DiagnosticsExporter.report(context)),
                                    "Share diagnostics"
                                )
                            )
                        }
                    }
                )
                RowDivider()
                SettingRow(
                    title = "Manage storage",
                    subtitle = "Free space is checked before downloads",
                    trailing = {
                        SmallButton("OPEN") {
                            context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                        }
                    }
                )
                RowDivider()
                SettingRow(
                    title = "LinkGrab $versionName",
                    subtitle = "yt-dlp ${engineVersion ?: "unknown"}"
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Sticker.Ink)
    )
}

@Composable
private fun ChipRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ChipOption(label: String, selected: Boolean, onClick: () -> Unit) {
    StickerChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        height = 44.dp,
        fontSize = 14.sp
    )
}

@Composable
private fun SmallButton(
    text: String,
    enabled: Boolean = true,
    container: Color = Sticker.Ink,
    content: Color = Sticker.Yellow,
    onClick: () -> Unit
) {
    StickerButton(
        text = text,
        onClick = onClick,
        container = container,
        content = content,
        enabled = enabled,
        height = 44.dp,
        radius = 12.dp,
        shadowOffset = 3.dp,
        fontSize = 14.sp,
        horizontalPadding = 14.dp
    )
}
