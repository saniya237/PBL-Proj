
import joblib
import numpy as np
from pathlib import Path

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score
)

BASE_DIR = Path(__file__).resolve().parent.parent

DATA_DIR = BASE_DIR / "data" / "processed"
MODEL_DIR = BASE_DIR / "models"

MODEL_DIR.mkdir(parents=True, exist_ok=True)


def tune_model():

    print("Loading training data...")

    X, y = joblib.load(DATA_DIR / "train_processed.pkl")

    X_train, X_val, y_train, y_val = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    configurations = [
        {
            "n_estimators": 100,
            "max_depth": None,
            "min_samples_leaf": 1,
            "max_features": "sqrt",
            "class_weight": "balanced"
        },
        {
            "n_estimators": 100,
            "max_depth": 30,
            "min_samples_leaf": 2,
            "max_features": "sqrt",
            "class_weight": "balanced"
        },
        {
            "n_estimators": 100,
            "max_depth": 25,
            "min_samples_leaf": 2,
            "max_features": 0.7,
            "class_weight": "balanced_subsample"
        },
        {
            "n_estimators": 100,
            "max_depth": None,
            "min_samples_leaf": 2,
            "max_features": "sqrt",
            "class_weight": None
        }
    ]

    best_score = -1
    best_model = None

    for index, config in enumerate(configurations, start=1):

        print(f"\nTraining configuration {index}/4...")

        model = RandomForestClassifier(
            **config,
            random_state=42,
            n_jobs=-1
        )

        model.fit(X_train, y_train)

        predictions = model.predict(X_val)

        accuracy = accuracy_score(y_val, predictions)
        precision = precision_score(y_val, predictions)
        recall = recall_score(y_val, predictions)
        f1 = f1_score(y_val, predictions)

        # Equal importance to all four metrics
        balanced_score = np.mean([
            accuracy,
            precision,
            recall,
            f1
        ])

        print(f"Accuracy: {accuracy:.4f}")
        print(f"Precision: {precision:.4f}")
        print(f"Recall: {recall:.4f}")
        print(f"F1-score: {f1:.4f}")
        print(f"Balanced score: {balanced_score:.4f}")

        if balanced_score > best_score:
            best_score = balanced_score
            best_model = model

    print("\nBest configuration selected!")

    print("Retraining selected model on full training dataset...")

    best_model.fit(X, y)

    joblib.dump(
        best_model,
        MODEL_DIR / "tuned_random_forest_model.joblib"
    )

    print("Tuned model saved successfully!")

if __name__ == "__main__":
    tune_model()
