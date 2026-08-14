import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler
import random
import os
import joblib

app = FastAPI(
    title="EduPilot AI - Core Analytics Service",
    description="Microservice providing machine learning predictions, SGI profiling, and adaptive recommendations.",
    version="1.1.0"
)

# Load machine learning models (with fallback safety)
MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "StudentGrowthMLmodel")
model_path = os.path.join(MODEL_DIR, "student_growth_model.pkl")
encoders_path = os.path.join(MODEL_DIR, "label_encoders.pkl")
target_encoder_path = os.path.join(MODEL_DIR, "target_encoder.pkl")

rf_model = None
label_encoders = {}
target_encoder = None

try:
    if os.path.exists(model_path):
        rf_model = joblib.load(model_path)
    if os.path.exists(encoders_path):
        label_encoders = joblib.load(encoders_path)
    if os.path.exists(target_encoder_path):
        target_encoder = joblib.load(target_encoder_path)
    print("AI Core Engine: Machine learning models loaded successfully.")
except Exception as e:
    print(f"AI Core Engine Warning: Failed to load ML models. Fallback logic active. Error: {e}")

# Existing models
class LifestyleMetrics(BaseModel):
    sleep_hours: float
    screen_time_hours: float
    stress_level: int # 1 to 10
    exercise_minutes: float
    study_minutes: float
    attendance_rate: float # 0 to 100
    productivity_rating: float # 1 to 10

class StudentProfileRequest(BaseModel):
    student_id: str
    target_cgpa: float
    current_cgpa: float
    concept_mastery: Dict[str, float]
    lifestyle: LifestyleMetrics
    completed_quizzes_count: int

class PredictionResponse(BaseModel):
    predicted_cgpa: float
    academic_risk_level: str # LOW, MEDIUM, HIGH
    student_growth_index: float # SGI (0.0 to 10.0)
    improvement_rate: float
    insights: List[str]

class QuizPerformanceRequest(BaseModel):
    concept: str
    current_difficulty: str # EASY, MEDIUM, HARD
    is_correct: bool
    response_time_seconds: float

class QuizAdaptiveResponse(BaseModel):
    next_difficulty: str
    reason: str

# NEW: Questionnaire and Student Development response schemas
class LifestyleQuestionnaireRequest(BaseModel):
    student_id: str
    hours_studied: float
    attendance: float
    parental_involvement: str # Low, Medium, High
    access_to_resources: str # Low, Medium, High
    extracurricular_activities: str # Yes, No
    sleep_hours: float
    previous_scores: float # 0-100
    motivation_level: str # Low, Medium, High
    internet_access: str # Yes, No
    tutoring_sessions: float
    family_income: str # Low, Medium, High
    teacher_quality: str # Low, Medium, High
    school_type: str # Public, Private
    peer_influence: str # Positive, Negative, Neutral
    physical_activity: float # hours per week
    learning_disabilities: str # Yes, No
    parental_education_level: str # High School, College, Postgraduate
    distance_from_home: str # Near, Moderate, Far
    gender: str # Male, Female

class StudentDevelopmentResponse(BaseModel):
    predicted_performance_level: str # Low, Medium, High
    predicted_cgpa: float
    academic_risk_level: str # LOW, MEDIUM, HIGH
    student_growth_index: float # 0.0 - 10.0
    consistency_score: int
    productivity_score: int
    lifestyle_score: int
    insights: List[str]

# Encoders safe helper
def safe_encode(col_name: str, value: str) -> int:
    encoder = label_encoders.get(col_name)
    if not encoder:
        return 0
    val_clean = str(value).strip()
    for cls in encoder.classes_:
        if str(cls).lower() == val_clean.lower():
            return int(encoder.transform([cls])[0])
    return int(encoder.transform([encoder.classes_[0]])[0])

