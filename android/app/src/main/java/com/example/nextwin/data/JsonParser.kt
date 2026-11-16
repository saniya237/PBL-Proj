
package com.example.nextwin.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray.toStringList(): List<String> =
    List(length()) { getString(it) }

object JsonParser {

    fun parseDevice(o: JSONObject) = Device(
        id = o.getString("id"),
        name = o.getString("name"),
        type = o.getString("type"),
        status = o.getString("status"),
        connectedTo = o.getJSONArray("connected_to").toStringList()
    )

    fun parseTopology(o: JSONObject): Topology {
        val deviceArray = o.getJSONArray("devices")
        val linkArray = o.getJSONArray("links")

        return Topology(
            devices = List(deviceArray.length()) {
                parseDevice(deviceArray.getJSONObject(it))
            },
            links = List(linkArray.length()) {
                val l = linkArray.getJSONObject(it)
                Link(
                    l.getString("source"),
                    l.getString("target")
                )
            }
        )
    }

    fun parseTopology(json: String): Topology =
        parseTopology(JSONObject(json))

    fun parseSimulationResponse(json: String): SimulationResponse {
        val root = JSONObject(json)
        val r = root.getJSONObject("result")

        val result = SimulationResult(
            scenario = r.getString("scenario"),
            deviceId = r.getString("device_id"),
            deviceName = r.getString("device_name"),
            previousStatus = r.getString("previous_status"),
            newStatus = r.getString("new_status"),
            directlyAffected = r.getJSONArray("directly_affected").toStringList(),
            lostConnectivity = r.getJSONArray("lost_connectivity").toStringList(),
            gainedConnectivity = r.getJSONArray("gained_connectivity").toStringList(),
            offlineDevices = r.getJSONArray("offline_devices").toStringList(),
            reachableBefore = r.getInt("reachable_before"),
            reachableAfter = r.getInt("reachable_after"),
            summary = r.getString("summary")
        )

        val topology = parseTopology(root.getJSONObject("topology"))

        return SimulationResponse(
            result = result,
            topology = topology
        )
    }
}
