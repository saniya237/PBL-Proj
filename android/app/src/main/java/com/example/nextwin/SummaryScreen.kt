
package com.example.nextwin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextwin.data.ApiClient
import com.example.nextwin.data.SummaryResponse
import com.example.nextwin.data.Topology
import kotlinx.coroutines.launch

private val SummaryText = Color(0xFFEAF4FF)
private val SummaryMuted = Color(0xFFA8BCD4)
private val SummaryCyan = Color(0xFF38BDF8)
private val SummaryCardColor = Color(0xFF1B2D49)
private val SummaryTrack = Color(0xFF30445F)

@Composable
fun SummaryScreen(
    topology: Topology,
    modifier: Modifier = Modifier
) {
    var mlSummary by remember {
        mutableStateOf<SummaryResponse?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    fun loadSummary() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                mlSummary = ApiClient.getSummary()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load summary"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSummary()
    }

    // DIGITAL TWIN DEVICE STATISTICS

    val total = topology.devices.size

    val normal = topology.devices.count {
        it.status.equals("normal", ignoreCase = true)
    }

    val suspicious = topology.devices.count {
        it.status.equals("suspicious", ignoreCase = true)
    }

    val isolated = topology.devices.count {
        it.status.equals("isolated", ignoreCase = true)
    }

    val disconnected = topology.devices.count {
        it.status.equals("disconnected", ignoreCase = true)
    }

    val internetReachable =
        (total - 1 - disconnected - isolated).coerceAtLeast(0)

    val networkDeviceCount = (total - 1).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Detection Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SummaryText
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "CAMPUS NETWORK OVERVIEW",
            color = SummaryCyan,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // DEVICE STATISTICS

        SummaryHeading("Device Statistics")

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("Total Devices", total, Modifier.weight(1f))
            SummaryCard("Normal", normal, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("Suspicious", suspicious, Modifier.weight(1f))
            SummaryCard("Isolated", isolated, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("Disconnected", disconnected, Modifier.weight(1f))
            SummaryCard("Connections", topology.links.size, Modifier.weight(1f))
        }

        Spacer(Modifier.height(26.dp))

        // DEVICE STATUS DISTRIBUTION

        SummaryHeading("Device Status Distribution")

        Spacer(Modifier.height(12.dp))

        StatusDistribution("Normal", normal, total, Color(0xFF35D399))
        StatusDistribution("Suspicious", suspicious, total, Color(0xFFFFB020))
        StatusDistribution("Isolated", isolated, total, Color(0xFFFF647C))
        StatusDistribution("Disconnected", disconnected, total, Color(0xFF94A3B8))

        Spacer(Modifier.height(26.dp))

        // NETWORK CONNECTIVITY

        SummaryHeading("Network Connectivity")

        Spacer(Modifier.height(12.dp))

        SummaryInfoCard(
            "Internet Reachability",
            "$internetReachable of $networkDeviceCount network devices reachable"
        )

        Spacer(Modifier.height(12.dp))

        SummaryInfoCard(
            "Network Connections",
            "${topology.links.size} links across ${topology.devices.size} devices"
        )

        Spacer(Modifier.height(26.dp))

        // ML DETECTION STATISTICS

        SummaryHeading("ML Detection Statistics")

        Spacer(Modifier.height(12.dp))

        when {

            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SummaryCyan
                    )
                }
            }

            errorMessage != null -> {

                SummaryInfoCard(
                    "Backend Connection Error",
                    errorMessage ?: "Unable to load ML statistics"
                )

                TextButton(
                    onClick = { loadSummary() }
                ) {
                    Text(
                        "Retry",
                        color = SummaryCyan
                    )
                }
            }

            mlSummary != null -> {

                val data = mlSummary!!

                SummaryInfoCard(
                    "Detection Status",
                    "ML backend connected successfully"
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        "Total Alerts",
                        data.totalAlerts,
                        Modifier.weight(1f)
                    )

                    SummaryCard(
                        "Malicious",
                        data.maliciousDetections,
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        "Normal Detections",
                        data.normalDetections,
                        Modifier.weight(1f)
                    )

                    SummaryCard(
                        "Total Predictions",
                        data.normalDetections + data.maliciousDetections,
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                SummaryInfoCard(
                    "Backend Device Statistics",
                    """
                    Total devices: ${data.totalDevices}
                    Active devices: ${data.activeDevices}
                    Suspicious devices: ${data.suspiciousDevices}
                    Disconnected devices: ${data.disconnectedDevices}
                    Isolated devices: ${data.isolatedDevices}
                    """.trimIndent()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "ML detection statistics are retrieved from the FastAPI backend. Device status figures are calculated from the simulated NetworkX topology. These represent separate measurements.",
            style = MaterialTheme.typography.bodySmall,
            color = SummaryMuted
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "This summary represents the simulated campus network topology. It does not represent live campus traffic.",
            style = MaterialTheme.typography.bodySmall,
            color = SummaryMuted
        )

        Spacer(Modifier.height(20.dp))
    }
}

// SECTION HEADING

@Composable
private fun SummaryHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = SummaryText
    )
}

// SUMMARY CARD

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = SummaryCardColor
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
                style = MaterialTheme.typography.bodyMedium,
                color = SummaryMuted
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SummaryCyan
            )
        }
    }
}

// DEVICE STATUS DISTRIBUTION

@Composable
private fun StatusDistribution(
    label: String,
    count: Int,
    total: Int,
    progressColor: Color
) {
    val progress =
        if (total > 0) count.toFloat() / total else 0f

    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                color = SummaryText
            )

            Text(
                text = "$count / $total",
                color = SummaryMuted
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp),
            color = progressColor,
            trackColor = SummaryTrack
        )
    }
}

// INFORMATION CARD

@Composable
private fun SummaryInfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SummaryCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = SummaryText
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = SummaryMuted
            )
        }
    }
}
