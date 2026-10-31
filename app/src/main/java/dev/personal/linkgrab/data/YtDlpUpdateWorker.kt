package dev.personal.linkgrab.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.personal.linkgrab.LinkGrabApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Updates yt-dlp about once a week in the background, even if the app isn't opened.
 * Respects the "Automatically update yt-dlp" switch in Settings.
 */
class YtDlpUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LinkGrabApp
        if (!app.settingsRepository.settings.first().autoUpdateYtDlp) return Result.success()

        return when (YtDlpUpdater.update(applicationContext)) {
            is YtDlpUpdater.Result.Failed -> if (runAttemptCount < 3) Result.retry() else Result.success()
            else -> {
                applicationContext
                    .getSharedPreferences("linkgrab_updates", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("yt_dlp_last_update", System.currentTimeMillis())
                    .apply()
                Result.success()
            }
        }
    }

    companion object {
        const val UNIQUE_NAME = "linkgrab-ytdlp-weekly-update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<YtDlpUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