# Insight generation helper
def generate_personalized_insights(req: LifestyleQuestionnaireRequest, pred_level: str) -> List[str]:
    recs = []
    
    # 1. Performance-based core insight
    if pred_level == "Low":
        recs.append("Academic Risk: Student shows high vulnerability. Immediate study hour escalation and tutoring are highly recommended.")
    elif pred_level == "Medium":
        recs.append("Academic Standing: Stable. Focused revision on concept gaps will secure standard placement eligibility.")
    else:
        recs.append("Academic Success: Excellent standing. Suggest targeting advanced problem-solving modules and leadership paths.")

    # 2. Sleep hours recommendations
    if req.sleep_hours < 6.5:
        recs.append("Sleep Guide: Sleep duration is below optimal thresholds. Aim for 7.5 hours tonight to improve next-day memory recall.")
    elif req.sleep_hours > 9.0:
        recs.append("Sleep Guide: High sleep durations could point to fatigue. Aim for a structured 8 hours schedule.")

    # 3. Study Hours recommendations
    if req.hours_studied < 15:
        recs.append("Study Plan: Weekly study hours are low. Schedule structured 2-hour daily study sessions to master advanced subjects.")
    
    # 4. Physical Activity recommendations
    if req.physical_activity < 2.0:
        recs.append("Wellness Break: Physical activity is under baseline limits. Integrate a 25-minute brisk walk to lower cortisol/stress levels.")

    # 5. Peer Influence
    if req.peer_influence.lower() == "negative":
        recs.append("Social Circle: Negative peer influence detected. Participate in university collaborative study hackathons for healthy peer matching.")

    # 6. Tutoring Sessions
    if req.tutoring_sessions == 0 and pred_level in ["Low", "Medium"]:
        recs.append("Guidance Plan: Consider booking a faculty tutoring sync for complex topics in Algorithms and Database Systems.")

    # 7. General Productivity & Revision suggestion
    recs.append("Daily Routine: Activate Pomodoro cycles (25m study + 5m break) during focus blocks to bypass digital screen fatigue.")

    return recs

# Fallback development calculation
def calculate_fallback_development(req: LifestyleQuestionnaireRequest) -> StudentDevelopmentResponse:
    # Diagnostic rules-based fallback calculation
    attendance_factor = req.attendance / 100.0
    study_factor = min(req.hours_studied / 40.0, 1.0)
    sleep_factor = max(0, 1.0 - abs(req.sleep_hours - 8.0) * 0.15)
    
    composite_score = (req.previous_scores * 0.40) + (attendance_factor * 25) + (study_factor * 20) + (sleep_factor * 15)
    
    if composite_score >= 75:
        pred_level = "High"
        risk = "LOW"
        predicted_cgpa = round(8.5 + (composite_score - 75) * 0.06, 2)
        consistency = 90
        productivity = 85
        lifestyle = 88
    elif composite_score >= 50:
        pred_level = "Medium"
        risk = "MEDIUM"
        predicted_cgpa = round(7.0 + (composite_score - 50) * 0.06, 2)
        consistency = 75
        productivity = 70
        lifestyle = 68
    else:
        pred_level = "Low"
        risk = "HIGH"
        predicted_cgpa = round(5.0 + composite_score * 0.04, 2)
        consistency = 50
        productivity = 45
        lifestyle = 42

    predicted_cgpa = min(10.0, max(0.0, predicted_cgpa))
    sgi = round(composite_score / 10.0, 2)
    insights = generate_personalized_insights(req, pred_level)
    
    return StudentDevelopmentResponse(
        predicted_performance_level=pred_level,
        predicted_cgpa=predicted_cgpa,
        academic_risk_level=risk,
        student_growth_index=sgi,
        consistency_score=consistency,
        productivity_score=productivity,
        lifestyle_score=lifestyle,
        insights=insights
    )

@app.get("/")
def read_root():
    return {"status": "online", "service": "EduPilot AI Core Engine"}

