package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.SavedQuestion
import io.github.eyalshimshi.betterstips.data.parse.asArray
import io.github.eyalshimshi.betterstips.data.parse.asObject
import io.github.eyalshimshi.betterstips.data.parse.long
import io.github.eyalshimshi.betterstips.data.parse.str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import java.io.File

/** Questions the user saved. They stay on this phone only. */
class SavedStore(context: Context) {
    private val prefs = context.getSharedPreferences("saved", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(read())
    val items: StateFlow<List<SavedQuestion>> = _items.asStateFlow()

    fun toggle(question: Question) {
        val current = _items.value
        val next = if (current.any { it.id == question.id }) {
            current.filterNot { it.id == question.id }
        } else {
            listOf(SavedQuestion(question.id, question.title, question.cover?.thumbnailUrl, System.currentTimeMillis())) + current
        }
        _items.value = next
        write(next)
    }

    fun remove(id: Long) {
        val next = _items.value.filterNot { it.id == id }
        _items.value = next
        write(next)
    }

    private fun write(list: List<SavedQuestion>) {
        val json = buildJsonArray {
            list.forEach { item ->
                addJsonObject {
                    put("id", item.id)
                    put("title", item.title)
                    put("cover", item.coverUrl)
                    put("savedAt", item.savedAt)
                }
            }
        }
        prefs.edit().putString("items", json.toString()).apply()
    }

    private fun read(): List<SavedQuestion> = try {
        Json.parseToJsonElement(prefs.getString("items", null) ?: "[]").asArray()?.mapNotNull { element ->
            val o = element.asObject() ?: return@mapNotNull null
            val id = o.long("id") ?: return@mapNotNull null
            SavedQuestion(id, o.str("title").orEmpty(), o.str("cover"), o.long("savedAt") ?: 0L)
        }.orEmpty()
    } catch (e: Exception) {
        emptyList()
    }
}

/** The last-seen first page of each list, so the next launch shows something at once (F-19). */
class FeedCache(private val dir: File) {
    fun read(key: String): String? = file(key).takeIf { it.exists() }?.readText()

    fun write(key: String, json: String) {
        dir.mkdirs()
        file(key).writeText(json)
    }

    private fun file(key: String) = File(dir, "feed-" + Integer.toHexString(key.hashCode()) + ".json")
}

data class Timing(val label: String, val millis: Long, val at: Long)

/** Recent load times, shown as "Loaded in 0.4 s" and in Settings → Performance (F-18). */
class Timings {
    private val _recent = MutableStateFlow<List<Timing>>(emptyList())
    val recent: StateFlow<List<Timing>> = _recent.asStateFlow()

    fun record(label: String, millis: Long) {
        val entry = Timing(label, millis, System.currentTimeMillis())
        synchronized(this) { _recent.value = (listOf(entry) + _recent.value).take(20) }
    }

    companion object {
        const val FEED = "feed"
        const val QUESTION = "question"
    }
}
