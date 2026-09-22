package dev.personal.linkgrab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.personal.linkgrab.navigation.LinkGrabNavGraph
import dev.personal.linkgrab.ui.theme.LinkGrabTheme
import dev.personal.linkgrab.util.PerfTrace

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LinkGrabTheme { LinkGrabNavGraph(intent.getStringArrayExtra("shared_urls")?.firstOrNull()) } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); recreate() }
}

@Synchronized
internal fun ensureYoutubeDL(context: android.content.Context) {
    PerfTrace.mark("engine_init_start")
    com.yausername.youtubedl_android.YoutubeDL.getInstance().init(context.applicationContext)
    PerfTrace.mark("engine_init_end")
}
