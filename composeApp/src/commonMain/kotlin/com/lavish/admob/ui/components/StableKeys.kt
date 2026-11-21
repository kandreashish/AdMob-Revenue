package com.lavish.admob.ui.components

/**
 * Builds LazyColumn keys from an id, guaranteeing uniqueness.
 * Duplicate ids (e.g. a locally added app colliding with a synced one) get a "#n" suffix
 * instead of crashing with "Key was already used".
 */
fun <T> uniqueKeys(items: List<T>, id: (T) -> String): List<String> {
    val seen = HashMap<String, Int>(items.size)
    return items.map { item ->
        val base = id(item)
        val n = seen.getOrElse(base) { 0 }
        seen[base] = n + 1
        if (n == 0) base else "$base#$n"
    }
}
