"""
=========================================================
Student Growth Intelligence Engine
FastAPI Prediction Service
=========================================================

Author : Anushka Kadam
=========================================================
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from risk_engine import StudentRiskEngine


# =====================================================
# FastAPI Application
# =====================================================

app = FastAPI(
    title="Student Risk Prediction API",
    description="ML microservice for predicting student academic risk.",
    version="1.0.0"
)


# =====================================================
# Load ML Model (Load Once)
# =====================================================

try:
    engine = StudentRiskEngine()
    print("✓ Student Risk Engine loaded successfully.")
except Exception as e:
    print(f"Error loading model: {e}")
    engine = None


# =====================================================
# Request Schema
# =====================================================

class StudentData(BaseModel):

    Hours_Studied: float
    Attendance: float
    Parental_Involvement: str
    Access_to_Resources: str
    Extracurricular_Activities: str
    Sleep_Hours: float
    Previous_Scores: float
    Motivation_Level: str
    Internet_Access: str
    Tutoring_Sessions: float
    Family_Income: str
    Teacher_Quality: str
    School_Type: str
    Peer_Influence: str
    Physical_Activity: float
    Learning_Disabilities: str
    Parental_Education_Level: str
    Distance_from_Home: str
    Gender: str


# =====================================================
# Root Endpoint
# =====================================================

@app.get("/")
def home():

    return {
        "service": "Student Risk Prediction API",
        "status": "running",
        "version": "1.0.0"
    }


# =====================================================
# Health Check
# =====================================================

@app.get("/health")
def health():

    if engine is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded."
        )

    return {
        "status": "healthy"
    }


# =====================================================
# Prediction Endpoint
# =====================================================

@app.post("/predict-risk")
def predict(student: StudentData):

    if engine is None:

        raise HTTPException(
            status_code=503,
            detail="Prediction engine unavailable."
        )

    try:

        result = engine.predict(student.model_dump())

        return result

    except Exception as e:

        raise HTTPException(
            status_code=400,
            detail=str(e)
        )


# =====================================================
# Run Application
# =====================================================

if __name__ == "__main__":

    import uvicorn

    uvicorn.run(
        "api:app",
        host="127.0.0.1",
        port=8000,
        reload=True
    )
    