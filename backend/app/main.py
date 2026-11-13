
from fastapi import FastAPI
from pathlib import Path
import joblib
import pandas as pd
from typing import Dict, Any
from pydantic import BaseModel
from fastapi import HTTPException

app = FastAPI(
    title="NexTwin API",
    description="Campus Network Security and Threat Detection Platform",
    version="1.0.0"
)

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_DIR = BASE_DIR / "models"

model = None
preprocessor = None

# Security Alert Storage

alerts = []
alert_counter = 1
total_predictions = 0
# Simulated Service-to-Device Mapping

SERVICE_DEVICE_MAPPING = {
    "http": "WEB001",
    "https": "WEB001",
    "dns": "DNS001",
    "ftp": "FILE001",
    "ssh": "ADMIN001"
}

DEFAULT_DEVICE = "GW001"
# Dynamic Device Status Storage

DEVICE_STATUS = {
    "GW001": "active",
    "SW001": "active",
    "WEB001": "active",
    "DNS001": "active",
    "FILE001": "active",
    "ADMIN001": "active"
}
DEVICE_CONNECTION_MAPPING = {
    "GW001": ["SW001"],
    "SW001": ["GW001", "WEB001", "DNS001", "FILE001", "ADMIN001"],
    "WEB001": ["SW001"],
    "DNS001": ["SW001"],
    "FILE001": ["SW001"],
    "ADMIN001": ["SW001"]
}
class TrafficRequest(BaseModel):
    traffic: Dict[str, Any]

class SimulationRequest(BaseModel):
    device_id: str
    scenario: str

@app.on_event("startup")
def load_models():
    global model, preprocessor

    model_path = MODEL_DIR / "random_forest_model.joblib"
    preprocessor_path = MODEL_DIR / "preprocessor.joblib"

    model = joblib.load(model_path)
    preprocessor = joblib.load(preprocessor_path)


@app.get("/health")
def health_check():
    return {
        "status": "success",
        "message": "NexTwin backend is running",
        "model_loaded": model is not None,
        "preprocessor_loaded": preprocessor is not None
    }

@app.get("/alerts")
def get_alerts():
    return {
        "total_alerts": len(alerts),
        "alerts": alerts
    }

@app.get("/summary")
def get_summary():

    total_alerts = len(alerts)

    malicious_alerts = sum(
        1 for alert in alerts
        if alert["is_malicious"] is True
    )

    normal_predictions = total_predictions - malicious_alerts

    active_devices = sum(
        1 for status in DEVICE_STATUS.values()
        if status == "active"
    )

    suspicious_devices = sum(
        1 for status in DEVICE_STATUS.values()
        if status == "suspicious"
    )

    disconnected_devices = sum(
        1 for status in DEVICE_STATUS.values()
        if status == "disconnect"
    )

    isolated_devices = sum(
        1 for status in DEVICE_STATUS.values()
        if status == "isolate"
    )

    return {
        "total_alerts": total_alerts,
        "malicious_detections": malicious_alerts,
        "normal_detections": normal_predictions,
        "total_devices": len(DEVICE_STATUS),
        "active_devices": active_devices,
        "suspicious_devices": suspicious_devices,
        "disconnected_devices": disconnected_devices,
        "isolated_devices": isolated_devices
    }

@app.get("/topology")
def get_topology():
    devices = [
        {
            "device_id": "GW001",
            "device_name": "Default Gateway",
            "device_type": "Gateway",
            "status": DEVICE_STATUS["GW001"]
        },
        {
            "device_id": "SW001",
            "device_name": "Network Switch",
            "device_type": "Switch",
            "status": DEVICE_STATUS["SW001"]
        },
        {
            "device_id": "WEB001",
            "device_name": "Web Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["WEB001"]
        },
        {
            "device_id": "DNS001",
            "device_name": "DNS Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["DNS001"]
        },
        {
            "device_id": "FILE001",
            "device_name": "File Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["FILE001"]
        },
        {
            "device_id": "ADMIN001",
            "device_name": "Admin Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["ADMIN001"]
        }
    ]

    all_connections = [
    {"source": "GW001", "target": "SW001"},
    {"source": "SW001", "target": "WEB001"},
    {"source": "SW001", "target": "DNS001"},
    {"source": "SW001", "target": "FILE001"},
    {"source": "SW001", "target": "ADMIN001"}
    ]

    connections = [
        connection for connection in all_connections
    if DEVICE_STATUS[connection["source"]] == "active"
    and DEVICE_STATUS[connection["target"]] == "active"
   ]

      

    return {
        "topology_type": "simulated",
        "total_devices": len(devices),
        "devices": devices,
        "connections": connections
    }

