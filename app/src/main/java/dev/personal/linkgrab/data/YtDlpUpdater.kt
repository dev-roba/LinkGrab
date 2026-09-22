package dev.personal.linkgrab.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YtDlpUpdater {
    sealed interface Result { data object Updated : Result; data object Current : Result; data class Failed(val message: String) : Result }
    suspend fun update(context: Context): Result = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().init(context.applicationContext)
            val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel._STABLE)
            if (status == YoutubeDL.UpdateStatus.DONE) Result.Updated else Result.Current
        }.getOrElse { Result.Failed(it.message ?: "Could not update yt-dlp") }
    }
}
