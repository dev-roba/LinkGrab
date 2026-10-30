package dev.personal.linkgrab

import android.app.Application
import dev.personal.linkgrab.data.history.HistoryRepository
import dev.personal.linkgrab.data.YtDlpUpdater
import dev.personal.linkgrab.data.settings.SettingsRepository
import dev.personal.linkgrab.service.DownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkGrabApp : Application() {
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    override fun onCreate() {
        super.onCreate()
        historyRepository = HistoryRepository(this)
        settingsRepository = SettingsRepository(this)
        // Fresh dashboard every launch: empty queue, no auto-resume of old jobs
        DownloadStore.init(this, clearPersisted = true)
        CoroutineScope(Dispatchers.IO).launch {
            ensureYoutubeDL(this@LinkGrabApp)
            val preferences = getSharedPreferences("linkgrab_updates", MODE_PRIVATE)
            val lastUpdate = preferences.getLong("yt_dlp_last_update", 0L)
            if (System.currentTimeMillis() - lastUpdate >= 24 * 60 * 60 * 1000L) {
                YtDlpUpdater.update(this@LinkGrabApp)
                preferences.edit().putLong("yt_dlp_last_update", System.currentTimeMillis()).apply()
            }
        }
    }
}