@app.post("/api/ai/predict-performance", response_model=PredictionResponse)
def predict_performance(profile: StudentProfileRequest):
    academic_score = (profile.current_cgpa / 10.0) * 100.0 if profile.current_cgpa <= 10.0 else profile.current_cgpa
    mastery_values = list(profile.concept_mastery.values())
    avg_mastery = sum(mastery_values) / len(mastery_values) if mastery_values else 50.0
    
    sleep_points = 100 - min(abs(profile.lifestyle.sleep_hours - 8.0) * 20, 100)
    stress_points = (10 - profile.lifestyle.stress_level) * 10
    exercise_points = min((profile.lifestyle.exercise_minutes / 30.0) * 100, 100)
    screen_points = max(100 - (profile.lifestyle.screen_time_hours * 15), 0)
    study_points = min((profile.lifestyle.study_minutes / 240.0) * 100, 100)
    
    lifestyle_calc = (sleep_points + stress_points + exercise_points + screen_points + study_points) / 5.0
    consistency_points = min((profile.completed_quizzes_count / 10.0) * 100, 100)
    
    sgi = ((academic_score * 0.40) + (avg_mastery * 0.30) + (lifestyle_calc * 0.20) + (consistency_points * 0.10)) / 10.0
    sgi = round(max(min(sgi, 10.0), 0.0), 2)
    
    predicted_cgpa_delta = (avg_mastery / 150.0) + (profile.lifestyle.study_minutes / 600.0) - (profile.lifestyle.stress_level / 40.0)
    predicted_cgpa = round(min(max(profile.current_cgpa + predicted_cgpa_delta - 0.2, 0.0), 10.0), 2)
    
    if sgi >= 7.5 and profile.lifestyle.attendance_rate >= 80:
        risk = "LOW"
    elif sgi >= 5.0 and profile.lifestyle.attendance_rate >= 70:
        risk = "MEDIUM"
    else:
        risk = "HIGH"
        
    insights = []
    if profile.lifestyle.sleep_hours < 6.5:
        insights.append("Sleep deficit detected. Increasing sleep toward 7.5h can boost daytime productivity by up to 20%.")
    if profile.lifestyle.stress_level > 7:
        insights.append("High stress levels are impacting information retention. Integrate mindfulness or scheduled breaks.")
    if avg_mastery < 65:
        insights.append("Focus on revision sets. Weak mastery scores in fundamental chapters may bottleneck advanced semesters.")
    if profile.lifestyle.screen_time_hours > 6.0:
        insights.append("High screen time detected. Replacing 1 hour of recreational screen time with active study yields better focus.")
    if profile.lifestyle.attendance_rate < 75:
        insights.append("Attendance is below recommendation limit. Try to attend upcoming live faculty reviews to capture key test concepts.")
        
    if not insights:
        insights.append("Excellent balance! Student maintains a healthy growth velocity. Suggest moving to advanced quizzes.")
        
    return PredictionResponse(
        predicted_cgpa=predicted_cgpa,
        academic_risk_level=risk,
        student_growth_index=sgi,
        improvement_rate=round(predicted_cgpa_delta * 10.0, 2),
        insights=insights
    )

