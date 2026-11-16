"""
NexTwin digital twin: what-if simulation.

All scenarios run on a COPY of the topology, inside the simulated
environment only. The original twin is never changed.
"""

import copy

import networkx as nx

from twin.topology import (
    STATUS_NORMAL,
    STATUS_SUSPICIOUS,
    STATUS_ISOLATED,
    STATUS_DISCONNECTED,
)

INTERNET_ID = "INET001"

# Scenario name -> the status the target device gets.
SCENARIOS = {
    "suspicious": STATUS_SUSPICIOUS,
    "disconnect": STATUS_DISCONNECTED,
    "isolate": STATUS_ISOLATED,
    "restore": STATUS_NORMAL,
}

# Devices in these states are offline: they cannot send or forward traffic.
OFFLINE_STATUSES = {STATUS_DISCONNECTED, STATUS_ISOLATED}


def reachable_devices(graph):
    """
    Return the set of campus devices that currently have a working path
    to the internet (INET001 itself is not counted).
    A path may only pass through devices that are online.
    """
    if INTERNET_ID not in graph:
        return set()
    if graph.nodes[INTERNET_ID]["status"] in OFFLINE_STATUSES:
        return set()

    online = [
        node for node, attrs in graph.nodes(data=True)
        if attrs["status"] not in OFFLINE_STATUSES
    ]
    online_graph = graph.subgraph(online)
    component = nx.node_connected_component(online_graph, INTERNET_ID)
    return set(component) - {INTERNET_ID}


def _build_summary(scenario, name, device_id, lost, gained):
    label = f"{name} ({device_id})"
    if scenario == "suspicious":
        return (f"{label} is marked suspicious. It is still online, so "
                f"connectivity is unchanged.")
    if scenario == "disconnect":
        return (f"{label} was disconnected. {len(lost)} other device(s) "
                f"lost internet connectivity.")
    if scenario == "isolate":
        return (f"{label} was isolated (quarantined). {len(lost)} other "
                f"device(s) lost internet connectivity.")
    return (f"{label} was restored to normal. {len(gained)} other "
            f"device(s) regained internet connectivity.")


def simulate(graph, device_id, scenario):
    """
    Run one what-if scenario.

    Returns (new_graph, result):
      new_graph - a modified COPY of the topology (the input is untouched)
      result    - a dictionary describing what happened

    Raises ValueError for an unknown device or scenario.
    """
    if scenario not in SCENARIOS:
        raise ValueError(
            f"Unknown scenario '{scenario}'. Valid: {sorted(SCENARIOS)}"
        )
    if device_id not in graph:
        raise ValueError(f"Unknown device '{device_id}'")

    reachable_before = reachable_devices(graph)

    new_graph = copy.deepcopy(graph)
    previous_status = new_graph.nodes[device_id]["status"]
    new_graph.nodes[device_id]["status"] = SCENARIOS[scenario]

    reachable_after = reachable_devices(new_graph)

    # The device we changed is reported through new_status, not here.
    lost = sorted((reachable_before - reachable_after) - {device_id})
    gained = sorted((reachable_after - reachable_before) - {device_id})
    offline = sorted(
        node for node, attrs in new_graph.nodes(data=True)
        if attrs["status"] in OFFLINE_STATUSES
    )
    name = new_graph.nodes[device_id]["name"]

    result = {
        "scenario": scenario,
        "device_id": device_id,
        "device_name": name,
        "previous_status": previous_status,
        "new_status": new_graph.nodes[device_id]["status"],
        "directly_affected": sorted(new_graph.neighbors(device_id)),
        "lost_connectivity": lost,
        "gained_connectivity": gained,
        "offline_devices": offline,
        "reachable_before": len(reachable_before),
        "reachable_after": len(reachable_after),
        "summary": _build_summary(scenario, name, device_id, lost, gained),
    }
    return new_graph, result


def _print_result(title, result):
    print(f"=== {title} ===")
    print(f"Device:              {result['device_id']} ({result['device_name']})")
    print(f"Status:              {result['previous_status']} -> {result['new_status']}")
    print(f"Directly affected:   {result['directly_affected']}")
    print(f"Lost connectivity:   {result['lost_connectivity']}")
    print(f"Gained connectivity: {result['gained_connectivity']}")
    print(f"Reachable devices:   {result['reachable_before']} -> {result['reachable_after']}")
    print(f"Summary:             {result['summary']}")
    print()


if __name__ == "__main__":
    from twin.topology import build_twin

    twin = build_twin()

    # Test 1: disconnect the lab switch
    broken, r = simulate(twin, "SW002", "disconnect")
    _print_result("TEST 1: disconnect SW002", r)

    # Test 2: isolate the web server (on the original twin)
    _, r = simulate(twin, "WEB001", "isolate")
    _print_result("TEST 2: isolate WEB001", r)

    # Test 3: mark the student laptop suspicious
    _, r = simulate(twin, "LAP001", "suspicious")
    _print_result("TEST 3: suspicious LAP001", r)

    # Test 4: restore SW002 on the graph that was broken in test 1
    _, r = simulate(broken, "SW002", "restore")
    _print_result("TEST 4: restore SW002 (after test 1)", r)

    # Test 5: the original twin must be unchanged
    unchanged = all(a["status"] == "normal" for _, a in twin.nodes(data=True))
    print(f"Original twin unchanged: {unchanged}")