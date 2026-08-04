"""
=========================================================
Train Student Risk Prediction Model
Student Growth Intelligence Engine
=========================================================

Responsibilities:
1. Load dataset
2. Create Risk_Level target
3. Preprocess data
4. Hyperparameter tuning
5. Train best model
6. Evaluate model
7. Save trained pipeline

Author: Anushka Kadam
=========================================================
"""

import os
import warnings
import joblib
import pandas as pd

from sklearn.pipeline import Pipeline
from sklearn.model_selection import (
    train_test_split,
    RandomizedSearchCV,
    StratifiedKFold
)

from sklearn.preprocessing import LabelEncoder

from sklearn.linear_model import LogisticRegression

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix
)

from preprocess import (
    load_dataset,
    create_risk_target,
    split_features_target,
    build_preprocessor
)

warnings.filterwarnings("ignore")

# ==========================================================
# Create Output Directories
# ==========================================================

os.makedirs("../outputs/models", exist_ok=True)
os.makedirs("../outputs/reports", exist_ok=True)

# ==========================================================
# Load Dataset
# ==========================================================

print("=" * 70)
print("TRAINING STUDENT RISK MODEL")
print("=" * 70)

df = load_dataset()

print("\nDataset Loaded")
print("Shape :", df.shape)

# ==========================================================
# Create Risk Target
# ==========================================================

df = create_risk_target(df)

print("\nRisk Distribution")
print(df["Risk_Level"].value_counts())

# ==========================================================
# Features & Target
# ==========================================================

X, y = split_features_target(df)

label_encoder = LabelEncoder()
y = label_encoder.fit_transform(y)

# ==========================================================
# Train Test Split
# ==========================================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)

print("\nTraining Samples :", X_train.shape[0])
print("Testing Samples  :", X_test.shape[0])

# ==========================================================
# Preprocessor
# ==========================================================

preprocessor = build_preprocessor(X_train)

# ==========================================================
# Pipeline
# ==========================================================

pipeline = Pipeline([
    ("preprocessor", preprocessor),
    ("classifier", LogisticRegression())
])

# ==========================================================
# Hyperparameter Search Space
# ==========================================================

param_distributions = {

    "classifier__C": [0.01, 0.1, 1, 10, 100],

    "classifier__solver": [
        "lbfgs",
        "saga"
    ],

    "classifier__penalty": [
        "l2"
    ],

    "classifier__max_iter": [
        1000,
        1500,
        2000
    ]
}

# ==========================================================
# Cross Validation
# ==========================================================

cv = StratifiedKFold(
    n_splits=5,
    shuffle=True,
    random_state=42
)

# ==========================================================
# Random Search
# ==========================================================

print("\nTraining Model...")

search = RandomizedSearchCV(

    estimator=pipeline,

    param_distributions=param_distributions,

    n_iter=10,

    cv=cv,

    scoring="f1_weighted",

    random_state=42,

    n_jobs=-1,

    verbose=1

)

search.fit(X_train, y_train)

best_model = search.best_estimator_

# ==========================================================
# Best Parameters
# ==========================================================

print("\nBest Parameters")

print(search.best_params_)

print("\nBest Cross Validation Score")

print(search.best_score_)

# ==========================================================
# Predictions
# ==========================================================

y_pred = best_model.predict(X_test)

# ==========================================================
# Metrics
# ==========================================================

accuracy = accuracy_score(y_test, y_pred)

precision = precision_score(
    y_test,
    y_pred,
    average="weighted"
)

recall = recall_score(
    y_test,
    y_pred,
    average="weighted"
)

f1 = f1_score(
    y_test,
    y_pred,
    average="weighted"
)

print("\n")
print("=" * 70)
print("MODEL PERFORMANCE")
print("=" * 70)

print(f"Accuracy : {accuracy:.4f}")
print(f"Precision: {precision:.4f}")
print(f"Recall   : {recall:.4f}")
print(f"F1 Score : {f1:.4f}")

print("\nClassification Report\n")

report = classification_report(
    y_test,
    y_pred,
    target_names=label_encoder.classes_
)

print(report)

print("\nConfusion Matrix\n")

cm = confusion_matrix(y_test, y_pred)

print(cm)

# ==========================================================
# Save Pipeline
# ==========================================================

joblib.dump(
    best_model,
    "../outputs/models/student_risk_pipeline.pkl"
)

joblib.dump(
    label_encoder,
    "../outputs/models/label_encoder.pkl"
)

# ==========================================================
# Save Report
# ==========================================================

results = pd.DataFrame({

    "Metric": [
        "Accuracy",
        "Precision",
        "Recall",
        "F1 Score"
    ],

    "Value": [
        accuracy,
        precision,
        recall,
        f1
    ]
})

results.to_csv(
    "../outputs/reports/training_results.csv",
    index=False
)

with open(
    "../outputs/reports/classification_report.txt",
    "w"
) as f:

    f.write(report)

print("\n")
print("=" * 70)
print("MODEL SAVED SUCCESSFULLY")
print("=" * 70)

print("Pipeline : outputs/models/student_risk_pipeline.pkl")

print("Encoder  : outputs/models/label_encoder.pkl")

print("Report   : outputs/reports/training_results.csv")

print("\nTraining Completed Successfully.")