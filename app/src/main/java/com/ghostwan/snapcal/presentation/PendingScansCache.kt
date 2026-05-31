package com.ghostwan.snapcal.presentation

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache that holds completed background scan results until the user
 * opens them via their notification. Each scan gets a unique id so multiple
 * pending scans can coexist (one notification per scan, each opening its own
 * result).
 */
object PendingScansCache {
    private val cache = ConcurrentHashMap<String, FoodAnalysis>()

    fun newId(): String = UUID.randomUUID().toString()

    fun put(id: String, analysis: FoodAnalysis) {
        cache[id] = analysis
    }

    fun take(id: String): FoodAnalysis? = cache.remove(id)

    fun peek(id: String): FoodAnalysis? = cache[id]
}
