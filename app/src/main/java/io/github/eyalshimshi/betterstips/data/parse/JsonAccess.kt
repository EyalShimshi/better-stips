package io.github.eyalshimshi.betterstips.data.parse

import io.github.eyalshimshi.betterstips.util.TextUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Lenient accessors: Stips' JSON mixes numbers and strings, and fields come and go.

internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArray(): JsonArray? = this as? JsonArray

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonElement?.primitiveText(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    return primitive.content
}

internal fun JsonObject.str(key: String): String? = this[key].primitiveText()

/** What people wrote, as they wrote it: Stips sends it HTML-escaped ("&gt;" for ">"), so it's decoded once (v0.12). */
internal fun JsonObject.text(key: String): String? = str(key)?.let(TextUtils::decodeEntities)

internal fun JsonElement?.decodedText(): String? = primitiveText()?.let(TextUtils::decodeEntities)

internal fun JsonObject.long(key: String): Long? {
    val text = str(key)?.trim() ?: return null
    return text.toLongOrNull() ?: text.toDoubleOrNull()?.toLong()
}

internal fun JsonObject.int(key: String): Int? = long(key)?.toInt()

internal fun JsonObject.bool(key: String): Boolean? = when (str(key)?.trim()?.lowercase()) {
    "true", "1" -> true
    "false", "0" -> false
    else -> null
}
