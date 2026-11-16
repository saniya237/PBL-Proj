
package com.example.nextwin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextwin.data.SimulationResult
import com.example.nextwin.data.Topology

private data class ScenarioOption(
    val id: String,
    val label: String
)

private val scenarios = listOf(
    ScenarioOption("suspicious", "Mark as Suspicious"),
    ScenarioOption("disconnect", "Disconnect Device"),
    ScenarioOption("isolate", "Isolate Device"),
    ScenarioOption("restore", "Restore Device")
)

@Composable
fun SimulationScreen(
    topology: Topology,
    deviceStatuses: Map<String, String>,
    originalDeviceStatuses: Map<String, String>,
    onDeviceStatusesChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDeviceId by remember {
        mutableStateOf(topology.devices.firstOrNull()?.id ?: "")
    }

    var selectedScenario by remember {
        mutableStateOf(scenarios[0])
    }

    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var scenarioMenuExpanded by remember { mutableStateOf(false) }

    var result by remember {
        mutableStateOf<SimulationResult?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "What-if Simulation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "LOCAL SIMULATION PREVIEW",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Select Device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { deviceMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    topology.devices.firstOrNull {
                        it.id == selectedDeviceId
                    }?.let { "${it.name} (${it.id})" } ?: "Select device"
                )
            }

            DropdownMenu(
                expanded = deviceMenuExpanded,
                onDismissRequest = { deviceMenuExpanded = false }
            ) {
                topology.devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text("${device.name} (${device.id})")
                        },
                        onClick = {
                            selectedDeviceId = device.id
                            deviceMenuExpanded = false
                            result = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Select Scenario",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { scenarioMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedScenario.label)
            }

            DropdownMenu(
                expanded = scenarioMenuExpanded,
                onDismissRequest = { scenarioMenuExpanded = false }
            ) {
                scenarios.forEach { scenario ->
                    DropdownMenuItem(
                        text = { Text(scenario.label) },
                        onClick = {
                            selectedScenario = scenario
                            scenarioMenuExpanded = false
                            result = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val calculated = calculateLocalSimulation(
                    topology = topology,
                    currentStatuses = deviceStatuses,
                    deviceId = selectedDeviceId,
                    scenario = selectedScenario.id
                )

                result = calculated.first
                onDeviceStatusesChange(calculated.second)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDeviceId.isNotBlank()
        ) {
            Text("Run Simulation")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                onDeviceStatusesChange(originalDeviceStatuses)
                result = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Simulation")
        }

        Spacer(Modifier.height(20.dp))

        result?.let { simulationResult ->
            SimulationResultContent(simulationResult)
        } ?: Text(
            "Select a device and scenario, then tap Run Simulation to view the predicted network impact.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "This is a local Android preview calculated from the bundled simulated network topology. It does not contact the Python backend or represent a live campus network event. The actual NetworkX engine will be connected through FastAPI later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateLocalSimulation(
    topology: Topology,
    currentStatuses: Map<String, String>,
    deviceId: String,
    scenario: String
): Pair<SimulationResult, Map<String, String>> {

    val device = topology.devices.first { it.id == deviceId }

    val newStatus = when (scenario) {
        "suspicious" -> "suspicious"
        "disconnect" -> "disconnected"
        "isolate" -> "isolated"
        "restore" -> "normal"
        else -> currentStatuses[deviceId] ?: "normal"
    }

    val before = reachableDevices(topology, currentStatuses)

    val updatedStatuses = currentStatuses.toMutableMap()
    val previousStatus = updatedStatuses[deviceId] ?: "normal"
    updatedStatuses[deviceId] = newStatus

    val after = reachableDevices(topology, updatedStatuses)

    val lost = (before - after - deviceId).sorted()
    val gained = (after - before - deviceId).sorted()

    val directlyAffected = topology.links
        .filter { it.source == deviceId || it.target == deviceId }
        .map { if (it.source == deviceId) it.target else it.source }
        .sorted()

    val offline = updatedStatuses
        .filterValues { it == "disconnected" || it == "isolated" }
        .keys.sorted()

    val summary = when (scenario) {
        "suspicious" ->
            "${device.name} (${deviceId}) is marked suspicious. It remains online, so connectivity is unchanged."

        "disconnect" ->
            "${device.name} (${deviceId}) was disconnected. ${lost.size} other device(s) lost internet connectivity."

        "isolate" ->
            "${device.name} (${deviceId}) was isolated. ${lost.size} other device(s) lost internet connectivity."

        else ->
            "${device.name} (${deviceId}) was restored to normal. ${gained.size} other device(s) regained internet connectivity."
    }

    return SimulationResult(
        scenario = scenario,
        deviceId = deviceId,
        deviceName = device.name,
        previousStatus = previousStatus,
        newStatus = newStatus,
        directlyAffected = directlyAffected,
        lostConnectivity = lost,
        gainedConnectivity = gained,
        offlineDevices = offline,
        reachableBefore = before.size,
        reachableAfter = after.size,
        summary = summary
    ) to updatedStatuses
}

private fun reachableDevices(
    topology: Topology,
    statuses: Map<String, String>
): Set<String> {

    val internetId = "INET001"

    if (statuses[internetId] == "disconnected" ||
        statuses[internetId] == "isolated" ||
        topology.devices.none { it.id == internetId }
    ) {
        return emptySet()
    }

    val onlineDevices = statuses
        .filterValues { it != "disconnected" && it != "isolated" }
        .keys

    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()

    visited.add(internetId)
    queue.add(internetId)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()

        topology.links.forEach { link ->
            val neighbour = when (current) {
                link.source -> link.target
                link.target -> link.source
                else -> null
            }

            if (neighbour != null &&
                neighbour in onlineDevices &&
                visited.add(neighbour)
            ) {
                queue.add(neighbour)
            }
        }
    }

    return visited - internetId
}

@Composable
private fun SimulationResultContent(result: SimulationResult) {
    Text(
        "Simulation Result",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
        "Selected Device",
        "${result.deviceName} (${result.deviceId})"
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
        "Status Change",
        "${result.previousStatus} → ${result.newStatus}"
    )

    Spacer(Modifier.height(20.dp))

    Text(
        "Network Impact",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(12.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        InfoCard(
            "Reachable Before",
            result.reachableBefore.toString(),
            Modifier.weight(1f)
        )

        InfoCard(
            "Reachable After",
            result.reachableAfter.toString(),
            Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    DeviceListCard("Directly Affected Devices", result.directlyAffected)

    Spacer(Modifier.height(12.dp))

    DeviceListCard("Lost Connectivity", result.lostConnectivity)

    Spacer(Modifier.height(12.dp))

    DeviceListCard("Offline Devices", result.offlineDevices)

    Spacer(Modifier.height(12.dp))

    DeviceListCard("Gained Connectivity", result.gainedConnectivity)

    Spacer(Modifier.height(12.dp))

    InfoCard("Simulation Summary", result.summary)
}

@Composable
private fun InfoCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DeviceListCard(
    title: String,
    devices: List<String>
) {
    InfoCard(
        title = title,
        content = if (devices.isEmpty()) "None" else devices.joinToString(", ")
    )
}
