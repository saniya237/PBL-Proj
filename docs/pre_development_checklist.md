# NEXTWIN – PRE-DEVELOPMENT AGREEMENT CHECKLIST

**Project:** NexTwin – Campus Network Security Platform

**Team Members:** Part 1 (Backend & ML) and Part 2 (Digital Twin & Android)

**Purpose:** Finalize all technical decisions before starting implementation.

## 1. Dataset and Preprocessing

Proposed decisions:
- Dataset: UNSW-NB15.
- Target column: `label` (0 = Normal, 1 = Malicious).
- Drop `id` and exclude `attack_cat` from input features.
- Check missing values, invalid values and duplicates.
- Encode categorical features using One-Hot Encoding.
- Fit preprocessing on training data only.
- Apply the same preprocessing to testing data.
- Never modify original dataset files.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| Dataset and preprocessing finalized | [ ] | [ ] |

## 2. Random Forest Model

Proposed decisions:
- Algorithm: Random Forest Classifier.
- Library: Scikit-learn.
- Classification: Binary (Normal / Malicious).
- Evaluation: Accuracy, Precision, Recall and F1-score.
- Additional evaluation: Confusion Matrix and Feature Importance.
- Save and load the trained pipeline using Joblib.
- Give special importance to attack detection recall.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| ML model and evaluation finalized | [ ] | [ ] |

## 3. Digital Twin and Device Mapping

Proposed service-to-device mapping:

| Network Service | Simulated Device |
|---|---|
| HTTP / HTTPS | Web Server |
| DNS | DNS Server |
| FTP | File Server |
| SSH | Admin Server |
| Unknown / Missing | Default Gateway |

Additional decisions:
- Part 2 will provide the device list and unique device IDs.
- Part 1 will use those IDs while generating alerts.
- Every mapping will be identified as a simulated mapping.
- The campus network topology is a simulation, not a real campus network.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| Device IDs finalized | [ ] | [ ] |
| Service mapping rules finalized | [ ] | [ ] |
| Device list shared | [ ] | [ ] |

## 4. Alert JSON Format

Both parts will follow this common structure:

```json
{
  "alert_id": "ALT001",
  "predicted_category": "malicious",
  "is_malicious": true,
  "traffic_details": {
    "proto": "tcp",
    "service": "http",
    "source_ip": "192.168.1.10",
    "destination_ip": "192.168.1.20"
  },
  "device_id": "SRV001",
  "related_connections": ["SW001"],
  "mapping_note": "simulated mapping"
}
```

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| Alert JSON structure finalized | [ ] | [ ] |
| Field names and data types agreed | [ ] | [ ] |

## 5. FastAPI API Contract

Proposed endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | Check backend status |
| GET | `/alerts` | Fetch security alerts |
| GET | `/topology` | Fetch network topology |
| GET | `/devices/{id}` | Fetch device details |
| POST | `/simulate` | Execute what-if simulation |
| GET | `/summary` | Fetch detection summary |

Additional decisions:
- Backend: Python 3.12 + FastAPI.
- Android communicates with the backend using HTTP requests.
- Both parts will follow the same request and response JSON formats.
- Simulation requests will use a device ID and scenario type.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| API endpoints finalized | [ ] | [ ] |
| Request and response formats finalized | [ ] | [ ] |
| Backend and Android communication agreed | [ ] | [ ] |

## 6. GitHub Workflow

Proposed decisions:
- One shared private GitHub repository.
- Part 1 branch: `part1-backend`.
- Part 2 branch: `part2-twin-android`.
- Each member works in their assigned folders.
- Pull the latest changes before starting work.
- Commit and push changes regularly.
- Merge tested work into `main`.
- Do not modify another member's folders without discussion.
- Python virtual environments and original datasets will not be pushed.
- Shared dependencies will be maintained in the agreed requirements file.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| Branch names finalized | [ ] | [ ] |
| Folder ownership finalized | [ ] | [ ] |
| Git workflow finalized | [ ] | [ ] |
| Requirements and .gitignore finalized | [ ] | [ ] |

## 7. Testing and Final Demonstration

Proposed demonstration flow:

1. Load the UNSW-NB15 dataset.
2. Perform preprocessing.
3. Train and evaluate the Random Forest model.
4. Generate predictions and security alerts.
5. Associate alerts with simulated network devices.
6. Display alerts in the Android application.
7. Display the NetworkX campus topology.
8. Run what-if simulation scenarios.
9. Display the detection summary.

Additional decisions:
- Use actual model predictions and measured evaluation results.
- Use clearly labelled simulated device mappings.
- Test all API endpoints.
- Test the complete Android-to-FastAPI connection.
- Verify that all six Android screens work.

| Agreement | Part 1 | Part 2 |
|---|:---:|:---:|
| Testing procedure finalized | [ ] | [ ] |
| Demo flow finalized | [ ] | [ ] |
| Integration testing agreed | [ ] | [ ] |

---

## FINAL APPROVAL

| Confirmation | Part 1 | Part 2 |
|---|:---:|:---:|
| All pre-development decisions reviewed | [ ] | [ ] |
| Ready to start implementation | [ ] | [ ] |

**Rule:** Both members must review and agree before marking their respective checkboxes. Any changes to the agreed formats or rules must be discussed and updated in this document.