package dev.personal.linkgrab.data

import android.net.Uri

object UrlParser {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    /**
     * Extracts and validates URLs from arbitrary text input.
     * Returns a list of valid, normalized http/https URLs.
     */
    fun extractUrls(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        val candidates = Regex("https?://[^\\s<>\\\"]+").findAll(input)
            .map { it.value.trimEnd('.', ',', '!', '?', ')', ']', '}', '#') }
            .filter { it.isNotBlank() }

        return candidates
            .mapNotNull { normalizeUrl(it) }
            .distinct()
            .toList()
    }

    /**
     * Normalizes a URL by ensuring it has a valid http/https scheme.
     * Returns null if the URL cannot be normalized to a valid http/https URL.
     */
    fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase() ?: return null

        if (scheme !in ALLOWED_SCHEMES) return null
        if (uri.host.isNullOrBlank()) return null

        return if (isValidUrl(trimmed)) trimmed else null
    }

    /**
     * Validates a URL string for safety and format.
     * Returns true only for http/https URLs with a valid host.
     */
    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme !in ALLOWED_SCHEMES) return false
        if (uri.host.isNullOrBlank()) return false

        return android.util.Patterns.WEB_URL.matcher(trimmed).matches()
    }
}
