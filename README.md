
# NexTwin: AI-Based Digital Twin for Campus Network Security

A mobile-based campus network security platform combining ML-based anomaly detection (Random Forest on UNSW-NB15), a simulated campus network digital twin (NetworkX), security alerts with topology context, and what-if simulation. Backend: Python/FastAPI. App: Kotlin/Jetpack Compose.

Note: UNSW-NB15 is a benchmark dataset, not real campus traffic. The campus topology and the traffic-to-device mapping are simulated.

## Structure
- backend/data: dataset (not stored in Git; download separately into data/raw)
- backend/ml: preprocessing and Random Forest
- backend/twin: NetworkX digital twin
- backend/models: saved models (not stored in Git)
- backend/app: FastAPI backend
- android: Android app
- docs: documentation and API contract
