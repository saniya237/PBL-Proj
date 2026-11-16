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
import androidx.compose.ui.unit.sp
import com.example.nextwin.data.ApiClient
import com.example.nextwin.data.SecurityAlert
import kotlinx.coroutines.launch

private val AlertWhite = Color(0xFFEAF4FF)
private val AlertCyan = Color(0xFF38BDF8)
private val AlertMuted = Color(0xFFB8C9DD)
private val AlertCard = Color(0xFF172B46)
private val AlertRed = Color(0xFFFB7185)
private val AlertGreen = Color(0xFF34D399)

@Composable
fun AlertsScreen(
    modifier: Modifier = Modifier
) {
    var alerts by remember {
        mutableStateOf<List<SecurityAlert>>(emptyList())
    }

    var totalAlerts by remember {
        mutableIntStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    fun loadAlerts() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = ApiClient.getAlerts()
                alerts = response.alerts
                totalAlerts = response.totalAlerts
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load alerts"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAlerts()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Security Alerts",
            color = AlertWhite,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "NETWORK SECURITY MONITORING",
            color = AlertCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Alert Overview",
            color = AlertWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        AlertCountCard(
            title = "Total Alerts",
            count = totalAlerts,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Alerts",
                color = AlertWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { loadAlerts() },
                enabled = !isLoading
            ) {
                Text(
                    "Refresh",
                    color = AlertCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AlertCyan
                    )
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AlertCard
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Unable to Load Alerts",
                            color = AlertRed,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "",
                            color = AlertMuted
                        )
                    }
                }
            }

            alerts.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AlertCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Alerts Available",
                            color = AlertGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "No malicious traffic alerts have been generated yet.",
                            color = AlertMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            else -> {
                alerts.forEach { alert ->
                    SecurityAlertCard(alert = alert)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Alerts are retrieved from the NexTwin ML detection backend. Device associations use simulated service-to-device mapping.",
            color = AlertMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SecurityAlertCard(
    alert: SecurityAlert
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = AlertCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Text(
                text = alert.alertId,
                color = AlertWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "MALICIOUS TRAFFIC DETECTED",
                color = AlertRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color(0xFF344966)
            )

            AlertDetail("Category", alert.predictedCategory)
            AlertDetail("Device ID", alert.deviceId)
            AlertDetail("Protocol", alert.trafficDetails.proto ?: "N/A")
            AlertDetail("Service", alert.trafficDetails.service ?: "N/A")
            AlertDetail("Source IP", alert.trafficDetails.sourceIp ?: "N/A")
            AlertDetail("Destination IP", alert.trafficDetails.destinationIp ?: "N/A")

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Related Connections",
                color = AlertCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (alert.relatedConnections.isEmpty()) {
                    "No related connections"
                } else {
                    alert.relatedConnections.joinToString(", ")
                },
                color = AlertWhite,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = alert.mappingNote,
                color = AlertMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AlertDetail(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = AlertMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            color = AlertWhite,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AlertCountCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(125.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = AlertCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = AlertMuted,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = count.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AlertCyan
            )
        }
    }
}