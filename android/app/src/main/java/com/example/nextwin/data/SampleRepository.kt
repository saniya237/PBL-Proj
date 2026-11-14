package com.example.nextwin.data

import android.content.Context

/**
 * Loads SAMPLE data bundled inside the app (assets folder).
 * Used for building the screens before the backend is connected.
 * This is placeholder data, not live detections.
 */
class SampleRepository(private val context: Context) {

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    fun loadTopology(): Topology =
        JsonParser.parseTopology(readAsset("topology.json"))
}