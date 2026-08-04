"""
=========================================================
Model Comparison
Student Growth Intelligence Engine
=========================================================
"""

import os
import warnings
import joblib
import pandas as pd

from sklearn.pipeline import Pipeline
from sklearn.model_selection import (
    train_test_split,
    cross_val_score,
    StratifiedKFold
)

from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix
)

from sklearn.preprocessing import LabelEncoder

from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import (
    RandomForestClassifier,
    GradientBoostingClassifier
)

from preprocess import (
    load_dataset,
    create_target,
    split_features_target,
    build_preprocessor
)

warnings.filterwarnings("ignore")

# ==========================================================
# Create Output Folders
# ==========================================================

os.makedirs("../outputs/models", exist_ok=True)
os.makedirs("../outputs/reports", exist_ok=True)

# ==========================================================
# Load Dataset
# ==========================================================

print("=" * 70)
print("MODEL COMPARISON")
print("=" * 70)

df = load_dataset()

df = create_target(df)

X, y = split_features_target(df)

# Encode target labels only
target_encoder = LabelEncoder()
y = target_encoder.fit_transform(y)

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

# ==========================================================
# Preprocessor
# ==========================================================

preprocessor = build_preprocessor(X_train)

# ==========================================================
# Models
# ==========================================================

models = {
    "Logistic Regression":
        LogisticRegression(max_iter=1000),

    "Decision Tree":
        DecisionTreeClassifier(random_state=42),

    "Random Forest":
        RandomForestClassifier(
            random_state=42,
            n_estimators=300,
            class_weight="balanced"
        ),

    "Gradient Boosting":
        GradientBoostingClassifier(random_state=42)
}

results = []

best_pipeline = None
best_model_name = None
best_accuracy = 0

# ==========================================================
# Train & Evaluate
# ==========================================================

cv = StratifiedKFold(
    n_splits=5,
    shuffle=True,
    random_state=42
)

for name, model in models.items():

    print("\n" + "=" * 70)
    print(name)
    print("=" * 70)

    pipeline = Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", model)
    ])

    # -----------------------
    # Cross Validation
    # -----------------------

    cv_scores = cross_val_score(
        pipeline,
        X_train,
        y_train,
        cv=cv,
        scoring="accuracy",
        n_jobs=-1
    )

    # -----------------------
    # Train
    # -----------------------

    pipeline.fit(X_train, y_train)

    # -----------------------
    # Predict
    # -----------------------

    y_pred = pipeline.predict(X_test)

    # -----------------------
    # Metrics
    # -----------------------

    accuracy = accuracy_score(y_test, y_pred)

    precision = precision_score(
        y_test,
        y_pred,
        average="weighted",
        zero_division=0
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

    print(f"Accuracy : {accuracy:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall   : {recall:.4f}")
    print(f"F1 Score : {f1:.4f}")

    print("\nCross Validation Scores")

    print(cv_scores)

    print(f"Mean CV Accuracy : {cv_scores.mean():.4f}")
    print(f"Std Dev          : {cv_scores.std():.4f}")

    print("\nClassification Report\n")

    print(
        classification_report(
            y_test,
            y_pred,
            target_names=target_encoder.classes_,
            zero_division=0
        )
    )

    print("\nConfusion Matrix\n")

    print(confusion_matrix(y_test, y_pred))

    results.append({
        "Model": name,
        "Accuracy": accuracy,
        "Precision": precision,
        "Recall": recall,
        "F1 Score": f1,
        "CV Mean": cv_scores.mean(),
        "CV Std": cv_scores.std()
    })

    if accuracy > best_accuracy:
        best_accuracy = accuracy
        best_model_name = name
        best_pipeline = pipeline

# ==========================================================
# Results
# ==========================================================

results_df = pd.DataFrame(results)

results_df = results_df.sort_values(
    by="Accuracy",
    ascending=False
)

print("\n")
print("=" * 70)
print("MODEL COMPARISON SUMMARY")
print("=" * 70)

print(results_df)

results_df.to_csv(
    "../outputs/reports/model_comparison.csv",
    index=False
)

# ==========================================================
# Save Best Model
# ==========================================================

joblib.dump(
    best_pipeline,
    "../outputs/models/best_pipeline.pkl"
)

print("\nBest Model :", best_model_name)
print(f"Accuracy   : {best_accuracy:.4f}")

print("\nBest pipeline saved successfully.")

print("\nComparison report saved.")

print("\nPROJECT STEP COMPLETED.")