package dev.personal.linkgrab.util

import android.content.Context
import dev.personal.linkgrab.R

object ErrorMapper {

    data class FriendlyError(
        val title: String,
        val message: String,
        val suggestion: String?
    )

    fun map(throwable: Throwable, context: Context): FriendlyError {
        val message = throwable.message ?: ""

        return when {
            message.contains("Network is unreachable", ignoreCase = true) ||
            message.contains("Unable to resolve", ignoreCase = true) ||
            message.contains("No address associated", ignoreCase = true) -> FriendlyError(
                title = "No internet connection",
                message = "LinkGrab could not reach this website.",
                suggestion = "Check Wi-Fi or mobile data, then try again."
            )

            message.contains("private", ignoreCase = true) ||
            message.contains("video is unavailable", ignoreCase = true) ||
            message.contains("deleted", ignoreCase = true) -> FriendlyError(
                title = "This video is unavailable",
                message = "It may have been deleted, made private, or the link may be wrong.",
                suggestion = "Try opening the link in its original app first."
            )

            message.contains("HTTP Error 403", ignoreCase = true) ||
            message.contains("Forbidden", ignoreCase = true) -> FriendlyError(
                title = "Access Denied",
                message = "This link requires login or is geo-restricted.",
                suggestion = "Try a different link or check if the content is publicly available."
            )

            message.contains("HTTP Error 404", ignoreCase = true) ||
            message.contains("Not Found", ignoreCase = true) -> FriendlyError(
                title = "Not Found",
                message = "This content no longer exists or the link is incorrect.",
                suggestion = "Verify the URL and try again."
            )

            message.contains("HTTP Error 429", ignoreCase = true) ||
            message.contains("Too Many Requests", ignoreCase = true) -> FriendlyError(
                title = "Rate Limited",
                message = "Too many requests. Please wait a moment.",
                suggestion = "Try again in a few seconds."
            )

            message.contains("rate-limit reached", ignoreCase = true) ||
            message.contains("login required", ignoreCase = true) ||
            message.contains("locked behind the login page", ignoreCase = true) -> FriendlyError(
                title = "Login Required",
                message = "Instagram is asking for a login or has temporarily limited access to this Reel.",
                suggestion = "Open the Reel in Instagram, confirm it is public, then try again later."
            )

            message.contains("Unable to extract", ignoreCase = true) ||
            message.contains("Unsupported URL", ignoreCase = true) -> FriendlyError(
                title = "Unsupported Link",
                message = "This website is not supported by yt-dlp.",
                suggestion = "Check supported sites at yt-dlp supported sites list."
            )

            message.contains("Sign in", ignoreCase = true) ||
            message.contains("login", ignoreCase = true) -> FriendlyError(
                title = "Login Required",
                message = "This content requires you to be logged in.",
                suggestion = "The creator may have restricted access to this content."
            )

            message.contains("Geo", ignoreCase = true) ||
            message.contains("not available in your country", ignoreCase = true) -> FriendlyError(
                title = "Region Restricted",
                message = "This content is not available in your region.",
                suggestion = "Try using a VPN if you need to access this content."
            )

            message.contains("timed out", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> FriendlyError(
                title = "Connection Timeout",
                message = "The request took too long to complete.",
                suggestion = "Check your internet connection and try again."
            )

            message.contains("resolve", ignoreCase = true) ||
            message.contains("DNS", ignoreCase = true) -> FriendlyError(
                title = "Connection Error",
                message = "Could not connect to the server.",
                suggestion = "Check your internet connection."
            )

            else -> FriendlyError(
                title = "Download Failed",
                message = throwable.message ?: "An unknown error occurred.",
                suggestion = "Try again or use a different link."
            )
        }
    }

    fun invalidUrl(url: String): String {
        return if (url.isBlank()) {
            "Please enter a URL"
        } else {
            "Invalid URL: $url"
        }
    }
}
