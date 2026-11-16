
package com.example.nextwin

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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

private val HeadingWhite = Color(0xFFF1F7FF)
private val SubText = Color(0xFFB0C4DE)
private val AccentCyan = Color(0xFF38BDF8)
private val CardBlue = Color(0xFF142B47)

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
            fontWeight = FontWeight.Bold,
            color = HeadingWhite
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Simulated Campus Network",
            style = MaterialTheme.typography.bodyMedium,
            color = SubText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SAMPLE NETWORK",
            color = AccentCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

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

            // Draw network connections
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

                val deviceColor = statusColor(device.status)

                drawCircle(
                    color = deviceColor,
                    radius = nodeRadius,
                    center = center
                )

                // Selected device outline
                if (selectedDevice?.id == device.id) {

                    drawCircle(
                        color = AccentCyan,
                        radius = nodeRadius + 7f,
                        center = center,
                        style = Stroke(width = 5f)
                    )
                }

                // Device ID label
                drawIntoCanvas { canvas ->

                    val paint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        textSize = 19f

                        setColor(
                            android.graphics.Color.rgb(35, 45, 60)
                        )

                        typeface = Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                        )
                    }

                    val label = device.id
                    val textWidth = paint.measureText(label)

                    val padding = 9f
                    val labelX = center.x
                    val labelY = center.y + 54f

                    val backgroundPaint = Paint().apply {
                        isAntiAlias = true

                        setColor(
                            android.graphics.Color.rgb(235, 239, 250)
                        )
                    }

                    val rect = RectF(
                        labelX - textWidth / 2 - padding,
                        labelY - 21f,
                        labelX + textWidth / 2 + padding,
                        labelY + 8f
                    )

                    canvas.nativeCanvas.drawRoundRect(
                        rect,
                        10f,
                        10f,
                        backgroundPaint
                    )

                    canvas.nativeCanvas.drawText(
                        label,
                        labelX,
                        labelY,
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

        Spacer(modifier = Modifier.height(16.dp))

        // Selected device details
        selectedDevice?.let { device ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = CardBlue
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Selected Device",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DeviceDetail("Device ID", device.id)
                    DeviceDetail("Name", device.name)
                    DeviceDetail("Type", device.type)
                    DeviceDetail("Status", device.status)

                    DeviceDetail(
                        "Connected To",
                        device.connectedTo.joinToString(", ")
                    )
                }
            }

        } ?: Text(
            text = "Tap any network device to view its details.",
            color = SubText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DeviceDetail(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        Text(
            text = title,
            color = SubText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            color = HeadingWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp),
            shape = CircleShape,
            color = color
        ) {}

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = HeadingWhite
        )
    }
}
