package dev.personal.linkgrab

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.service.DownloadService
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.concurrent.thread
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DebugDiagnosticsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thread(name = "linkgrab-diagnostics") {
            try {
                when (intent.getStringExtra("mode") ?: "version") {
                    "speed" -> runSpeedProbe()
                    "download_video" -> runDownload(intent.getStringExtra("url"), false)
                    "download_audio" -> runDownload(intent.getStringExtra("url"), true)
                    "service_audio" -> runServiceAudio(intent.getStringExtra("url"), intent.getStringExtra("format"))
                    else -> runYtDlp(intent.getStringExtra("mode") ?: "version", intent.getStringExtra("url"))
                }
            } catch (t: Throwable) {
                Log.e(TAG, "diagnostic_failed type=${t.javaClass.name} message=${t.message}", t)
            } finally {
                runOnUiThread { finish() }
            }
        }
    }

    private fun runYtDlp(mode: String, url: String?) {
        val initStart = System.nanoTime()
        Log.d(TAG, "init_start_ns=$initStart mode=$mode")
        ensureYoutubeDL(applicationContext)
        val initEnd = System.nanoTime()
        Log.d(TAG, "init_end_ns=$initEnd init_ms=${(initEnd - initStart) / 1_000_000}")

        val request = if (mode == "version") {
            YoutubeDLRequest(emptyList()).addCommands(listOf("--version"))
        } else if (mode == "extractors") {
            YoutubeDLRequest(emptyList()).addCommands(listOf("--list-extractors"))
        } else {
            require(!url.isNullOrBlank()) { "url required" }
            YoutubeDLRequest(url).addCommands(buildList {
                add("-v")
                add("--simulate")
                add("--no-playlist")
                when (mode) {
                    "ipv4" -> add("--force-ipv4")
                    "timeout" -> { add("--socket-timeout"); add("10") }
                }
            })
        }
        Log.d(TAG, "execute_start_ns=${System.nanoTime()} phase=$mode")
        val response = YoutubeDL.getInstance().execute(request)
        Log.d(TAG, "execute_end_ns=${System.nanoTime()} exit=${response.exitCode} elapsed_ms=${response.elapsedTime}")
        Log.d(TAG, "stdout_begin\n${response.out}\nstdout_end")
        Log.d(TAG, "stderr_begin\n${response.err}\nstderr_end")
    }

    private fun runSpeedProbe() {
        Log.d(TAG, "speed_probe_start_ns=${System.nanoTime()}")
        probe("https://www.youtube.com")
        probe("https://www.tiktok.com")
        val url = URL("https://speed.cloudflare.com/__down?bytes=10000000")
        val start = System.nanoTime()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            val connected = System.nanoTime()
            var total = 0L
            connection.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                }
            }
            val end = System.nanoTime()
            val seconds = (end - start) / 1_000_000_000.0
            Log.d(TAG, "download url=cloudflare bytes=$total connect_ms=${(connected - start) / 1_000_000} total_ms=${(end - start) / 1_000_000} mbps=${(total * 8.0 / seconds) / 1_000_000}")
        } finally {
            connection.disconnect()
        }
    }

    private fun runDownload(url: String?, audio: Boolean) {
        require(!url.isNullOrBlank()) { "url required" }
        val outputDir = getExternalFilesDir(if (audio) android.os.Environment.DIRECTORY_MUSIC else android.os.Environment.DIRECTORY_MOVIES)!!
            .apply { mkdirs() }
        val request = YoutubeDLRequest(url).addCommands(buildList {
            add("--no-playlist")
            add("--socket-timeout"); add("15")
            add("--retries"); add("3")
            add("--fragment-retries"); add("3")
            add("--extractor-args"); add("youtube:player_client=android")
            add("-o"); add(java.io.File(outputDir, "%(title)s.%(ext)s").absolutePath)
            if (audio) {
                add("-f"); add("ba[ext=m4a]/ba")
            } else {
                add("-f"); add("bv*[vcodec^=avc1][height<=720]+ba[ext=m4a]/b[ext=mp4]")
                add("--merge-output-format"); add("mp4")
            }
        })
        Log.d(TAG, "download_start audio=$audio ns=${System.nanoTime()}")
        val response = YoutubeDL.getInstance().execute(request)
        Log.d(TAG, "download_end exit=${response.exitCode} elapsed_ms=${response.elapsedTime}")
        Log.d(TAG, "stdout_begin\n${response.out}\nstdout_end")
        Log.d(TAG, "stderr_begin\n${response.err}\nstderr_end")
        outputDir.listFiles()?.forEach { Log.d(TAG, "output path=${it.absolutePath} bytes=${it.length()}") }
    }

    private fun runServiceAudio(url: String?, format: String?) {
        require(!url.isNullOrBlank()) { "url required" }
        val audio = AudioFormat.valueOf(format ?: AudioFormat.MP3.name)
        DownloadService.enqueue(this, url, DownloadType.AUDIO, audio, 720)
        Log.d(TAG, "service_audio_enqueued format=$audio")
        Thread.sleep(45_000)
    }

    private fun probe(value: String) {
        val start = System.nanoTime()
        val host = URL(value).host
        val dnsStart = System.nanoTime()
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { InetAddress.getAllByName(host) }.get(15, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            Log.d(TAG, "http url=$value dns_timeout_ms=15000")
            return
        } finally {
            executor.shutdownNow()
        }
        val dnsEnd = System.nanoTime()
        val connection = (URL(value).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            requestMethod = "GET"
        }
        try {
            connection.connect()
            val connected = System.nanoTime()
            connection.inputStream.use { it.read() }
            val firstByte = System.nanoTime()
            Log.d(TAG, "http url=$value dns_ms=${(dnsEnd - dnsStart) / 1_000_000} connect_ms=${(connected - dnsEnd) / 1_000_000} ttfb_ms=${(firstByte - start) / 1_000_000} status=${connection.responseCode}")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "LinkGrabYtdlp"
    }
}
