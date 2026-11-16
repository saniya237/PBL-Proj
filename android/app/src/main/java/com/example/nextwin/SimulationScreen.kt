
package com.example.nextwin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextwin.data.ApiClient
import com.example.nextwin.data.SimulationResult
import com.example.nextwin.data.Topology
import kotlinx.coroutines.launch

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

private val SimBackground = Color(0xFF081729)
private val SimCard = Color(0xFF142B47)
private val SimCyan = Color(0xFF38BDF8)
private val SimWhite = Color(0xFFF1F7FF)
private val SimMuted = Color(0xFFB0C4DE)
private val SimGreen = Color(0xFF35C979)
private val SimRed = Color(0xFFFF647C)

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

    var isRunning by remember { mutableStateOf(false) }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val coroutineScope = rememberCoroutineScope()

    val selectedDevice = topology.devices.firstOrNull {
        it.id == selectedDeviceId
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "What-if Simulation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SimWhite
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "NETWORKX BACKEND SIMULATION",
            color = SimCyan,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // Simulation information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = SimCard
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    "SIMULATION CONTROL CENTER",
                    color = SimCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Test how network devices affect campus connectivity.",
                    color = SimWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Select Device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SimWhite
        )

        Spacer(Modifier.height(10.dp))

        // Device selection
        Box {
            OutlinedButton(
                onClick = { deviceMenuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SimWhite
                )
            ) {
                Text(
                    selectedDevice?.let {
                        "${it.name} (${it.id})"
                    } ?: "Select device"
                )
            }

            DropdownMenu(
                expanded = deviceMenuExpanded,
                onDismissRequest = { deviceMenuExpanded = false },
                containerColor = SimCard
            ) {
                topology.devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${device.name} (${device.id})",
                                color = SimWhite
                            )
                        },
                        onClick = {
                            selectedDeviceId = device.id
                            deviceMenuExpanded = false
                            result = null
                            errorMessage = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "Select Scenario",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SimWhite
        )

        Spacer(Modifier.height(10.dp))

        // Scenario selection
        Box {
            OutlinedButton(
                onClick = { scenarioMenuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SimWhite
                )
            ) {
                Text(selectedScenario.label)
            }

            DropdownMenu(
                expanded = scenarioMenuExpanded,
                onDismissRequest = { scenarioMenuExpanded = false },
                containerColor = SimCard
            ) {
                scenarios.forEach { scenario ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                scenario.label,
                                color = SimWhite
                            )
                        },
                        onClick = {
                            selectedScenario = scenario
                            scenarioMenuExpanded = false
                            result = null
                            errorMessage = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Run simulation
        Button(
            onClick = {
                coroutineScope.launch {
                    isRunning = true
                    errorMessage = null
                    result = null

                    try {
                        val response = ApiClient.runSimulation(
                            deviceId = selectedDeviceId,
                            scenario = selectedScenario.id
                        )

                        result = response.result

                        val updatedStatuses =
                            response.topology.devices.associate {
                                it.id to it.status
                            }

                        onDeviceStatusesChange(updatedStatuses)

                    } catch (e: Exception) {
                        errorMessage =
                            e.message ?: "Simulation failed"
                    } finally {
                        isRunning = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            enabled = selectedDeviceId.isNotBlank() && !isRunning,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SimCyan,
                contentColor = SimBackground,
                disabledContainerColor = SimCard,
                disabledContentColor = SimMuted
            )
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = SimBackground
                )

                Spacer(Modifier.width(10.dp))

                Text("Running Simulation...")
            } else {
                Text(
                    "Run Simulation",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Clear result
        OutlinedButton(
            onClick = {
                result = null
                errorMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isRunning,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SimMuted
            )
        ) {
            Text("Clear Result")
        }

        // Error display
        errorMessage?.let { error ->
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF482331)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Simulation Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFFFA6B5)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Selected device information
        selectedDevice?.let { device ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SimCard
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        "SELECTED DEVICE",
                        color = SimCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        device.name,
                        color = SimWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Device ID: ${device.id}",
                        color = SimMuted
                    )

                    Text(
                        "Current Status: ${
                            deviceStatuses[device.id] ?: device.status
                        }",
                        color = SimMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        result?.let { simulationResult ->
            SimulationResultContent(simulationResult)
        } ?: Text(
            "Select a device and scenario, then tap Run Simulation to view the network impact.",
            style = MaterialTheme.typography.bodyMedium,
            color = SimMuted
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Simulation results are generated by the Python FastAPI backend using the simulated NetworkX campus topology. They represent a what-if simulation, not a live campus network event.",
            style = MaterialTheme.typography.bodySmall,
            color = SimMuted
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SimulationResultContent(result: SimulationResult) {

    Text(
        "Simulation Result",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = SimWhite
    )

    Spacer(Modifier.height(16.dp))

    InfoCard(
        "Selected Device",
        "${result.deviceName} (${result.deviceId})"
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
        "Status Change",
        "${result.previousStatus} → ${result.newStatus}"
    )

    Spacer(Modifier.height(22.dp))

    Text(
        "Network Impact",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = SimCyan
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

    DeviceListCard(
        "Directly Affected Devices",
        result.directlyAffected
    )

    Spacer(Modifier.height(12.dp))

    DeviceListCard(
        "Lost Connectivity",
        result.lostConnectivity
    )

    Spacer(Modifier.height(12.dp))

    DeviceListCard(
        "Offline Devices",
        result.offlineDevices
    )

    Spacer(Modifier.height(12.dp))

    DeviceListCard(
        "Gained Connectivity",
        result.gainedConnectivity
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
        "Simulation Summary",
        result.summary
    )
}

@Composable
private fun InfoCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SimCard
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = SimCyan,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                content,
                style = MaterialTheme.typography.bodyLarge,
                color = SimWhite
            )
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
        content = if (devices.isEmpty()) {
            "None"
        } else {
            devices.joinToString(", ")
        }
    )
}
