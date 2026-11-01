package dev.personal.linkgrab.playback

import android.content.Context

/** Remembers where you stopped in each file (keyed by its media address). */
object PositionStore {
    private const val FILE = "linkgrab_positions"

    fun get(context: Context, mediaId: String): Long =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(mediaId, 0L)

    fun save(context: Context, mediaId: String, positionMs: Long, durationMs: Long) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // Near the start or the end counts as "not started" / "finished": forget the position.
        if (durationMs > 0 && (positionMs < 5_000L || positionMs > durationMs - 5_000L)) {
            prefs.edit().remove(mediaId).apply()
        } else if (positionMs >= 5_000L) {
            prefs.edit().putLong(mediaId, positionMs).apply()
        }
    }

    fun clear(context: Context, mediaId: String) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(mediaId).apply()
    }
}
