
from pathlib import Path
from typing import Dict, Any

import joblib
import pandas as pd

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

# Digital Twin imports
from twin.topology import build_twin, get_device as twin_get_device
from twin.simulation import simulate
from twin.export import get_topology as export_topology


# --------------------------------------------------
# FASTAPI APPLICATION
# --------------------------------------------------

app = FastAPI(
    title="NexTwin API",
    description="Campus Network Security and Threat Detection Platform",
    version="2.0.0"
)


# --------------------------------------------------
# MODEL PATHS AND LOADING
# --------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_DIR = BASE_DIR / "models"

model = None
preprocessor = None


@app.on_event("startup")
def load_models():
    global model, preprocessor

    model_path = MODEL_DIR / "random_forest_model.joblib"
    preprocessor_path = MODEL_DIR / "preprocessor.joblib"

    if not model_path.exists():
        raise FileNotFoundError(f"Model file not found: {model_path}")

    if not preprocessor_path.exists():
        raise FileNotFoundError(
            f"Preprocessor file not found: {preprocessor_path}"
        )

    model = joblib.load(model_path)
    preprocessor = joblib.load(preprocessor_path)


# --------------------------------------------------
# DIGITAL TWIN INITIALIZATION
# --------------------------------------------------

# One persistent simulated campus network.
# This is NOT a real campus network.

twin_graph = build_twin()


# --------------------------------------------------
# ALERT STORAGE
# --------------------------------------------------

alerts = []
alert_counter = 1
total_predictions = 0


# --------------------------------------------------
# SIMULATED SERVICE-TO-DEVICE MAPPING
# --------------------------------------------------

SERVICE_DEVICE_MAPPING = {
    "http": "WEB001",
    "https": "WEB001",
    "dns": "DNS001",
    "ftp": "FILE001",
    "ssh": "ADMIN001"
}

DEFAULT_DEVICE = "GW001"


# --------------------------------------------------
# REQUEST MODELS
# --------------------------------------------------

class TrafficRequest(BaseModel):
    traffic: Dict[str, Any]


class SimulationRequest(BaseModel):
    device_id: str
    scenario: str


# --------------------------------------------------
# HEALTH CHECK
# --------------------------------------------------

@app.get("/health")
def health_check():

    return {
        "status": "success",
        "message": "NexTwin backend is running",
        "model_loaded": model is not None,
        "preprocessor_loaded": preprocessor is not None,
        "digital_twin_loaded": twin_graph is not None
    }


# --------------------------------------------------
# ALERTS API
# --------------------------------------------------

@app.get("/alerts")
def get_alerts():

    return {
        "total_alerts": len(alerts),
        "alerts": alerts
    }


# --------------------------------------------------
# SUMMARY API
# --------------------------------------------------

@app.get("/summary")
def get_summary():

    total_alerts = len(alerts)

    malicious_alerts = sum(
        1 for alert in alerts
        if alert["is_malicious"] is True
    )

    normal_predictions = total_predictions - malicious_alerts

    statuses = [
        data["status"]
        for _, data in twin_graph.nodes(data=True)
    ]

    active_devices = statuses.count("normal")

    suspicious_devices = statuses.count("suspicious")

    disconnected_devices = statuses.count("disconnected")

    isolated_devices = statuses.count("isolated")

    return {
        "total_alerts": total_alerts,
        "malicious_detections": malicious_alerts,
        "normal_detections": normal_predictions,
        "total_devices": len(statuses),
        "active_devices": active_devices,
        "suspicious_devices": suspicious_devices,
        "disconnected_devices": disconnected_devices,
        "isolated_devices": isolated_devices
    }


# --------------------------------------------------
# DIGITAL TWIN TOPOLOGY API
# --------------------------------------------------

@app.get("/topology")
def get_topology():

    return export_topology(twin_graph)


# --------------------------------------------------
# INDIVIDUAL DEVICE API
# --------------------------------------------------