@app.get("/devices/{device_id}")
def get_device(device_id: str):

    devices = {
        "GW001": {
            "device_id": "GW001",
            "device_name": "Default Gateway",
            "device_type": "Gateway",
            "status": DEVICE_STATUS["GW001"]
        },
        "SW001": {
            "device_id": "SW001",
            "device_name": "Network Switch",
            "device_type": "Switch",
            "status": DEVICE_STATUS["SW001"]
        },
        "WEB001": {
            "device_id": "WEB001",
            "device_name": "Web Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["WEB001"]
        },
        "DNS001": {
            "device_id": "DNS001",
            "device_name": "DNS Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["DNS001"]
        },
        "FILE001": {
            "device_id": "FILE001",
            "device_name": "File Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["FILE001"]
        },
        "ADMIN001": {
            "device_id": "ADMIN001",
            "device_name": "Admin Server",
            "device_type": "Server",
            "status": DEVICE_STATUS["ADMIN001"]
        }
    }

    device = devices.get(device_id.upper())

    if device is None:
        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    return device

@app.post("/simulate")
def simulate_device(request: SimulationRequest):

    device_id = request.device_id.upper()
    scenario = request.scenario.lower()

    valid_devices = [
        "GW001", "SW001", "WEB001",
        "DNS001", "FILE001", "ADMIN001"
    ]

    if device_id not in valid_devices:
        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    valid_scenarios = [
    "suspicious",
    "disconnect",
    "isolate",
    "restore"
    ]
    if scenario not in valid_scenarios:
        raise HTTPException(
            status_code=400,
            detail="Invalid scenario"
        )
    if scenario == "restore":
        DEVICE_STATUS[device_id] = "active"
    else:
        DEVICE_STATUS[device_id] = scenario
    return {
        "status": "success",
        "device_id": device_id,
        "scenario": scenario,
        "message": f"Simulation completed: {device_id} is now {scenario}",
        "simulation_note": "This is a simulated scenario"
    }


@app.post("/predict")
def predict_traffic(request: TrafficRequest):
    global alert_counter, total_predictions

    try:
        # Convert incoming traffic into DataFrame
        traffic_df = pd.DataFrame([request.traffic])

        # Remove unwanted columns if supplied
        traffic_df = traffic_df.drop(
            columns=["id", "attack_cat", "label"],
            errors="ignore"
        )

        # Check required features
        required_features = list(preprocessor.feature_names_in_)

        missing_features = [
            col for col in required_features
            if col not in traffic_df.columns
        ]

        if missing_features:
            raise HTTPException(
                status_code=400,
                detail={
                    "message": (f"{device_id} has been restored to active status"
                    if scenario == "restore"
                    else f"Simulation completed: {device_id} is now {scenario}"),
                    "missing_features": missing_features
                }
            )

        # Arrange features in training order
        traffic_df = traffic_df[required_features]

        print("INPUT RECEIVED:", traffic_df.iloc[0].to_dict())

        # Apply preprocessing
        processed_data = preprocessor.transform(traffic_df)

# Predict traffic using threshold
        probability = model.predict_proba(processed_data)[0]

        malicious_confidence = float(probability[1])

        prediction = int(malicious_confidence >= 0.70)

        total_predictions += 1

        generated_alert = None


        if prediction == 1:

            service = str(
                request.traffic.get("service") or ""
            ).lower()

            device_id = SERVICE_DEVICE_MAPPING.get(
                service, DEFAULT_DEVICE
            )

            generated_alert = {
                "alert_id": f"ALT{alert_counter:03d}",
                "predicted_category": "malicious",
                "is_malicious": True,
                "traffic_details": {
                    "proto": request.traffic.get("proto"),
                    "service": request.traffic.get("service"),
                    "source_ip": request.traffic.get("source_ip"),
                    "destination_ip": request.traffic.get("destination_ip")
                },
                "device_id": device_id,
                "related_connections": DEVICE_CONNECTION_MAPPING.get(device_id, []),
                "mapping_note": "simulated mapping"
            }

            alerts.append(generated_alert)
            alert_counter += 1

        return {
            "predicted_category": (
                "malicious" if prediction == 1 else "normal"
            ),
            "is_malicious": bool(prediction == 1),
            "confidence": round(malicious_confidence, 4),
            "alert": generated_alert
        }

    except HTTPException:
        raise

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )