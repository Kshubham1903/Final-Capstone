"""
=========================================================
Exploratory Data Analysis (EDA)
Student Growth Intelligence Engine
=========================================================
"""

import os
import warnings

import pandas as pd
import numpy as np

import matplotlib.pyplot as plt
import seaborn as sns

warnings.filterwarnings("ignore")

# =========================================================
# Create Output Folder
# =========================================================

os.makedirs("outputs/plots", exist_ok=True)

# =========================================================
# Load Dataset
# =========================================================

df = pd.read_csv("StudentPerformanceFactors.csv")

print("=" * 70)
print("DATASET INFORMATION")
print("=" * 70)

print("\nShape of Dataset")
print(df.shape)

print("\nColumn Names")
print(df.columns.tolist())

print("\nData Types")
print(df.dtypes)

# =========================================================
# Basic Information
# =========================================================

print("\nDataset Info")
print(df.info())

print("\nSummary Statistics")
print(df.describe(include="all"))

# =========================================================
# Missing Values
# =========================================================

print("\nMissing Values")

missing = df.isnull().sum()

print(missing)

missing = missing[missing > 0]

if len(missing) > 0:

    plt.figure(figsize=(8,5))

    missing.sort_values().plot(kind="barh")

    plt.title("Missing Values")

    plt.tight_layout()

    plt.savefig("outputs/plots/missing_values.png")

    plt.close()

# =========================================================
# Duplicate Rows
# =========================================================

duplicates = df.duplicated().sum()

print("\nDuplicate Rows :", duplicates)

# =========================================================
# Numerical Features
# =========================================================

numerical_columns = df.select_dtypes(include=np.number).columns

print("\nNumerical Features")

print(numerical_columns)

# =========================================================
# Categorical Features
# =========================================================

categorical_columns = df.select_dtypes(exclude=np.number).columns

print("\nCategorical Features")

print(categorical_columns)

# =========================================================
# Distribution of Exam Score
# =========================================================

plt.figure(figsize=(8,5))

sns.histplot(df["Exam_Score"], bins=30, kde=True)

plt.title("Distribution of Exam Score")

plt.tight_layout()

plt.savefig("outputs/plots/exam_score_distribution.png")

plt.close()

# =========================================================
# Boxplot of Exam Score
# =========================================================

plt.figure(figsize=(7,4))

sns.boxplot(x=df["Exam_Score"])

plt.title("Exam Score Boxplot")

plt.tight_layout()

plt.savefig("outputs/plots/exam_score_boxplot.png")

plt.close()

# =========================================================
# Correlation Heatmap
# =========================================================

plt.figure(figsize=(10,8))

corr = df[numerical_columns].corr()

sns.heatmap(
    corr,
    annot=True,
    cmap="coolwarm",
    fmt=".2f"
)

plt.title("Correlation Heatmap")

plt.tight_layout()

plt.savefig("outputs/plots/correlation_heatmap.png")

plt.close()

# =========================================================
# Histograms of Numerical Features
# =========================================================

for column in numerical_columns:

    plt.figure(figsize=(7,4))

    sns.histplot(df[column], kde=True)

    plt.title(column)

    plt.tight_layout()

    plt.savefig(f"outputs/plots/{column}_histogram.png")

    plt.close()

# =========================================================
# Boxplots of Numerical Features
# =========================================================

for column in numerical_columns:

    plt.figure(figsize=(7,4))

    sns.boxplot(x=df[column])

    plt.title(column)

    plt.tight_layout()

    plt.savefig(f"outputs/plots/{column}_boxplot.png")

    plt.close()

# =========================================================
# Categorical Count Plots
# =========================================================

for column in categorical_columns:

    plt.figure(figsize=(8,4))

    sns.countplot(
        x=df[column],
        order=df[column].value_counts().index
    )

    plt.title(column)

    plt.xticks(rotation=30)

    plt.tight_layout()

    plt.savefig(f"outputs/plots/{column}_countplot.png")

    plt.close()

# =========================================================
# Average Exam Score by Category
# =========================================================

for column in categorical_columns:

    plt.figure(figsize=(8,4))

    avg_score = (
        df.groupby(column)["Exam_Score"]
        .mean()
        .sort_values(ascending=False)
    )

    sns.barplot(
        x=avg_score.index,
        y=avg_score.values
    )

    plt.title(f"Average Exam Score by {column}")

    plt.xticks(rotation=30)

    plt.tight_layout()

    plt.savefig(f"outputs/plots/{column}_average_score.png")

    plt.close()

# =========================================================
# Correlation with Exam Score
# =========================================================

print("\nCorrelation with Exam Score")

correlation = corr["Exam_Score"].sort_values(ascending=False)

print(correlation)

# =========================================================
# Top Students
# =========================================================

print("\nTop 10 Students")

print(
    df.sort_values(
        by="Exam_Score",
        ascending=False
    ).head(10)
)

# =========================================================
# Lowest Students
# =========================================================

print("\nLowest 10 Students")

print(
    df.sort_values(
        by="Exam_Score"
    ).head(10)
)

# =========================================================
# Outlier Detection using IQR
# =========================================================

print("\nOutlier Detection")

for column in numerical_columns:

    Q1 = df[column].quantile(0.25)

    Q3 = df[column].quantile(0.75)

    IQR = Q3 - Q1

    lower = Q1 - 1.5 * IQR

    upper = Q3 + 1.5 * IQR

    outliers = df[
        (df[column] < lower) |
        (df[column] > upper)
    ]

    print(f"{column:25s}: {len(outliers)} outliers")

print("\nEDA COMPLETED SUCCESSFULLY")

print("\nPlots saved inside:")

print("outputs/plots/")