@app.post("/api/ai/predict-student-development", response_model=StudentDevelopmentResponse)
def predict_student_development(req: LifestyleQuestionnaireRequest):
    if rf_model is None or not label_encoders or target_encoder is None:
        return calculate_fallback_development(req)
        
    try:
        features = {
            "Hours_Studied": req.hours_studied,
            "Attendance": req.attendance,
            "Parental_Involvement": safe_encode("Parental_Involvement", req.parental_involvement),
            "Access_to_Resources": safe_encode("Access_to_Resources", req.access_to_resources),
            "Extracurricular_Activities": safe_encode("Extracurricular_Activities", req.extracurricular_activities),
            "Sleep_Hours": req.sleep_hours,
            "Previous_Scores": req.previous_scores,
            "Motivation_Level": safe_encode("Motivation_Level", req.motivation_level),
            "Internet_Access": safe_encode("Internet_Access", req.internet_access),
            "Tutoring_Sessions": req.tutoring_sessions,
            "Family_Income": safe_encode("Family_Income", req.family_income),
            "Teacher_Quality": safe_encode("Teacher_Quality", req.teacher_quality),
            "School_Type": safe_encode("School_Type", req.school_type),
            "Peer_Influence": safe_encode("Peer_Influence", req.peer_influence),
            "Physical_Activity": req.physical_activity,
            "Learning_Disabilities": safe_encode("Learning_Disabilities", req.learning_disabilities),
            "Parental_Education_Level": safe_encode("Parental_Education_Level", req.parental_education_level),
            "Distance_from_Home": safe_encode("Distance_from_Home", req.distance_from_home),
            "Gender": safe_encode("Gender", req.gender)
        }
        
        df_input = pd.DataFrame([features])
        pred_encoded = rf_model.predict(df_input)
        pred_level = target_encoder.inverse_transform(pred_encoded)[0]
        
        academic_score = req.previous_scores / 10.0
        sleep_points = max(0, 10 - abs(req.sleep_hours - 8.0) * 2.0)
        study_points = min(10.0, (req.hours_studied / 30.0) * 10.0)
        attendance_points = min(10.0, (req.attendance / 100.0) * 10.0)
        
        sgi = (academic_score * 0.40) + (sleep_points * 0.20) + (study_points * 0.20) + (attendance_points * 0.20)
        sgi = round(max(min(sgi, 10.0), 0.0), 2)
        
        if pred_level == "Low":
            risk = "HIGH"
            predicted_cgpa = round(5.5 + (req.previous_scores / 100.0) * 1.5, 2)
            consistency = 50
            productivity = 45
            lifestyle = 40
        elif pred_level == "Medium":
            risk = "MEDIUM"
            predicted_cgpa = round(7.0 + (req.previous_scores / 100.0) * 1.5, 2)
            consistency = 75
            productivity = 70
            lifestyle = 68
        else:
            risk = "LOW"
            predicted_cgpa = round(8.3 + (req.previous_scores / 100.0) * 1.5, 2)
            consistency = 90
            productivity = 85
            lifestyle = 88
            
        predicted_cgpa = min(10.0, max(0.0, predicted_cgpa))
        insights = generate_personalized_insights(req, pred_level)
        
        return StudentDevelopmentResponse(
            predicted_performance_level=pred_level,
            predicted_cgpa=predicted_cgpa,
            academic_risk_level=risk,
            student_growth_index=sgi,
            consistency_score=consistency,
            productivity_score=productivity,
            lifestyle_score=lifestyle,
            insights=insights
        )
    except Exception as e:
        print(f"Error in prediction: {e}")
        return calculate_fallback_development(req)

class QuestionGenRequest(BaseModel):
    subject: str
    difficulty: str
    exclude_questions: Optional[List[str]] = []
    count: Optional[int] = 8

class QuestionItem(BaseModel):
    subject: str
    concept: str
    conceptId: Optional[str] = None
    difficulty: str
    questionText: str
    options: List[str]
    correctOptionIndex: int
    conceptualExplanation: str
    questionType: Optional[str] = "Conceptual Understanding"
    templateFamilyId: Optional[str] = None
    questionFingerprint: Optional[str] = None

