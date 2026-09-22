package dev.personal.linkgrab

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SPIKE-ONLY debug activity. Not part of the product UI.
 * Usage: adb shell am start -n dev.personal.linkgrab/.SpikeTestActivity \
 *   --es url "https://..." --es mode info|download
 * Writes results to /sdcard/Download/linkgrab_spike_result.txt
 */
class SpikeTestActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url")
        val mode = intent.getStringExtra("mode") ?: "info"
        // Keep the activity alive (visible) while work runs so the process is
        // not frozen by the cached-app freezer; finish() only when done.
        // (NoDisplay + immediate finish() caused the process to be frozen.)

        if (url == null) {
            writeResult("ERROR: no url extra provided")
            finish()
            return
        }

        val appContext = applicationContext
        scope.launch {
            val result = try {
                ensureYoutubeDL(appContext)
                val instance = YoutubeDL.getInstance()
                when (mode) {
                    "info" -> {
                        val info = instance.getInfo(url)
                        "INFO_OK\ntitle=${info.title}\nurl=$url"
                    }
                    "download" -> {
                        val outputDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                            ?: File(appContext.filesDir, "downloads")
                        outputDir.mkdirs()
                        val request = YoutubeDLRequest(url)
                        request.addOption("-o", outputDir.absolutePath + "/%(title)s.%(ext)s")
                        instance.execute(request, null)
                        val files = outputDir.listFiles()?.map { it.name } ?: emptyList()
                        "DOWNLOAD_OK\nurl=$url\nfiles=${files.joinToString(",")}"
                    }
                    else -> "ERROR: unknown mode $mode"
                }
            } catch (t: Throwable) {
                "ERROR\nurl=$url\nmode=$mode\ntype=${t.javaClass.name}\nmsg=${t.message}"
            }
            writeResult(result)
            finish()
        }
    }

    private fun writeResult(text: String) {
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "linkgrab_spike")
            dir.mkdirs()
            val f = File(dir, "result.txt")
            f.writeText(text)
        } catch (t: Throwable) {
            // fallback to app-private storage
            try {
                File(filesDir, "spike_result.txt").writeText(text)
            } catch (_: Throwable) {}
        }
    }
}