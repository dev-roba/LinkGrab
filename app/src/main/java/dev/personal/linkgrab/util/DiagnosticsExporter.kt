package dev.personal.linkgrab.util

import android.content.Context
import dev.personal.linkgrab.service.DownloadStore

object DiagnosticsExporter {
    fun report(context: Context): String = buildString {
        appendLine("LinkGrab diagnostics")
        appendLine("version=${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
        appendLine("android=${android.os.Build.VERSION.SDK_INT}")
        appendLine("jobs=${DownloadStore.items.value.size}")
        DownloadStore.items.value.forEach { appendLine("job=${it.id} type=${it.type} status=${it.status} progress=${(it.progress * 100).toInt()}% size=${it.sizeBytes}") }
        appendLine("URLs and personal metadata omitted")
    }
}
