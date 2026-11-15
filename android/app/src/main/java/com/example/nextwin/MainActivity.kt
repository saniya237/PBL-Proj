package com.example.nextwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextwin.data.SampleRepository
import com.example.nextwin.data.Topology
import com.example.nextwin.ui.theme.NexTwinTheme
import com.example.nextwin.data.SimulationResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = SampleRepository(this)
        val topology = repository.loadTopology()
        val simulation = repository.loadSampleSimulation()

        setContent {
            NexTwinTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NexTwinApp(topology , simulation)
                }
            }
        }
    }
}

private enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    TOPOLOGY("Topology"),
    SIMULATION("Simulation"),
    ALERTS("Alerts"),
    SUMMARY("Summary")
}

@Composable
fun NexTwinApp(
    topology: Topology,
    simulation: SimulationResponse
) {
    var selectedScreen by remember {
        mutableStateOf(AppScreen.DASHBOARD)
    }

    val originalDeviceStatuses = remember(topology) {
    topology.devices.associate { it.id to it.status }
}

var deviceStatuses by remember(topology) {
    mutableStateOf(originalDeviceStatuses)
}

    val sharedTopology = remember(topology, deviceStatuses) {
        topology.copy(
            devices = topology.devices.map { device ->
                device.copy(
                    status = deviceStatuses[device.id] ?: device.status
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppScreen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        icon = {
                            Text(screen.title.take(1))
                        },
                        label = {
                            Text(screen.title)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        when (selectedScreen) {
            AppScreen.DASHBOARD -> DashboardScreen(
    topology = sharedTopology,
    modifier = Modifier.padding(innerPadding)
)

           AppScreen.TOPOLOGY -> NetworkTopologyScreen(
    topology = sharedTopology,
    modifier = Modifier.padding(innerPadding)
)

           AppScreen.SIMULATION -> SimulationScreen(
    topology = sharedTopology,
    deviceStatuses = deviceStatuses,
    originalDeviceStatuses = originalDeviceStatuses,
    onDeviceStatusesChange = { deviceStatuses = it },
    modifier = Modifier.padding(innerPadding)
)

            AppScreen.ALERTS -> AlertsScreen(
                modifier = Modifier.padding(innerPadding)
            )

            AppScreen.SUMMARY -> SummaryScreen(
    topology = sharedTopology,
    modifier = Modifier.padding(innerPadding)
)
        }
    }
}

@Composable
fun DashboardScreen(
    topology: Topology,
    modifier: Modifier = Modifier
) {
    val normalCount = topology.devices.count {
        it.status.equals("normal", ignoreCase = true)
    }

    val suspiciousCount = topology.devices.count {
        it.status.equals("suspicious", ignoreCase = true)
    }

    val isolatedCount = topology.devices.count {
        it.status.equals("isolated", ignoreCase = true)
    }

    val disconnectedCount = topology.devices.count {
        it.status.equals("disconnected", ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "NexTwin",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Campus Network Security",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SAMPLE DATA",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Network Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                title = "Total Devices",
                count = topology.devices.size,
                modifier = Modifier.weight(1f)
            )

            StatusCard(
                title = "Connections",
                count = topology.links.size,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                title = "Normal",
                count = normalCount,
                modifier = Modifier.weight(1f)
            )

            StatusCard(
                title = "Suspicious",
                count = suspiciousCount,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                title = "Isolated",
                count = isolatedCount,
                modifier = Modifier.weight(1f)
            )

            StatusCard(
                title = "Disconnected",
                count = disconnectedCount,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Security Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Alert data will appear after backend integration."
                )

                Text(
                    text = "No live alert count available yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Network Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Devices are displayed using the local sample topology. " +
                "This is a simulated campus network, not live campus traffic.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun StatusCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ComingSoonScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This module will be implemented in the next development step.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
