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
            devices = List(deviceArray.length()) { parseDevice(deviceArray.getJSONObject(it)) },
            links = List(linkArray.length()) {
                val l = linkArray.getJSONObject(it)
                Link(l.getString("source"), l.getString("target"))
            }
        )
    }

    fun parseTopology(json: String): Topology = parseTopology(JSONObject(json))
}