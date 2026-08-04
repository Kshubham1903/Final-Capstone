"""
=========================================================
Preprocessing Module
Student Growth Intelligence Engine
=========================================================

Responsibilities:
1. Load dataset
2. Create Risk Level target
3. Split features and target
4. Build preprocessing pipeline

Author: Anushka Kadam
=========================================================
"""

import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


# ==========================================================
# Load Dataset
# ==========================================================

def load_dataset(path="../dataset/StudentPerformanceFactors.csv"):
    """
    Load the student performance dataset.
    """
    df = pd.read_csv(path)
    return df


# ==========================================================
# Create Risk Target
# ==========================================================

def create_risk_target(df):
    """
    Create Risk_Level based on Exam_Score.

    Bottom 30%  -> High Risk
    Middle 40%  -> Medium Risk
    Top 30%     -> Low Risk
    """

    df = df.copy()

    # Calculate quantiles
    low_cutoff = df["Exam_Score"].quantile(0.30)
    high_cutoff = df["Exam_Score"].quantile(0.70)

    def assign_risk(score):
        if score <= low_cutoff:
            return "High Risk"
        elif score <= high_cutoff:
            return "Medium Risk"
        else:
            return "Low Risk"

    df["Risk_Level"] = df["Exam_Score"].apply(assign_risk)

    return df


# ==========================================================
# Split Features & Target
# ==========================================================

def split_features_target(df):
    """
    Separate features and target.
    """

    X = df.drop(columns=["Exam_Score", "Risk_Level"])
    y = df["Risk_Level"]

    return X, y


# ==========================================================
# Build Preprocessor
# ==========================================================

def build_preprocessor(X):
    """
    Build preprocessing pipeline.
    """

    numeric_features = X.select_dtypes(
        include=["int64", "float64"]
    ).columns.tolist()

    categorical_features = X.select_dtypes(
        include=["object"]
    ).columns.tolist()

    # -------------------------
    # Numeric Pipeline
    # -------------------------

    numeric_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler())
    ])

    # -------------------------
    # Categorical Pipeline
    # -------------------------

    categorical_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("encoder", OneHotEncoder(handle_unknown="ignore"))
    ])

    # -------------------------
    # Column Transformer
    # -------------------------

    preprocessor = ColumnTransformer([
        ("num", numeric_pipeline, numeric_features),
        ("cat", categorical_pipeline, categorical_features)
    ])

    return preprocessor


# ==========================================================
# Test Module
# ==========================================================

if __name__ == "__main__":

    print("=" * 60)
    print("PREPROCESSING MODULE")
    print("=" * 60)

    df = load_dataset()

    print("\nDataset Shape:", df.shape)

    df = create_risk_target(df)

    print("\nRisk Level Distribution")
    print(df["Risk_Level"].value_counts())

    X, y = split_features_target(df)

    print("\nFeatures Shape :", X.shape)
    print("Target Shape   :", y.shape)

    preprocessor = build_preprocessor(X)

    print("\nPreprocessor Created Successfully")

    print("\nNumeric Features")
    print(
        X.select_dtypes(include=["int64", "float64"]).columns.tolist()
    )

    print("\nCategorical Features")
    print(
        X.select_dtypes(include=["object"]).columns.tolist()
    )

    print("\nPreprocessing module is ready!")