package dev.personal.linkgrab.util

import android.util.Patterns

object UrlValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    /**
     * Validates a URL string for safety and format.
     * Returns true only for http/https URLs that pass Android's Patterns matcher.
     */
    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        val uri = android.net.Uri.parse(trimmed) ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme !in ALLOWED_SCHEMES) return false
        if (uri.host.isNullOrBlank()) return false

        return Patterns.WEB_URL.matcher(trimmed).matches()
    }

    /**
     * Normalizes a URL by trimming whitespace and ensuring a scheme is present.
     * Returns null if the input cannot be normalized to a valid URL.
     */
    fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return if (isValidUrl(withScheme)) withScheme else null
    }
}
