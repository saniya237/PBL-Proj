"""
NexTwin digital twin: simulated campus network topology.

This topology is a SIMULATION designed for the NexTwin project.
It is not a map of any real campus network.
"""

import networkx as nx

# Allowed device status values (agreed with the backend).
STATUS_NORMAL = "normal"
STATUS_SUSPICIOUS = "suspicious"
STATUS_ISOLATED = "isolated"
STATUS_DISCONNECTED = "disconnected"
VALID_STATUSES = {
    STATUS_NORMAL,
    STATUS_SUSPICIOUS,
    STATUS_ISOLATED,
    STATUS_DISCONNECTED,
}

# (id, name, type)
DEVICES = [
    ("INET001", "Internet", "cloud"),
    ("GW001", "Default Gateway", "router"),
    ("SW001", "Core Switch", "switch"),
    ("WEB001", "Web Server", "server"),
    ("DNS001", "DNS Server", "server"),
    ("FILE001", "File Server", "server"),
    ("ADMIN001", "Admin Server", "server"),
    ("SW002", "Lab Switch", "switch"),
    ("AP001", "Wi-Fi Access Point", "access_point"),
    ("LAB001", "Lab Computer 1", "workstation"),
    ("LAB002", "Lab Computer 2", "workstation"),
    ("CAM001", "CCTV Camera", "iot_device"),
    ("LAP001", "Student Laptop", "laptop"),
]

# Links between devices (undirected)
LINKS = [
    ("INET001", "GW001"),
    ("GW001", "SW001"),
    ("SW001", "WEB001"),
    ("SW001", "DNS001"),
    ("SW001", "FILE001"),
    ("SW001", "ADMIN001"),
    ("SW001", "SW002"),
    ("SW001", "AP001"),
    ("SW002", "LAB001"),
    ("SW002", "LAB002"),
    ("SW002", "CAM001"),
    ("AP001", "LAP001"),
]


def build_twin():
    """Build and return the campus topology as a NetworkX graph."""
    graph = nx.Graph()
    for device_id, name, device_type in DEVICES:
        graph.add_node(
            device_id,
            name=name,
            type=device_type,
            status=STATUS_NORMAL,
        )
    graph.add_edges_from(LINKS)
    return graph


def get_device(graph, device_id):
    """Return one device as a dictionary, or None if it does not exist."""
    if device_id not in graph:
        return None
    attrs = graph.nodes[device_id]
    return {
        "id": device_id,
        "name": attrs["name"],
        "type": attrs["type"],
        "status": attrs["status"],
        "connected_to": sorted(graph.neighbors(device_id)),
    }


def get_all_devices(graph):
    """Return all devices as a list of dictionaries."""
    return [get_device(graph, device_id) for device_id in graph.nodes]


def get_connections(graph):
    """Return all links as a list of {'source', 'target'} dictionaries."""
    return [{"source": a, "target": b} for a, b in graph.edges]


if __name__ == "__main__":
    twin = build_twin()
    print(f"Devices: {twin.number_of_nodes()}")
    print(f"Links:   {twin.number_of_edges()}")
    print(f"Fully connected: {nx.is_connected(twin)}")
    print()
    for device in get_all_devices(twin):
        print(f"{device['id']:<9} {device['name']:<20} {device['type']:<13} "
              f"{device['status']:<8} -> {device['connected_to']}")