package com.example.nextwin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextwin.data.Topology

@Composable
fun SummaryScreen(
    topology: Topology,
    modifier: Modifier = Modifier
) {
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

    val internetReachable = (total - 1 - disconnected - isolated).coerceAtLeast(0)
val networkDeviceCount = (total - 1).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "Detection Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "CAMPUS NETWORK OVERVIEW",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Device Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

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

        Spacer(Modifier.height(24.dp))

        Text(
            "Device Status Distribution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        StatusDistribution(
            label = "Normal",
            count = normal,
            total = total
        )

        StatusDistribution(
            label = "Suspicious",
            count = suspicious,
            total = total
        )

        StatusDistribution(
            label = "Isolated",
            count = isolated,
            total = total
        )

        StatusDistribution(
            label = "Disconnected",
            count = disconnected,
            total = total
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Network Connectivity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

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

        Spacer(Modifier.height(24.dp))

        Text(
            "ML Detection Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        SummaryInfoCard(
            "Detection Status",
            "Pending backend integration"
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Actual traffic classification, prediction counts, accuracy and detection metrics will be displayed after connecting the ML detection backend. Device status figures above are calculated from the current topology data, not ML predictions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "This summary represents the simulated campus network topology. It does not represent live campus traffic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
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
            Text(title, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusDistribution(
    label: String,
    count: Int,
    total: Int
) {
    val progress = if (total > 0) count.toFloat() / total else 0f

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Text("$count / $total")
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
        )
    }
}

@Composable
private fun SummaryInfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
