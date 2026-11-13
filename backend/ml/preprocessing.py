
import pandas as pd
import joblib
from pathlib import Path

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler


BASE_DIR = Path(__file__).resolve().parent.parent

TRAIN_PATH = BASE_DIR / "data" / "raw" / "UNSW_NB15_training-set.csv"
TEST_PATH = BASE_DIR / "data" / "raw" / "UNSW_NB15_testing-set.csv"

OUTPUT_DIR = BASE_DIR / "data" / "processed"
MODEL_DIR = BASE_DIR / "models"

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
MODEL_DIR.mkdir(parents=True, exist_ok=True)


def preprocess_data():

    print("Loading datasets...")

    train_df = pd.read_csv(TRAIN_PATH)
    test_df = pd.read_csv(TEST_PATH)

    # Separate target column
    y_train = train_df["label"]
    y_test = test_df["label"]

    # Remove unwanted columns
    X_train = train_df.drop(columns=["id", "attack_cat", "label"])
    X_test = test_df.drop(columns=["id", "attack_cat", "label"])

    # Identify column types
    categorical_cols = X_train.select_dtypes(
        include=["object"]
    ).columns.tolist()

    numerical_cols = X_train.select_dtypes(
        include=["number"]
    ).columns.tolist()

    # Numerical preprocessing
    numerical_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler())
    ])

    # Categorical preprocessing
    categorical_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("encoder", OneHotEncoder(handle_unknown="ignore"))
    ])

    # Combine preprocessing
    preprocessor = ColumnTransformer([
        ("num", numerical_pipeline, numerical_cols),
        ("cat", categorical_pipeline, categorical_cols)
    ])

    print("Fitting preprocessing on training data...")

    X_train_processed = preprocessor.fit_transform(X_train)

    print("Transforming testing data...")

    X_test_processed = preprocessor.transform(X_test)

    # Save processed datasets
    joblib.dump(
        (X_train_processed, y_train),
        OUTPUT_DIR / "train_processed.pkl"
    )

    joblib.dump(
        (X_test_processed, y_test),
        OUTPUT_DIR / "test_processed.pkl"
    )

    # Save preprocessing pipeline
    joblib.dump(
        preprocessor,
        MODEL_DIR / "preprocessor.joblib"
    )

    print("\nPreprocessing completed successfully!")
    print("Training shape:", X_train_processed.shape)
    print("Testing shape:", X_test_processed.shape)
    print("Categorical columns:", categorical_cols)
    print("Numerical columns:", len(numerical_cols))


if __name__ == "__main__":
    preprocess_data()
