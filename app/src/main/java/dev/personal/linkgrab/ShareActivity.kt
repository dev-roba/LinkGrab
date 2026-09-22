package dev.personal.linkgrab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dev.personal.linkgrab.data.UrlParser

/**
 * ShareActivity: ONLY exported activity in v1.
 * Handles incoming ACTION_SEND / ACTION_SEND_MULTIPLE intents with text/plain.
 * Extracts shared URLs/text and forwards to MainActivity for processing.
 */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val urls = extractUrlsFromIntent(intent)
        if (urls.isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("shared_urls", urls.toTypedArray())
            })
            return
        }
        finish()
    }

    private fun extractUrlsFromIntent(intent: Intent): List<String> {
        val urls = mutableListOf<String>()
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && "text/plain" == type) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            if (!text.isNullOrBlank()) urls.addAll(UrlParser.extractUrls(text))
        } else if (Intent.ACTION_SEND_MULTIPLE == action && "text/plain" == type) {
            val texts = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)
            texts?.forEach { text ->
                if (!text.isNullOrBlank()) urls.addAll(UrlParser.extractUrls(text.toString()))
            }
        }
        return urls.distinct()
    }
}
