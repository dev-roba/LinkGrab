package dev.personal.linkgrab.data.history

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HistoryRepository(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("linkgrab_history", Context.MODE_PRIVATE)
    private val mutableHistory = MutableStateFlow(load())
    val history: StateFlow<List<HistoryEntity>> = mutableHistory
    fun hasDownloaded(url: String, type: String): Boolean = mutableHistory.value.any { it.url == url && it.downloadType == type }

    suspend fun add(item: HistoryEntity) {
        mutableHistory.value = listOf(item.copy(id = System.currentTimeMillis())) + mutableHistory.value
        persist()
    }

    suspend fun remove(item: HistoryEntity) {
        mutableHistory.value = mutableHistory.value.filterNot { it.id == item.id }
        persist()
    }

    private fun load(): List<HistoryEntity> = runCatching {
        val items = JSONArray(preferences.getString("items", "[]"))
        List(items.length()) { index -> items.getJSONObject(index).let { json ->
            HistoryEntity(json.getLong("id"), json.getString("url"), json.getString("title"), json.getString("mimeType"), json.getString("mediaUri"), json.getString("downloadType"), json.optLong("sizeBytes", 0), json.optString("thumbnailUrl").ifBlank { null }, json.getLong("createdAt"))
        } }
    }.getOrDefault(emptyList())

    private fun persist() {
        val values = JSONArray()
        mutableHistory.value.forEach { item -> values.put(JSONObject().apply {
            put("id", item.id); put("url", item.url); put("title", item.title); put("mimeType", item.mimeType)
            put("mediaUri", item.mediaUri); put("downloadType", item.downloadType); put("createdAt", item.createdAt)
            put("sizeBytes", item.sizeBytes)
            put("thumbnailUrl", item.thumbnailUrl)
        }) }
        preferences.edit().putString("items", values.toString()).apply()
    }
}