@app.post("/api/ai/adaptive-quiz", response_model=QuizAdaptiveResponse)
def adaptive_quiz(req: QuizPerformanceRequest):
    levels = ["EASY", "MEDIUM", "HARD"]
    current_idx = levels.index(req.current_difficulty) if req.current_difficulty in levels else 0
    
    if req.is_correct:
        if req.response_time_seconds < 15 and current_idx < 2:
            next_diff = levels[current_idx + 1]
            reason = "Correct answer submitted in record time. Scaling difficulty up to match competency."
        elif current_idx < 2:
            next_diff = levels[current_idx + 1]
            reason = "Concept mastery confirmed. Advancing to next learning tier."
        else:
            next_diff = "HARD"
            reason = "Maximum mastery depth achieved. Continuous validation on hard tier active."
    else:
        if req.response_time_seconds > 45 and current_idx > 0:
            next_diff = levels[current_idx - 1]
            reason = "Prolonged attempt with conceptual bottleneck. Adjusting down to reinforce foundations."
        elif current_idx > 0:
            next_diff = levels[current_idx - 1]
            reason = "Incorrect submission. Revisiting intermediate concepts to strengthen mastery."
        else:
            next_diff = "EASY"
            reason = "Reinforcing core fundamentals. Focus on reviewing provided conceptual hints."
            
    return QuizAdaptiveResponse(
        next_difficulty=next_diff,
        reason=reason
    )

def normalize_text(text: str) -> str:
    if not text:
        return ""
    clean = "".join([c.lower() if c.isalnum() else " " for c in text])
    return " ".join(clean.split())

STOP_WORDS = {"regarding", "fundamental", "principles", "what", "is", "the", "a", "an", "in", "of", "to", "and", "or", "for", "with", "by", "how", "which", "does", "do", "when", "why", "where", "are"}

def generate_question_fingerprint(q_text: str, subject: str = "") -> str:
    if not q_text:
        return ""
    norm = normalize_text(q_text)
    if subject:
        subj_norm = normalize_text(subject)
        norm = norm.replace(subj_norm, "")
    words = [w for w in norm.split() if len(w) > 2 and w not in STOP_WORDS]
    if not words:
        words = [w for w in norm.split() if len(w) > 2]
    return "fp_" + "_".join(words[:6])

def is_duplicate(q_text: str, exclude_list: List[str], subject: str = "") -> bool:
    norm_q = normalize_text(q_text)
    fp_q = generate_question_fingerprint(q_text, subject)
    words_q = set(w for w in norm_q.split() if w not in STOP_WORDS)
    if not norm_q or not words_q:
        return False
    for exc in exclude_list:
        norm_e = normalize_text(exc)
        fp_e = generate_question_fingerprint(exc, subject)
        if norm_q == norm_e or (fp_q and fp_q == fp_e):
            return True
        words_e = set(w for w in norm_e.split() if w not in STOP_WORDS)
        if words_e:
            intersection = words_q.intersection(words_e)
            union = words_q.union(words_e)
            if len(intersection) / len(union) > 0.75:
                return True
    return False

GENERIC_TEMPLATE_PATTERNS = [
    "primary objective of understanding core concepts",
    "recommended diagnostic step",
    "importance of understanding",
    "common challenge in",
    "baseline principles",
    "software implementations in",
    "verification standards in",
    "performance metrics during",
    "what is an important concept in",
    "what is the main goal of",
    "fundamental design principle when building scalable modules in",
    "optimizing latency and throughput in",
    "how should high-availability systems in",
    "protecting data integrity and access control within",
    "automated unit and integration test suites vital when iterating on",
    "maintains deterministic state transitions across complex workflows in"
]

def is_generic(q_text: str) -> bool:
    low = q_text.lower()
    return any(pat in low for pat in GENERIC_TEMPLATE_PATTERNS)

def shuffle_options_and_update_index(item: dict) -> dict:
    original_options = list(item["options"])
    original_idx = item.get("correctOptionIndex", 0)
    if original_idx < 0 or original_idx >= len(original_options):
        original_idx = 0
    correct_text = original_options[original_idx]
    
    paired = [(opt, i == original_idx) for i, opt in enumerate(original_options)]
    random.shuffle(paired)
    
    shuffled_options = [p[0] for p in paired]
    new_correct_idx = next(i for i, p in enumerate(paired) if p[1])
    
    result = dict(item)
    result["options"] = shuffled_options
    result["correctOptionIndex"] = new_correct_idx
    return result

