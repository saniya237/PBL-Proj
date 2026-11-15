
package com.example.nextwin.data

import android.content.Context

/**
 * Loads SAMPLE data bundled inside the app.
 * This is placeholder data, not live detections or real campus traffic.
 */
class SampleRepository(private val context: Context) {

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    fun loadTopology(): Topology =
        JsonParser.parseTopology(readAsset("topology.json"))

    fun loadSampleSimulation(): SimulationResponse =
        JsonParser.parseSimulationResponse(
            readAsset("simulation_disconnect_SW002.json")
        )
}
