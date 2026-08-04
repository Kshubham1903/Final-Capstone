"""
=========================================================
Risk Prediction Engine
Student Growth Intelligence Engine
=========================================================

This module provides the Student Risk Prediction Engine,
which is responsible for:

1. Loading the trained machine learning model.
2. Validating incoming student data.
3. Predicting student academic risk.
4. Computing a mathematically correct risk score.
5. Generating explanations and recommendations.
6. Returning a structured prediction response.

Author : Anushka Kadam
Project: Student Growth Intelligence Engine
=========================================================
"""

from typing import Dict, Any

import joblib
import pandas as pd

from recommendation_engine import RecommendationEngine


# =========================================================
# Configuration
# =========================================================

MODEL_PATH = "../outputs/models/student_risk_pipeline.pkl"

ENCODER_PATH = "../outputs/models/label_encoder.pkl"


# =========================================================
# Student Risk Engine
# =========================================================

class StudentRiskEngine:
    """
    Student Risk Prediction Engine.

    This class loads the trained ML pipeline and predicts
    the academic risk level of a student based on the
    provided academic and demographic features.
    """

    # -----------------------------------------------------

    def __init__(
        self,
        model_path: str = MODEL_PATH,
        encoder_path: str = ENCODER_PATH
    ):
        """
        Initialize the prediction engine.

        Parameters
        ----------
        model_path : str
            Path to trained ML pipeline.

        encoder_path : str
            Path to label encoder.
        """

        self.model = joblib.load(model_path)

        self.encoder = joblib.load(encoder_path)

        self.recommendation_engine = RecommendationEngine()

    # =====================================================
    # Input Validation
    # =====================================================

    def validate_student_data(
        self,
        student_data: Dict[str, Any]
    ) -> None:
        """
        Validate incoming student data.

        Raises
        ------
        ValueError
            If required fields are missing or values
            are outside acceptable ranges.

        TypeError
            If field data types are incorrect.
        """

        required_fields = {

            "Hours_Studied": (int, float),

            "Attendance": (int, float),

            "Parental_Involvement": str,

            "Access_to_Resources": str,

            "Extracurricular_Activities": str,

            "Sleep_Hours": (int, float),

            "Previous_Scores": (int, float),

            "Motivation_Level": str,

            "Internet_Access": str,

            "Tutoring_Sessions": (int, float),

            "Family_Income": str,

            "Teacher_Quality": str,

            "School_Type": str,

            "Peer_Influence": str,

            "Physical_Activity": (int, float),

            "Learning_Disabilities": str,

            "Parental_Education_Level": str,

            "Distance_from_Home": str,

            "Gender": str

        }

        # -------------------------------------------------
        # Check missing fields
        # -------------------------------------------------

        missing_fields = [

            field

            for field in required_fields

            if field not in student_data

        ]

        if missing_fields:

            raise ValueError(

                f"Missing required fields: "

                f"{', '.join(missing_fields)}"

            )

        # -------------------------------------------------
        # Validate data types
        # -------------------------------------------------

        for field, expected_type in required_fields.items():

            if not isinstance(

                student_data[field],

                expected_type

            ):

                raise TypeError(

                    f"{field} should be "

                    f"{expected_type}, "

                    f"got {type(student_data[field]).__name__}"

                )

        # -------------------------------------------------
        # Numeric validation
        # -------------------------------------------------

        if not (0 <= student_data["Attendance"] <= 100):
            raise ValueError(
                "Attendance must be between 0 and 100."
            )

        if not (0 <= student_data["Previous_Scores"] <= 100):
            raise ValueError(
                "Previous_Scores must be between 0 and 100."
            )

        if student_data["Hours_Studied"] < 0:
            raise ValueError(
                "Hours_Studied cannot be negative."
            )

        if student_data["Sleep_Hours"] <= 0:
            raise ValueError(
                "Sleep_Hours must be greater than zero."
            )

        if student_data["Tutoring_Sessions"] < 0:
            raise ValueError(
                "Tutoring_Sessions cannot be negative."
            )

        if student_data["Physical_Activity"] < 0:
            raise ValueError(
                "Physical_Activity cannot be negative."
            )

    # =====================================================
    # Risk Score Calculation
    # =====================================================

    def calculate_risk_score(
        self,
        probabilities
    ) -> float:
        """
        Compute the expected academic risk score.

        Risk Weights
        ------------

        Low Risk      -> 0
        Medium Risk   -> 1
        High Risk     -> 2

        Formula
        -------

        Expected Risk = Σ(P(class) × Weight)

        The expected value is normalized to a
        percentage scale (0–100).

        Returns
        -------
        float
            Risk score between 0 and 100.
        """

        risk_mapping = {

            "Low Risk": 0,

            "Medium Risk": 1,

            "High Risk": 2

        }

        expected_risk = sum(

            risk_mapping[label] * probability

            for label, probability in zip(

                self.encoder.classes_,

                probabilities

            )

        )

        risk_score = (expected_risk / 2) * 100

        return round(risk_score, 2)

    # =====================================================
    # Student Risk Prediction
    # =====================================================

    def predict(
        self,
        student_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Predict the student's academic risk.

        Parameters
        ----------
        student_data : dict

            Dictionary containing student
            academic and demographic information.

        Returns
        -------
        dict

            Structured prediction response.
        """

        # -------------------------------------------------
        # Validate Input
        # -------------------------------------------------

        self.validate_student_data(student_data)

        # -------------------------------------------------
        # Convert dictionary into DataFrame
        # -------------------------------------------------

        student_df = pd.DataFrame([student_data])

        # -------------------------------------------------
        # Predict class
        # -------------------------------------------------

        prediction = self.model.predict(student_df)

        # -------------------------------------------------
        # Predict probabilities
        # -------------------------------------------------

        probabilities = self.model.predict_proba(
            student_df
        )[0]

        # -------------------------------------------------
        # Decode predicted label
        # -------------------------------------------------

        risk_level = self.encoder.inverse_transform(
            prediction
        )[0]

        # -------------------------------------------------
        # Prediction confidence
        # -------------------------------------------------

        confidence = round(

            float(probabilities.max()) * 100,

            2

        )

        # -------------------------------------------------
        # Mathematical Risk Score
        # -------------------------------------------------

        risk_score = self.calculate_risk_score(
            probabilities
        )

        # -------------------------------------------------
        # Generate explanations and recommendations
        # -------------------------------------------------

        recommendation_result = (

            self.recommendation_engine.generate(
                student_data
            )

        )

        # -------------------------------------------------
        # Probability Distribution
        # -------------------------------------------------

        probability_distribution = {

            label: round(probability * 100, 2)

            for label, probability in zip(

                self.encoder.classes_,

                probabilities

            )

        }

        # -------------------------------------------------
        # Final Response
        # -------------------------------------------------

        result = {

            "risk_level": risk_level,

            "confidence": confidence,

            "risk_score": risk_score,

            "risk_probabilities":
                probability_distribution,

            "explanations":
                recommendation_result["explanations"],

            "recommendations":
                recommendation_result["recommendations"]

        }

        return result


        # ==========================================================
# Helper Function
# ==========================================================

def print_result(title: str, result: Dict[str, Any]) -> None:
    """
    Display prediction results in a formatted manner.
    """

    print("\n" + "=" * 70)
    print(title.upper())
    print("=" * 70)

    print(f"\nPredicted Risk Level : {result['risk_level']}")
    print(f"Prediction Confidence: {result['confidence']:.2f}%")
    print(f"Risk Score (0-100)   : {result['risk_score']:.2f}")

    print("\nPrediction Probabilities")
    print("-" * 40)

    for label, probability in result["risk_probabilities"].items():
        print(f"{label:<15}: {probability:.2f}%")

    print("\nReasons")
    print("-" * 40)

    for explanation in result["explanations"]:
        print(f"• {explanation}")

    print("\nRecommendations")
    print("-" * 40)

    for recommendation in result["recommendations"]:
        print(f"• {recommendation}")

    print("=" * 70)


# ==========================================================
# Test Students
# ==========================================================

HIGH_RISK_STUDENT = {

    "Hours_Studied": 2,
    "Attendance": 65,
    "Parental_Involvement": "Low",
    "Access_to_Resources": "Low",
    "Extracurricular_Activities": "No",
    "Sleep_Hours": 5,
    "Previous_Scores": 52,
    "Motivation_Level": "Low",
    "Internet_Access": "Yes",
    "Tutoring_Sessions": 1,
    "Family_Income": "Low",
    "Teacher_Quality": "Medium",
    "School_Type": "Public",
    "Peer_Influence": "Negative",
    "Physical_Activity": 2,
    "Learning_Disabilities": "No",
    "Parental_Education_Level": "High School",
    "Distance_from_Home": "Far",
    "Gender": "Male"

}


MEDIUM_RISK_STUDENT = {

    "Hours_Studied": 6,
    "Attendance": 90,
    "Parental_Involvement": "Medium",
    "Access_to_Resources": "Medium",
    "Extracurricular_Activities": "Yes",
    "Sleep_Hours": 7,
    "Previous_Scores": 80,
    "Motivation_Level": "Medium",
    "Internet_Access": "Yes",
    "Tutoring_Sessions": 3,
    "Family_Income": "Medium",
    "Teacher_Quality": "Medium",
    "School_Type": "Public",
    "Peer_Influence": "Neutral",
    "Physical_Activity": 5,
    "Learning_Disabilities": "No",
    "Parental_Education_Level": "College",
    "Distance_from_Home": "Moderate",
    "Gender": "Female"

}


LOW_RISK_STUDENT = {

    "Hours_Studied": 8,
    "Attendance": 97,
    "Parental_Involvement": "High",
    "Access_to_Resources": "High",
    "Extracurricular_Activities": "Yes",
    "Sleep_Hours": 8,
    "Previous_Scores": 95,
    "Motivation_Level": "High",
    "Internet_Access": "Yes",
    "Tutoring_Sessions": 5,
    "Family_Income": "High",
    "Teacher_Quality": "High",
    "School_Type": "Private",
    "Peer_Influence": "Positive",
    "Physical_Activity": 6,
    "Learning_Disabilities": "No",
    "Parental_Education_Level": "Postgraduate",
    "Distance_from_Home": "Near",
    "Gender": "Female"

}



# ==========================================================
# Main Program
# ==========================================================

if __name__ == "__main__":

    print("\n" + "=" * 70)
    print("STUDENT GROWTH INTELLIGENCE ENGINE")
    print("Academic Risk Prediction Module")
    print("=" * 70)

    try:

        # -------------------------------------------------
        # Initialize Prediction Engine
        # -------------------------------------------------

        engine = StudentRiskEngine()

        print("\n✓ Student Risk Engine loaded successfully.")

        # -------------------------------------------------
        # Test Cases
        # -------------------------------------------------

        test_cases = [

            ("High Risk Student", HIGH_RISK_STUDENT),

            ("Medium Risk Student", MEDIUM_RISK_STUDENT),

            ("Low Risk Student", LOW_RISK_STUDENT)

        ]

        # -------------------------------------------------
        # Run Predictions
        # -------------------------------------------------

        for title, student in test_cases:

            result = engine.predict(student)

            print_result(title, result)

        print("\n" + "=" * 70)
        print("All test cases executed successfully.")
        print("=" * 70)

    # -----------------------------------------------------
    # Input Validation Errors
    # -----------------------------------------------------

    except ValueError as error:

        print("\nValidation Error")
        print("-" * 70)
        print(error)

    # -----------------------------------------------------
    # Incorrect Data Types
    # -----------------------------------------------------

    except TypeError as error:

        print("\nType Error")
        print("-" * 70)
        print(error)

    # -----------------------------------------------------
    # Model Loading Errors
    # -----------------------------------------------------

    except FileNotFoundError as error:

        print("\nModel File Error")
        print("-" * 70)
        print(error)

    # -----------------------------------------------------
    # Unexpected Errors
    # -----------------------------------------------------

    except Exception as error:

        print("\nUnexpected Error")
        print("-" * 70)
        print(error)


        
