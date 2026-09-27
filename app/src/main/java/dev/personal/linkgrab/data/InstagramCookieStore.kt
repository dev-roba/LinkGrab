package dev.personal.linkgrab.data

import android.content.Context

object InstagramCookieStore {
    private const val PREFS = "instagram"
    private const val KEY = "cookies"

    fun load(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun save(context: Context, raw: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, raw.trim()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}