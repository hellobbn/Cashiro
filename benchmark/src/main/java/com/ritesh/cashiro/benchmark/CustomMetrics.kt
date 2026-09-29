package com.ritesh.cashiro.benchmark

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Timings Macrobenchmark has no metric for (e.g. time until data is on screen), written
 * next to its own results so CI pulls and compares them the same way.
 */
object CustomMetrics {
    fun write(name: String, unit: String, runs: List<Double>) {
        File(outputDir(), "$name-customMetric.json").writeText(
            JSONObject()
                .put("name", name)
                .put("unit", unit)
                .put("runs", JSONArray(runs))
                .toString()
        )
        step("$name: ${runs.joinToString { "%.0f".format(it) }} $unit")
    }
}
