package dev.personal.linkgrab.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadType
import java.util.UUID

object DownloadLaterScheduler {
    fun schedule(context: Context, url: String, type: DownloadType, audio: AudioFormat, quality: Int): UUID {
        val request = OneTimeWorkRequestBuilder<DownloadLaterWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresCharging(true).build())
            .setInputData(workDataOf(DownloadLaterWorker.KEY_URL to url, DownloadLaterWorker.KEY_TYPE to type.name, DownloadLaterWorker.KEY_AUDIO to audio.name, DownloadLaterWorker.KEY_QUALITY to quality))
            .addTag("linkgrab-scheduled")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("linkgrab-${UUID.randomUUID()}", ExistingWorkPolicy.KEEP, request)
        return request.id
    }
}
