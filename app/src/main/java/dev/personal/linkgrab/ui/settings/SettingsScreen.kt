package dev.personal.linkgrab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.data.YtDlpUpdater
import dev.personal.linkgrab.data.InstagramCookieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yausername.youtubedl_android.YoutubeDL
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import dev.personal.linkgrab.util.DiagnosticsExporter
import android.provider.Settings

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as LinkGrabApp).settingsRepository
    val settings by repository.settings.collectAsState(initial = dev.personal.linkgrab.model.AppSettings())
    val scope = rememberCoroutineScope()
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var engineVersion by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { engineVersion = withContext(Dispatchers.IO) { runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull() } }
    var instagramCookies by remember { mutableStateOf(InstagramCookieStore.load(context)) }
    var cookiesSaved by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item { ListItem(headlineContent = { Text("Default audio format") }, supportingContent = { Text(settings.audioFormat.name) }, trailingContent = { TextButton(onClick = { scope.launch { repository.setAudioFormat(if (settings.audioFormat == AudioFormat.M4A) AudioFormat.MP3 else AudioFormat.M4A) } }) { Text("Change") } }) }
        item { ListItem(headlineContent = { Text("Video quality") }, supportingContent = { Text("Best up to ${settings.qualityCeiling}p") }, trailingContent = { TextButton(onClick = { scope.launch { repository.setQuality(if (settings.qualityCeiling == 1080) 720 else 1080) } }) { Text("Change") } }) }
        item { ListItem(headlineContent = { Text("Automatically update yt-dlp") }, trailingContent = { Switch(settings.autoUpdateYtDlp, { scope.launch { repository.setAutoYtDlpUpdates(it) } }) }) }
        item { ListItem(headlineContent = { Text("Update yt-dlp now") }, supportingContent = { Text(updateMessage ?: "Engine version: ${engineVersion ?: "unknown"}") }, trailingContent = { Button(enabled = !updating, onClick = { scope.launch { updating = true; updateMessage = when (val result = YtDlpUpdater.update(context)) { YtDlpUpdater.Result.Updated -> "yt-dlp updated"; YtDlpUpdater.Result.Current -> "yt-dlp is already current"; is YtDlpUpdater.Result.Failed -> result.message }; engineVersion = withContext(Dispatchers.IO) { runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull() }; updating = false } }) { Text(if (updating) "Updating…" else "Update") } }) }
        item { Text("Instagram session", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        item { Text("Instagram blocks anonymous downloads. Paste your browser's instagram.com cookie header (sessionid, csrftoken) so yt-dlp can authenticate. Keep it secret — it grants access to your account.", style = MaterialTheme.typography.bodySmall) }
        item { OutlinedTextField(value = instagramCookies, onValueChange = { instagramCookies = it; cookiesSaved = false }, label = { Text("Instagram cookies (name=value; …)") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { InstagramCookieStore.save(context, instagramCookies); cookiesSaved = true }) { Text("Save") }; TextButton(onClick = { InstagramCookieStore.clear(context); instagramCookies = ""; cookiesSaved = false }) { Text("Clear") }; if (cookiesSaved) Text("Saved", color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically)) } }
        item { ListItem(headlineContent = { Text("Check for app updates") }, supportingContent = { Text("Checks GitHub Releases when configured") }, trailingContent = { Button(onClick = {}) { Text("Check") } }) }
        item { ListItem(headlineContent = { Text("Send this app") }, supportingContent = { Text("Share the LinkGrab APK over Quick Share or Bluetooth") }, trailingContent = { Button(onClick = {
            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val apk = File(shareDir, "LinkGrab.apk")
            context.applicationInfo.sourceDir.let { File(it).copyTo(apk, overwrite = true) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Send LinkGrab"))
        }) { Text("Share") } }) }
        item { ListItem(headlineContent = { Text("Export diagnostics") }, supportingContent = { Text("Shares redacted technical details; URLs and personal data are excluded") }, trailingContent = { Button(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, DiagnosticsExporter.report(context)), "Share diagnostics")) }) { Text("Export") } }) }
        item { ListItem(headlineContent = { Text("Manage storage") }, supportingContent = { Text("Free space is checked before downloads") }, trailingContent = { TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }) { Text("Open") } }) }
    }
}
