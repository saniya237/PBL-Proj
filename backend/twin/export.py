"""
NexTwin digital twin: JSON export.

Turns the topology and simulation results into plain dictionaries / JSON
for the Android app and the FastAPI backend.
"""

import json
from pathlib import Path

from twin.topology import (
    build_twin,
    get_all_devices,
    get_connections,
    get_device,
)
from twin.simulation import simulate


def get_topology(graph):
    """Whole network: all devices (with status) and all links."""
    return {
        "devices": get_all_devices(graph),
        "links": get_connections(graph),
    }


def simulation_response(graph, device_id, scenario):
    """
    Run a scenario and return what the app needs in ONE response:
    the simulation result plus the updated topology.
    Raises ValueError for an unknown device or scenario.
    """
    new_graph, result = simulate(graph, device_id, scenario)
    return {
        "result": result,
        "topology": get_topology(new_graph),
    }


def save_json(data, path):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Saved: {path}")


if __name__ == "__main__":
    twin = build_twin()
    out_dir = Path(__file__).resolve().parent.parent.parent / "docs" / "sample_data"

    save_json(get_topology(twin), out_dir / "topology.json")
    save_json(get_device(twin, "WEB001"), out_dir / "device_WEB001.json")
    save_json(
        simulation_response(twin, "SW002", "disconnect"),
        out_dir / "simulation_disconnect_SW002.json",
    )