@app.get("/devices/{device_id}")
def get_device_details(device_id: str):

    device = twin_get_device(
        twin_graph,
        device_id.upper()
    )

    if device is None:
        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    return device


# --------------------------------------------------
# DIGITAL TWIN SIMULATION API
# --------------------------------------------------

@app.post("/simulate")
def simulate_device(request: SimulationRequest):

    global twin_graph

    device_id = request.device_id.upper()
    scenario = request.scenario.lower()

    # Check whether the device exists.
    if twin_get_device(twin_graph, device_id) is None:
        raise HTTPException(
            status_code=404,
            detail="Device not found"
        )

    try:

        # Run the simulation on a copy of the graph.
        new_graph, result = simulate(
            twin_graph,
            device_id,
            scenario
        )

        # Save the updated graph.
        # This ensures simulations persist across requests.
        twin_graph = new_graph

        return {
            "result": result,
            "topology": export_topology(twin_graph)
        }

    except ValueError as error:

        raise HTTPException(
            status_code=400,
            detail=str(error)
        )


# --------------------------------------------------
# MACHINE LEARNING TRAFFIC PREDICTION API
# --------------------------------------------------

@app.post("/predict")
def predict_traffic(request: TrafficRequest):

    global alert_counter, total_predictions

    if model is None or preprocessor is None:
        raise HTTPException(
            status_code=503,
            detail="ML model or preprocessor is not loaded"
        )

    try:

        # Convert incoming traffic into DataFrame.
        traffic_df = pd.DataFrame([request.traffic])

        # Remove unwanted columns if supplied.
        traffic_df = traffic_df.drop(
            columns=["id", "attack_cat", "label"],
            errors="ignore"
        )

        # Get required model features.
        required_features = list(
            preprocessor.feature_names_in_
        )

        # Check for missing features.
        missing_features = [
            column for column in required_features
            if column not in traffic_df.columns
        ]

        if missing_features:

            raise HTTPException(
                status_code=400,
                detail={
                    "message": "Traffic record is missing required model features.",
                    "missing_features": missing_features
                }
            )

        # Arrange features in training order.
        traffic_df = traffic_df[required_features]

        print(
            "INPUT RECEIVED:",
            traffic_df.iloc[0].to_dict()
        )

        # Apply the existing fitted preprocessor.
        processed_data = preprocessor.transform(traffic_df)

        # Predict using the existing Random Forest model.
        probability = model.predict_proba(processed_data)[0]

        malicious_confidence = float(probability[1])

        # Retain the existing 0.70 decision threshold.
        prediction = int(malicious_confidence >= 0.70)

        total_predictions += 1

        generated_alert = None

        # Generate an alert for malicious traffic.
        if prediction == 1:

            service = str(
                request.traffic.get("service") or ""
            ).lower()

            # This is a simulated service-to-device mapping.
            device_id = SERVICE_DEVICE_MAPPING.get(
                service,
                DEFAULT_DEVICE
            )

            # Obtain connections from the actual simulated graph.
            mapped_device = twin_get_device(
                twin_graph,
                device_id
            )

            related_connections = (
                mapped_device["connected_to"]
                if mapped_device is not None
                else []
            )

            generated_alert = {
                "alert_id": f"ALT{alert_counter:03d}",
                "predicted_category": "malicious",
                "is_malicious": True,
                "traffic_details": {
                    "proto": request.traffic.get("proto"),
                    "service": request.traffic.get("service"),
                    "source_ip": request.traffic.get("source_ip"),
                    "destination_ip": request.traffic.get(
                        "destination_ip"
                    )
                },
                "device_id": device_id,
                "related_connections": related_connections,
                "mapping_note": "simulated mapping"
            }

            alerts.append(generated_alert)

            alert_counter += 1

        return {
            "predicted_category": (
                "malicious" if prediction == 1 else "normal"
            ),
            "is_malicious": bool(prediction == 1),
            "confidence": round(
                malicious_confidence,
                4
            ),
            "alert": generated_alert
        }

    except HTTPException:
        raise

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=str(error)
        )
