
import joblib
import numpy as np
from pathlib import Path

from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report
)

BASE_DIR = Path(__file__).resolve().parent.parent

DATA_DIR = BASE_DIR / "data" / "processed"
MODEL_DIR = BASE_DIR / "models"

MODEL_DIR.mkdir(parents=True, exist_ok=True)


def train_model():

    print("Loading processed datasets...")

    X_train, y_train = joblib.load(
        DATA_DIR / "train_processed.pkl"
    )

    X_test, y_test = joblib.load(
        DATA_DIR / "test_processed.pkl"
    )

    print("Training Random Forest model...")

    model = RandomForestClassifier(
        n_estimators=100,
        random_state=42,
        n_jobs=-1,
        class_weight="balanced"
    )

    model.fit(X_train, y_train)

    print("Evaluating model...")

    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]
    print("\n========== THRESHOLD ANALYSIS ==========")
    for threshold in [0.50, 0.55, 0.60, 0.65, 0.70]:
        threshold_pred = (y_prob >= threshold).astype(int)
        print(f"\nThreshold: {threshold}")
        print(f"\nThreshold: {threshold}")
        print("Accuracy:", accuracy_score(y_test, threshold_pred))
        print("Precision:", precision_score(y_test, threshold_pred))
        print("Recall:", recall_score(y_test, threshold_pred))
        print("F1 Score:", f1_score(y_test, threshold_pred))
        print("Confusion Matrix:")
        print(confusion_matrix(y_test, threshold_pred))

    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)

    print("\n========== MODEL EVALUATION ==========")

    print(f"Accuracy:  {accuracy:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall:    {recall:.4f}")
    print(f"F1 Score:  {f1:.4f}")

    print("\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred))

    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

        # Feature Importance Analysis

    print("\n========== FEATURE IMPORTANCE ==========")

    preprocessor = joblib.load(
        MODEL_DIR / "preprocessor.joblib"
    )

    feature_names = preprocessor.get_feature_names_out()

    importances = model.feature_importances_

    feature_importance = sorted(
        zip(feature_names, importances),
        key=lambda x: x[1],
        reverse=True
    )

    print("\nTop 15 Important Features:")

    for feature, importance in feature_importance[:15]:
        print(f"{feature}: {importance:.4f}")

    joblib.dump(
        feature_importance,
        MODEL_DIR / "feature_importance.joblib"
    )

    print("\nSaving trained model...")

    joblib.dump(
        model,
        MODEL_DIR / "random_forest_model.joblib"
    )

    print("\nRandom Forest training completed!")
    print("Model saved successfully.")


if __name__ == "__main__":
    train_model()
