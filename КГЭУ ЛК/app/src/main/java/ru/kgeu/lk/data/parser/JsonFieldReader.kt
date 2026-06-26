package ru.kgeu.lk.data.parser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.text(vararg keys: String): String? {
    for (key in keys) {
        val value = this[key]?.primitiveText()?.takeIf { it.isNotBlank() } ?: continue
        return value
    }
    return null
}

internal fun JsonObject.int(vararg keys: String): Int? {
    for (key in keys) {
        val element = this[key] ?: continue
        element.jsonPrimitive.intOrNull?.let { return it }
        element.jsonPrimitive.contentOrNull?.toIntOrNull()?.let { return it }
    }
    return null
}

internal fun JsonObject.bool(vararg keys: String): Boolean? {
    for (key in keys) {
        when (val element = this[key]) {
            null, is JsonNull -> continue
            is JsonPrimitive -> {
                when (element.content.lowercase()) {
                    "true", "1" -> return true
                    "false", "0" -> return false
                }
            }
            else -> continue
        }
    }
    return null
}

internal fun JsonElement.primitiveText(): String? =
    if (this is JsonNull) null else jsonPrimitive.contentOrNull

internal fun JsonObject.array(vararg keys: String): JsonArray? {
    for (key in keys) {
        this[key]?.jsonArray?.let { return it }
    }
    return null
}

internal fun JsonObject.obj(vararg keys: String): JsonObject? {
    for (key in keys) {
        this[key]?.jsonObject?.let { return it }
    }
    return null
}
