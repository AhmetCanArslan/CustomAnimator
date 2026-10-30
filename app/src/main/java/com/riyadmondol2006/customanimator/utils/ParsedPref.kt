package com.riyadmondol2006.customanimator.utils

class ParsedPref<T> {

    @Volatile
    private var entry: Pair<String, T>? = null

    fun get(raw: String, parse: (String) -> T): T {
        val current = entry
        if (current != null && current.first == raw) return current.second
        return parse(raw).also { entry = raw to it }
    }
}