SUBJECT_ALIASES: Dict[str, str] = {
    "data structures & algorithms": "data structures",
    "data structures and algorithms": "data structures",
    "dsa": "data structures",
    "data structures": "data structures",
    "database management systems": "database management systems",
    "dbms": "database management systems",
    "operating systems": "operating systems",
    "os": "operating systems",
    "computer networks": "computer networks",
    "cn": "computer networks",
    "software testing": "software testing",
    "machine learning": "machine learning",
    "ml": "machine learning",
    "blockchain development": "blockchain development",
    "blockchain": "blockchain development",
    "cloud security": "cloud security",
    "digital marketing": "digital marketing",
    "quantum computing": "quantum computing",
    "artificial intelligence": "artificial intelligence",
    "ai": "artificial intelligence",
    "discrete mathematical structures": "discrete mathematical structures",
    "discrete mathematics": "discrete mathematical structures",
    "discrete math": "discrete mathematical structures",
    "dms": "discrete mathematical structures"
}

def normalize_subject_key(raw_subj: str) -> str:
    if not raw_subj:
        return ""
    clean = raw_subj.strip().lower()
    if clean in SUBJECT_ALIASES:
        return SUBJECT_ALIASES[clean]
    for key, canonical in SUBJECT_ALIASES.items():
        if key in clean or clean in key:
            return canonical
    return clean

@app.post("/api/ai/generate-questions", response_model=List[QuestionItem])
def generate_questions(req: QuestionGenRequest):
    subj = req.subject.strip()
    diff = req.difficulty.upper() if req.difficulty else "EASY"
    excluded = req.exclude_questions or []
    count = req.count if req.count and req.count > 0 else 20
    
    subj_key = normalize_subject_key(subj)
    candidate_pool = []
    
    # Extensible lookup in domain knowledge banks
    if subj_key in DOMAIN_KNOWLEDGE_BANKS:
        domain_bank = DOMAIN_KNOWLEDGE_BANKS[subj_key]
        raw_items = domain_bank.get(diff, [])
        if not raw_items:
            for fallback_diff in ["EASY", "MEDIUM", "HARD"]:
                if fallback_diff in domain_bank and domain_bank[fallback_diff]:
                    raw_items.extend(domain_bank[fallback_diff])
            
        for r in raw_items:
            r_shuffled = shuffle_options_and_update_index(r)
            fp = r_shuffled.get("questionFingerprint") or generate_question_fingerprint(r_shuffled["questionText"], subj)
            candidate_pool.append(QuestionItem(
                subject=subj,
                concept=r_shuffled["concept"],
                conceptId=r_shuffled.get("conceptId", f"{subj_key[:4]}_{normalize_text(r_shuffled['concept'])[:10]}"),
                difficulty=diff,
                questionText=r_shuffled["questionText"],
                options=r_shuffled["options"],
                correctOptionIndex=r_shuffled["correctOptionIndex"],
                conceptualExplanation=r_shuffled["conceptualExplanation"],
                questionType=r_shuffled.get("questionType", "Conceptual Understanding"),
                templateFamilyId=r_shuffled.get("templateFamilyId", f"tf_{subj_key[:4]}"),
                questionFingerprint=fp
            ))
    else:
        # Unsupported / unconfigured subject: return empty candidate pool safely without generic template fallback
        print(f"AI Service Warning: Requested subject '{subj}' (key: '{subj_key}') is not present in DOMAIN_KNOWLEDGE_BANKS. Returning empty pool.")
        return []
            
    # Filter out duplicates or generic template matches
    filtered_items = []
    seen_fp = set()
    for item in candidate_pool:
        if not is_generic(item.questionText) and not is_duplicate(item.questionText, excluded, subj):
            if item.questionFingerprint not in seen_fp:
                seen_fp.add(item.questionFingerprint)
                filtered_items.append(item)

    return filtered_items[:count]

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)

