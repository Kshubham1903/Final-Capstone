import pandas as pd
import numpy as np
import joblib

from sklearn.model_selection import train_test_split
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix
)

# =====================================================
# LOAD DATASET
# =====================================================

df = pd.read_csv("StudentPerformanceFactors.csv")

print("=" * 60)
print("Dataset Shape")
print(df.shape)

print("\nMissing Values")
print(df.isnull().sum())

# =====================================================
# CREATE TARGET VARIABLE
# =====================================================

df["Performance_Level"] = pd.qcut(
    df["Exam_Score"],
    q=3,
    labels=["Low", "Medium", "High"]
)

print("\nPerformance Distribution")
print(df["Performance_Level"].value_counts())

# =====================================================
# FEATURES & TARGET
# =====================================================

X = df.drop(["Exam_Score", "Performance_Level"], axis=1)

y = df["Performance_Level"]

# =====================================================
# HANDLE MISSING VALUES
# =====================================================

numeric_cols = X.select_dtypes(include=np.number).columns

categorical_cols = X.select_dtypes(exclude=np.number).columns

num_imputer = SimpleImputer(strategy="median")

cat_imputer = SimpleImputer(strategy="most_frequent")

X[numeric_cols] = num_imputer.fit_transform(X[numeric_cols])

X[categorical_cols] = cat_imputer.fit_transform(X[categorical_cols])

# =====================================================
# LABEL ENCODING
# =====================================================

label_encoders = {}

for col in categorical_cols:
    encoder = LabelEncoder()
    X[col] = encoder.fit_transform(X[col])
    label_encoders[col] = encoder

target_encoder = LabelEncoder()

y = target_encoder.fit_transform(y)

print("\nTarget Classes")
print(target_encoder.classes_)

# =====================================================
# TRAIN TEST SPLIT
# =====================================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)

# =====================================================
# RANDOM FOREST MODEL
# =====================================================

model = RandomForestClassifier(
    n_estimators=300,
    random_state=42,
    class_weight="balanced",
    max_depth=None,
    min_samples_split=2,
    min_samples_leaf=1
)

model.fit(X_train, y_train)

# =====================================================
# PREDICTIONS
# =====================================================

y_pred = model.predict(X_test)

# =====================================================
# EVALUATION
# =====================================================

accuracy = accuracy_score(y_test, y_pred)

print("\n" + "=" * 60)
print("MODEL PERFORMANCE")
print("=" * 60)

print(f"Accuracy : {accuracy*100:.2f}%")

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

# =====================================================
# FEATURE IMPORTANCE
# =====================================================

importance = pd.DataFrame({
    "Feature": X.columns,
    "Importance": model.feature_importances_
})

importance = importance.sort_values(
    by="Importance",
    ascending=False
)

print("\nTop 10 Important Features\n")

print(importance.head(10))

# =====================================================
# SAVE MODEL
# =====================================================

joblib.dump(model, "student_growth_model.pkl")

joblib.dump(label_encoders, "label_encoders.pkl")

joblib.dump(target_encoder, "target_encoder.pkl")

print("\nModel Saved Successfully")

# =====================================================
# SAMPLE PREDICTION
# =====================================================

sample = X.iloc[[10]]

prediction = model.predict(sample)

prediction = target_encoder.inverse_transform(prediction)[0]

print("\nPredicted Performance :", prediction)

# =====================================================
# SIMPLE RECOMMENDATION ENGINE
# =====================================================

print("\nRecommendation")

if prediction == "Low":
    print("- Increase daily study hours.")
    print("- Improve attendance.")
    print("- Maintain 7-8 hours of sleep.")
    print("- Attend tutoring sessions.")

elif prediction == "Medium":
    print("- Focus on weak subjects.")
    print("- Revise consistently.")
    print("- Improve attendance.")

else:
    print("- Excellent performance.")
    print("- Maintain current study habits.")
    print("- Help peers through collaborative learning.")

print("\nProject Completed Successfully.")