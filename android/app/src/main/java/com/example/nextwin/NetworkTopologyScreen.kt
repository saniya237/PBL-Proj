
package com.example.nextwin

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextwin.data.Device
import com.example.nextwin.data.Topology
import kotlin.math.pow
import kotlin.math.sqrt

private val nodePositions = mapOf(
    "INET001" to Pair(0.50f, 0.07f),
    "GW001" to Pair(0.50f, 0.20f),
    "SW001" to Pair(0.50f, 0.36f),

    "WEB001" to Pair(0.09f, 0.54f),
    "DNS001" to Pair(0.27f, 0.54f),
    "FILE001" to Pair(0.45f, 0.54f),
    "ADMIN001" to Pair(0.63f, 0.54f),
    "SW002" to Pair(0.81f, 0.54f),
    "AP001" to Pair(0.94f, 0.36f),

    "LAB001" to Pair(0.65f, 0.80f),
    "LAB002" to Pair(0.81f, 0.80f),
    "CAM001" to Pair(0.94f, 0.80f),
    "LAP001" to Pair(0.94f, 0.66f)
)

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "normal" -> Color(0xFF35C979)
        "suspicious" -> Color(0xFFFFB020)
        "isolated" -> Color(0xFFEF5350)
        "disconnected" -> Color(0xFF909090)
        else -> Color(0xFF2196F3)
    }
}

@Composable
fun NetworkTopologyScreen(
    topology: Topology,
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember {
        mutableStateOf<Device?>(null)
    }

    Column(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
) {
        Text(
            text = "Network Topology",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Simulated Campus Network",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SAMPLE NETWORK",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Network visualization
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(530.dp)
                .pointerInput(topology) {
                    detectTapGestures { tap ->

                        val tappedDevice = topology.devices
                            .mapNotNull { device ->
                                val position = nodePositions[device.id]
                                    ?: return@mapNotNull null

                                val x = position.first * size.width
                                val y = position.second * size.height

                                val distance = sqrt(
                                    (tap.x - x).pow(2) +
                                    (tap.y - y).pow(2)
                                )

                                Pair(device, distance)
                            }
                            .filter { it.second < 65f }
                            .minByOrNull { it.second }

                        selectedDevice = tappedDevice?.first
                    }
                }
        ) {
            val nodeRadius = 28f

            // Draw actual connections from topology JSON
            topology.links.forEach { link ->

                val source = nodePositions[link.source]
                val target = nodePositions[link.target]

                if (source != null && target != null) {
                    drawLine(
                        color = Color(0xFF607D8B),
                        start = Offset(
                            source.first * size.width,
                            source.second * size.height
                        ),
                        end = Offset(
                            target.first * size.width,
                            target.second * size.height
                        ),
                        strokeWidth = 4f
                    )
                }
            }

            // Draw devices
            topology.devices.forEach { device ->

                val position = nodePositions[device.id]
                    ?: return@forEach

                val center = Offset(
                    position.first * size.width,
                    position.second * size.height
                )

                val color = statusColor(device.status)

                // Device circle
                drawCircle(
                    color = color,
                    radius = nodeRadius,
                    center = center
                )

                // Selected device outline
                if (selectedDevice?.id == device.id) {
                    drawCircle(
                        color = Color.White,
                        radius = nodeRadius + 7f,
                        center = center,
                        style = Stroke(width = 5f)
                    )
                }

                // Draw device ID below node
                drawIntoCanvas { canvas ->

                    val paint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        textSize = 23f
                        setColor(android.graphics.Color.WHITE)
                        typeface = Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                        )
                    }

                    canvas.nativeCanvas.drawText(
                        device.id,
                        center.x,
                        center.y + 53f,
                        paint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem("Normal", Color(0xFF35C979))
            LegendItem("Suspicious", Color(0xFFFFB020))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem("Isolated", Color(0xFFEF5350))
            LegendItem("Disconnected", Color(0xFF909090))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected device details
        selectedDevice?.let { device ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected Device",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("ID: ${device.id}")
                    Text("Name: ${device.name}")
                    Text("Type: ${device.type}")
                    Text("Status: ${device.status}")

                    Text(
                        text = "Connected to: ${
                            device.connectedTo.joinToString(", ")
                        }"
                    )
                }
            }
        } ?: Text(
            text = "Tap any network device to view its details."
        )
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row {
        Surface(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp),
            shape = CircleShape,
            color = color
        ) {}

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontSize = 12.sp
        )
    }
}
