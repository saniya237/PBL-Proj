
package com.example.nextwin.data

data class SimulationResult(
    val scenario: String,
    val deviceId: String,
    val deviceName: String,
    val previousStatus: String,
    val newStatus: String,
    val directlyAffected: List<String>,
    val lostConnectivity: List<String>,
    val gainedConnectivity: List<String>,
    val offlineDevices: List<String>,
    val reachableBefore: Int,
    val reachableAfter: Int,
    val summary: String
)

data class SimulationResponse(
    val result: SimulationResult,
    val topology: Topology
)
