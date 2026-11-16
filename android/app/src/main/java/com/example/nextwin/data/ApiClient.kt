
package com.example.nextwin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import org.json.JSONObject

// HEALTH RESPONSE MODEL
data class HealthResponse(
    val status: String,
    val message: String,
    val modelLoaded: Boolean,
    val preprocessorLoaded: Boolean,
    val digitalTwinLoaded: Boolean
)

// ALERT TRAFFIC DETAILS
data class AlertTrafficDetails(
    val proto: String?,
    val service: String?,
    val sourceIp: String?,
    val destinationIp: String?
)

// INDIVIDUAL ALERT MODEL
data class SecurityAlert(
    val alertId: String,
    val predictedCategory: String,
    val isMalicious: Boolean,
    val trafficDetails: AlertTrafficDetails,
    val deviceId: String,
    val relatedConnections: List<String>,
    val mappingNote: String
)

// ALERTS API RESPONSE MODEL
data class AlertsResponse(
    val totalAlerts: Int,
    val alerts: List<SecurityAlert>
)

// SUMMARY RESPONSE MODEL
data class SummaryResponse(
    val totalAlerts: Int,
    val maliciousDetections: Int,
    val normalDetections: Int,
    val totalDevices: Int,
    val activeDevices: Int,
    val suspiciousDevices: Int,
    val disconnectedDevices: Int,
    val isolatedDevices: Int
)

object ApiClient {

    private const val BASE_URL = "http://10.94.101.153:8000/"

    private val client = OkHttpClient()

    // 1. HEALTH CHECK
    suspend fun getHealth(): HealthResponse =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url("${BASE_URL}health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    throw Exception("Backend error: ${response.code}")
                }

                val json = JSONObject(
                    response.body?.string()
                        ?: throw Exception("Empty backend response")
                )

                HealthResponse(
                    status = json.getString("status"),
                    message = json.getString("message"),
                    modelLoaded = json.getBoolean("model_loaded"),
                    preprocessorLoaded = json.getBoolean("preprocessor_loaded"),
                    digitalTwinLoaded = json.getBoolean("digital_twin_loaded")
                )
            }
        }

    // 2. GET LIVE TOPOLOGY
    suspend fun getTopology(): Topology =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url("${BASE_URL}topology")
                .get()
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty topology response")

                if (!response.isSuccessful) {
                    throw Exception("Topology API error: $responseBody")
                }

                JsonParser.parseTopology(responseBody)
            }
        }

    // 3. RUN NETWORKX SIMULATION
    suspend fun runSimulation(
        deviceId: String,
        scenario: String
    ): SimulationResponse =
        withContext(Dispatchers.IO) {

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("scenario", scenario)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${BASE_URL}simulate")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty simulation response")

                if (!response.isSuccessful) {
                    throw Exception("Simulation API error: $responseBody")
                }

                JsonParser.parseSimulationResponse(responseBody)
            }
        }

    // 4. GET SECURITY ALERTS
    suspend fun getAlerts(): AlertsResponse =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url("${BASE_URL}alerts")
                .get()
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty alerts response")

                if (!response.isSuccessful) {
                    throw Exception("Alerts API error: $responseBody")
                }

                val json = JSONObject(responseBody)
                val alertArray = json.getJSONArray("alerts")

                val alertList = mutableListOf<SecurityAlert>()

                for (i in 0 until alertArray.length()) {

                    val item = alertArray.getJSONObject(i)

                    val traffic = item.optJSONObject("traffic_details")
                        ?: JSONObject()

                    val connections = item.optJSONArray("related_connections")

                    val connectionList = mutableListOf<String>()

                    if (connections != null) {
                        for (j in 0 until connections.length()) {
                            connectionList.add(connections.getString(j))
                        }
                    }

                    alertList.add(
                        SecurityAlert(
                            alertId = item.getString("alert_id"),
                            predictedCategory = item.getString("predicted_category"),
                            isMalicious = item.getBoolean("is_malicious"),

                            trafficDetails = AlertTrafficDetails(
                                proto = traffic.optString("proto")
                                    .takeIf { it != "null" },

                                service = traffic.optString("service")
                                    .takeIf { it != "null" },

                                sourceIp = traffic.optString("source_ip")
                                    .takeIf { it != "null" },

                                destinationIp = traffic.optString("destination_ip")
                                    .takeIf { it != "null" }
                            ),

                            deviceId = item.getString("device_id"),
                            relatedConnections = connectionList,
                            mappingNote = item.optString("mapping_note")
                        )
                    )
                }

                AlertsResponse(
                    totalAlerts = json.getInt("total_alerts"),
                    alerts = alertList
                )
            }
        }

    // 5. GET ML DETECTION SUMMARY
    suspend fun getSummary(): SummaryResponse =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url("${BASE_URL}summary")
                .get()
                .build()

            client.newCall(request).execute().use { response ->

                val body = response.body?.string()
                    ?: throw Exception("Empty summary response")

                if (!response.isSuccessful) {
                    throw Exception("Summary API error: $body")
                }

                val json = JSONObject(body)

                SummaryResponse(
                    totalAlerts = json.getInt("total_alerts"),
                    maliciousDetections = json.getInt("malicious_detections"),
                    normalDetections = json.getInt("normal_detections"),
                    totalDevices = json.getInt("total_devices"),
                    activeDevices = json.getInt("active_devices"),
                    suspiciousDevices = json.getInt("suspicious_devices"),
                    disconnectedDevices = json.getInt("disconnected_devices"),
                    isolatedDevices = json.getInt("isolated_devices")
                )
            }
        }
}
