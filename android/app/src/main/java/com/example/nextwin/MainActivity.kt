package com.example.nextwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextwin.data.ApiClient
import com.example.nextwin.data.SampleRepository
import com.example.nextwin.data.SimulationResponse
import com.example.nextwin.data.Topology
import com.example.nextwin.ui.theme.NexTwinTheme
import kotlinx.coroutines.launch

private val Navy = Color(0xFF071426)
private val NavyCard = Color(0xFF10243D)
private val NavyCardLight = Color(0xFF172F4D)
private val ElectricBlue = Color(0xFF38BDF8)
private val BrightBlue = Color(0xFF2563EB)
private val SoftWhite = Color(0xFFEAF4FF)
private val MutedText = Color(0xFF9DB2CC)
private val Green = Color(0xFF34D399)
private val Orange = Color(0xFFFBBF24)
private val Red = Color(0xFFFB7185)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = SampleRepository(this)
        val topology = repository.loadTopology()
        val simulation = repository.loadSampleSimulation()

        setContent {
            NexTwinTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy
                ) {
                    NexTwinApp(topology, simulation)
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
    var liveTopology by remember(topology) {
        mutableStateOf(topology)
    }

    LaunchedEffect(topology) {
        try {
            liveTopology = ApiClient.getTopology()
        } catch (e: Exception) {
            // Keep sample topology if API request fails.
        }
    }

    var backendStatus by remember {
        mutableStateOf("Checking backend...")
    }

    LaunchedEffect(Unit) {
        try {
            val health = ApiClient.getHealth()

            backendStatus = if (
                health.status == "success" &&
                health.modelLoaded &&
                health.preprocessorLoaded &&
                health.digitalTwinLoaded
            ) {
                "Backend Connected"
            } else {
                "Backend Components Not Ready"
            }

        } catch (e: Exception) {
            backendStatus = "Backend Connection Failed: ${e.message}"
        }
    }

    var selectedScreen by remember {
        mutableStateOf(AppScreen.DASHBOARD)
    }

    val originalDeviceStatuses = remember(liveTopology) {
        liveTopology.devices.associate { it.id to it.status }
    }

    var deviceStatuses by remember(liveTopology) {
        mutableStateOf(originalDeviceStatuses)
    }

    val sharedTopology = remember(liveTopology, deviceStatuses) {
        liveTopology.copy(
            devices = liveTopology.devices.map { device ->
                device.copy(
                    status = deviceStatuses[device.id] ?: device.status
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Navy,

        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A1B30),
                contentColor = SoftWhite
            ) {
                AppScreen.values().forEach { screen ->

                    NavigationBarItem(
                        selected = selectedScreen == screen,

                        onClick = {
                            selectedScreen = screen
                        },

                        icon = {
                            Text(
                                text = when (screen) {
                                    AppScreen.DASHBOARD -> "⌂"
                                    AppScreen.TOPOLOGY -> "◎"
                                    AppScreen.SIMULATION -> "⇄"
                                    AppScreen.ALERTS -> "⚠"
                                    AppScreen.SUMMARY -> "▤"
                                },
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },

                        label = {
                            Text(
                                screen.title,
                                fontSize = 10.sp
                            )
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = Color(0xFF173A5E),
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        }

    ) { innerPadding ->

        when (selectedScreen) {

            AppScreen.DASHBOARD -> DashboardScreen(
                topology = sharedTopology,
                backendStatus = backendStatus,
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
                onDeviceStatusesChange = {
                    deviceStatuses = it
                },
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
    backendStatus: String,
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

    var alertCount by remember {
        mutableStateOf<Int?>(null)
    }

    var alertError by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    fun refreshAlerts() {
        scope.launch {
            try {
                val response = ApiClient.getAlerts()
                alertCount = response.totalAlerts
                alertError = false
            } catch (e: Exception) {
                alertError = true
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAlerts()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // HEADER

        Text(
            text = "NexTwin",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SoftWhite
        )

        Text(
            text = "CAMPUS NETWORK SECURITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        // BACKEND CONNECTION

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = NavyCard
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (backendStatus == "Backend Connected")
                                Green else Orange,
                            CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        "SYSTEM STATUS",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        backendStatus,
                        color = if (backendStatus == "Backend Connected")
                            Green else Orange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // NETWORK OVERVIEW

        SectionHeading("Network Overview", "LIVE TOPOLOGY")

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                "Total Devices",
                topology.devices.size,
                "NETWORK NODES",
                ElectricBlue,
                Modifier.weight(1f)
            )

            StatusCard(
                "Connections",
                topology.links.size,
                "NETWORK LINKS",
                BrightBlue,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                "Normal",
                normalCount,
                "OPERATIONAL",
                Green,
                Modifier.weight(1f)
            )

            StatusCard(
                "Suspicious",
                suspiciousCount,
                "UNDER WATCH",
                Orange,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                "Isolated",
                isolatedCount,
                "ISOLATED NODES",
                Red,
                Modifier.weight(1f)
            )

            StatusCard(
                "Disconnected",
                disconnectedCount,
                "OFFLINE NODES",
                MutedText,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // SECURITY ALERTS

        SectionHeading("Security Center", "THREAT MONITOR")

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = NavyCardLight
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    "⚠  SECURITY ALERTS",
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {

                    alertCount != null -> {

                        Text(
                            text = alertCount.toString(),
                            color = if (alertCount!! > 0) Red else Green,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = if (alertCount!! > 0) {
                                "Malicious traffic alert(s) detected"
                            } else {
                                "No malicious traffic alerts detected"
                            },
                            color = SoftWhite,
                            fontSize = 14.sp
                        )
                    }

                    alertError -> {
                        Text(
                            "Unable to retrieve alert information.",
                            color = Red
                        )
                    }

                    else -> {
                        Text(
                            "Loading security alerts...",
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF153F68),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            refreshAlerts()
                        }
                ) {
                    Text(
                        text = "↻   Refresh Security Alerts",
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NETWORK INFORMATION

        SectionHeading("About NexTwin", "PROJECT INFORMATION")

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = NavyCard
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    "AI-Powered Campus Network Monitoring",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "NexTwin combines machine-learning-based " +
                        "traffic classification with a simulated " +
                        "campus network digital twin.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Network topology and device associations " +
                        "are simulated. This is not live campus traffic.",
                    color = ElectricBlue,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            title,
            color = SoftWhite,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            subtitle,
            color = ElectricBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    count: Int,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(125.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavyCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                title,
                color = MutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                count.toString(),
                color = accent,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                subtitle,
                color = MutedText,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
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
            .background(Navy)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            color = SoftWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "This module will be implemented in the next development step.",
            color = MutedText,
            fontSize = 16.sp
        )
    }
}