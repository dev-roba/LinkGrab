package dev.personal.linkgrab.service

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.personal.linkgrab.model.AudioFormat

class DownloadLaterWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val type = runCatching { dev.personal.linkgrab.model.DownloadType.valueOf(inputData.getString(KEY_TYPE) ?: "VIDEO") }.getOrDefault(dev.personal.linkgrab.model.DownloadType.VIDEO)
        val audio = runCatching { AudioFormat.valueOf(inputData.getString(KEY_AUDIO) ?: "MP3") }.getOrDefault(AudioFormat.MP3)
        val quality = inputData.getInt(KEY_QUALITY, 720)
        DownloadService.enqueue(applicationContext, url, type, audio, quality)
        return Result.success()
    }
    companion object { const val KEY_URL = "url"; const val KEY_TYPE = "type"; const val KEY_AUDIO = "audio"; const val KEY_QUALITY = "quality" }
}
