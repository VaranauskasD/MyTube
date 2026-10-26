package com.varanauskasd.mytube

import android.content.Context
import org.json.JSONArray

private const val PREFS_NAME = "mytube_prefs"
private const val KEY_SUBSCRIPTIONS = "subscriptions"

object SubscriptionsStore {

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SUBSCRIPTIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun save(context: Context, subscriptions: List<String>) {
        val array = JSONArray()
        subscriptions.forEach { array.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SUBSCRIPTIONS, array.toString())
            .apply()
    }
}

fun channelDisplayName(channelUrl: String): String {
    val trimmed = channelUrl.trim().removeSuffix("/videos").removeSuffix("/")
    val handleIndex = trimmed.indexOf('@')
    if (handleIndex != -1) return trimmed.substring(handleIndex)
    return trimmed.substringAfterLast('/')
}

/**
 * Accepts either a bare handle ("budgetforlifers", "@budgetforlifers") or a
 * full channel URL, and always returns a full youtube.com channel URL.
 */
fun normalizeChannelInput(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.contains("youtube.com")) return "https://$trimmed"
    val handle = if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    return "https://www.youtube.com/$handle"
}
