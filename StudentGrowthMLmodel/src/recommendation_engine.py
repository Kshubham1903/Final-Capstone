"""
=========================================================
Recommendation Engine
Student Growth Intelligence Engine
=========================================================

Responsibilities
----------------
Generate personalized learning recommendations based on
student information.

Author: Anushka Kadam
=========================================================
"""


class RecommendationEngine:

    def generate(self, student):

        recommendations = []
        explanations = []

        # -------------------------------------------------
        # Attendance
        # -------------------------------------------------

        if student["Attendance"] < 75:
            explanations.append(
                "Attendance is below the recommended level (75%)."
            )

            recommendations.append(
                "Increase attendance above 85%."
            )

        # -------------------------------------------------
        # Study Hours
        # -------------------------------------------------

        if student["Hours_Studied"] < 3:
            explanations.append(
                "Daily study hours are too low."
            )

            recommendations.append(
                "Study at least 2 additional hours every day."
            )

        # -------------------------------------------------
        # Previous Scores
        # -------------------------------------------------

        if student["Previous_Scores"] < 60:
            explanations.append(
                "Previous academic performance is below average."
            )

            recommendations.append(
                "Revise previously learned concepts before moving ahead."
            )

        # -------------------------------------------------
        # Tutoring
        # -------------------------------------------------

        if student["Tutoring_Sessions"] < 2:
            explanations.append(
                "Very few tutoring sessions attended."
            )

            recommendations.append(
                "Attend additional tutoring sessions every week."
            )

        # -------------------------------------------------
        # Sleep
        # -------------------------------------------------

        if student["Sleep_Hours"] < 6:
            explanations.append(
                "Insufficient sleep may affect concentration."
            )

            recommendations.append(
                "Maintain at least 7 hours of sleep daily."
            )

        # -------------------------------------------------
        # Physical Activity
        # -------------------------------------------------

        if student["Physical_Activity"] < 3:
            explanations.append(
                "Physical activity level is below recommended."
            )

            recommendations.append(
                "Increase physical activity to improve focus."
            )

        # -------------------------------------------------
        # Motivation
        # -------------------------------------------------

        if student["Motivation_Level"] == "Low":
            explanations.append(
                "Student reports low motivation."
            )

            recommendations.append(
                "Set small daily learning goals and track progress."
            )

        # -------------------------------------------------
        # Internet
        # -------------------------------------------------

        if student["Internet_Access"] == "No":
            explanations.append(
                "No internet access available."
            )

            recommendations.append(
                "Provide offline learning material whenever possible."
            )

        # -------------------------------------------------
        # Resources
        # -------------------------------------------------

        if student["Access_to_Resources"] == "Low":
            explanations.append(
                "Learning resources are limited."
            )

            recommendations.append(
                "Provide additional study materials and reference books."
            )

        # -------------------------------------------------
        # Excellent Student
        # -------------------------------------------------

        if len(recommendations) == 0:

            explanations.append(
                "No significant academic risk factors detected."
            )

            recommendations.append(
                "Maintain the current study routine and continue consistent learning."
            )

        return {

            "explanations": explanations,

            "recommendations": recommendations

        }


# ==========================================================
# Testing
# ==========================================================

if __name__ == "__main__":

    engine = RecommendationEngine()

    student = {

        "Attendance": 65,
        "Hours_Studied": 2,
        "Previous_Scores": 55,
        "Tutoring_Sessions": 1,
        "Sleep_Hours": 5,
        "Physical_Activity": 2,
        "Motivation_Level": "Low",
        "Internet_Access": "Yes",
        "Access_to_Resources": "Low"

    }

    result = engine.generate(student)

    print("=" * 60)
    print("RECOMMENDATION ENGINE")
    print("=" * 60)

    print("\nReasons")

    for item in result["explanations"]:
        print(f"• {item}")

    print("\nRecommendations")

    for item in result["recommendations"]:
        print(f"• {item}")