package com.example.nextwin.data

data class Device(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val connectedTo: List<String>
)

data class Link(
    val source: String,
    val target: String
)

data class Topology(
    val devices: List<Device>,
    val links: List<Link>
)