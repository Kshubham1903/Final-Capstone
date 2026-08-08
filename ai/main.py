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
    count: Optional[int] = 4

class QuestionItem(BaseModel):
    subject: str
    concept: str
    difficulty: str
    questionText: str
    options: List[str]
    correctOptionIndex: int
    conceptualExplanation: str

@app.post("/api/ai/adaptive-quiz", response_model=QuizAdaptiveResponse)
def adaptive_quiz(req: QuizPerformanceRequest):
    levels = ["EASY", "MEDIUM", "HARD"]
    current_idx = levels.index(req.current_difficulty)
    
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

def is_duplicate(q_text: str, exclude_list: List[str]) -> bool:
    norm_q = normalize_text(q_text)
    words_q = set(norm_q.split())
    if not norm_q or not words_q:
        return False
    for exc in exclude_list:
        norm_e = normalize_text(exc)
        if norm_q == norm_e:
            return True
        words_e = set(norm_e.split())
        if words_e:
            intersection = words_q.intersection(words_e)
            union = words_q.union(words_e)
            if len(intersection) / len(union) > 0.80:
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
    "what is the main goal of"
]

def is_generic(q_text: str) -> bool:
    low = q_text.lower()
    return any(pat in low for pat in GENERIC_TEMPLATE_PATTERNS)

# Rich Domain Knowledge Repositories
DOMAIN_KNOWLEDGE_BANKS = {
    "blockchain development": {
        "EASY": [
            {
                "concept": "Consensus Mechanisms",
                "questionText": "What core problem does a blockchain consensus mechanism solve in a decentralized network?",
                "options": ["Double-spending and Byzantine Generals Problem", "High network latency", "Excessive database storage usage", "Centralized server failure"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Consensus mechanisms enable distributed nodes to reach agreement on transaction validity without trusting a central authority, preventing double-spending."
            },
            {
                "concept": "Smart Contracts",
                "questionText": "What is the purpose of a smart contract on a blockchain platform like Ethereum?",
                "options": ["Self-executing code stored on-chain that runs automatically when predetermined conditions are met", "Hardware accelerator for node validation", "Encrypted email protocol", "A human-signed legal document scanned into the network"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Smart contracts are immutable programs deployed on a blockchain that execute deterministic logic when triggered by transactions."
            },
            {
                "concept": "Proof of Stake",
                "questionText": "How does Proof of Stake (PoS) differ fundamentally from Proof of Work (PoW)?",
                "options": ["PoS selects block validators based on staked tokens rather than computational hash power", "PoS requires significantly more electricity than PoW", "PoS eliminates transaction fees entirely", "PoW does not use cryptographic hashing"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "PoS replaces energy-intensive mining hardware with capital commitment (staking) to secure the network consensus."
            },
            {
                "concept": "Cryptographic Hash Functions",
                "questionText": "In blockchain data structures, how are blocks cryptographically linked together?",
                "options": ["Each block header includes the cryptographic hash of the preceding block", "Blocks are linked via IP addresses", "Blocks use relational foreign key primary key pairs", "Blocks are merged into a single flat file"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Including the previous block's SHA-256/Keccak hash creates an append-only chain where altering past blocks invalidates all subsequent hashes."
            }
        ],
        "MEDIUM": [
            {
                "concept": "Merkle Trees",
                "questionText": "What is the function of a Merkle Tree root in a blockchain block header?",
                "options": ["Allows efficient and secure verification of transaction inclusion via a single root hash", "Encrypts private keys of network nodes", "Generates random gas prices for smart contracts", "Schedules background cron tasks"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Merkle trees summarize all block transactions into a 32-byte root hash, enabling lightweight client (SPV) verification in O(log N) time."
            },
            {
                "concept": "51% Attack Vectors",
                "questionText": "What vulnerability occurs during a 51% attack on a Proof-of-Work blockchain?",
                "options": ["An entity controlling majority hash power can reverse recent transactions and double-spend", "The attacker steals all user private keys", "The network permanently deletes historical blocks", "The blockchain switches automatically to Proof of Stake"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Controlling >50% hash power allows an attacker to outpace the rest of the network in building a secret longer chain, enabling transaction reversal."
            },
            {
                "concept": "UTXO vs Account Model",
                "questionText": "What distinguishes the Unspent Transaction Output (UTXO) model from an Account/Balance model?",
                "options": ["UTXO tracks discrete coin outputs consumed by new transactions, while Account models update global account balances", "UTXO model requires centralized bank validation", "Account model cannot execute smart contracts", "UTXO model eliminates transaction digital signatures"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Bitcoin uses UTXOs where transactions consume old outputs and create new ones. Ethereum uses an Account model similar to a bank ledger."
            },
            {
                "concept": "EVM Gas Mechanics",
                "questionText": "What is gas in the context of the Ethereum Virtual Machine (EVM)?",
                "options": ["A computational fee unit required to execute operations and prevent infinite loops", "A physical fuel used in mining rigs", "A network bandwidth measurement", "A security vulnerability in Solidity"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Gas measures computational effort. Every EVM instruction costs a set amount of gas, protecting the network from Turing-complete infinite loops."
            }
        ],
        "HARD": [
            {
                "concept": "Layer 2 Zero-Knowledge Rollups",
                "questionText": "How do Zero-Knowledge Rollups (ZK-Rollups) enhance Layer 2 blockchain scaling?",
                "options": ["By bundling off-chain transactions into a single cryptographic validity proof verified on-chain", "By increasing the block size limit on Layer 1 by 100x", "By disabling cryptographic signatures for speed", "By storing all transaction data in a central SQL database"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "ZK-Rollups execute transactions off-chain and post a succinct validity proof (SNARK/STARK) to Layer 1, drastically reducing on-chain gas costs."
            },
            {
                "concept": "Smart Contract Reentrancy",
                "questionText": "What is a Reentrancy Attack in Solidity smart contract development?",
                "options": ["An attack where an external contract recursively calls back into a vulnerable function before state changes complete", "A brute-force attack on user private keys", "A denial of service attack on RPC endpoints", "An unauthorized modification of EVM bytecode"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Reentrancy occurs when an external call is made before updating state balances, allowing the fallback function to drain funds recursively."
            },
            {
                "concept": "Blockchain Oracles",
                "questionText": "What vital role does a Decentralized Oracle Network (like Chainlink) play in smart contract execution?",
                "options": ["Bridges off-chain real-world data (such as market prices or weather) deterministically into smart contracts", "Compiles Solidity code into EVM bytecode", "Generates public/private key pairs for users", "Encrypts transaction payloads"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Blockchains cannot natively make HTTP requests. Oracles query external web APIs and push validated data onto the blockchain."
            },
            {
                "concept": "Hard vs Soft Forks",
                "questionText": "What is the difference between a Hard Fork and a Soft Fork in protocol upgrades?",
                "options": ["A Hard Fork is non-backwards-compatible requiring all nodes to upgrade, while a Soft Fork is backwards-compatible", "A Soft Fork splits the network permanently into two separate cryptocurrencies", "Hard Forks do not require node operator consensus", "Soft Forks require changing the hashing algorithm"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Hard forks relax or change rules so older nodes reject new blocks. Soft forks tighten rules so older nodes still accept new blocks."
            }
        ]
    },
    "cloud security": {
        "EASY": [
            {
                "concept": "Identity & Access Management",
                "questionText": "Which IAM principle ensures users receive only the permissions required for their specific job tasks?",
                "options": ["Principle of Least Privilege", "Role-Based Overdrive", "Implicit Allow Access", "Root Credentials Sharing"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "The Principle of Least Privilege dictates granting users the minimum necessary access required to complete designated assignments."
            },
            {
                "concept": "Shared Responsibility Model",
                "questionText": "In the Cloud Shared Responsibility Model, which security layer is managed primarily by the cloud provider?",
                "options": ["Physical infrastructure and data center facility security", "User password strength policies", "Customer application code vulnerability patching", "S3 bucket public access settings"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Cloud providers manage security 'of' the cloud (hardware, facilities, host OS), while customers secure data 'in' the cloud."
            },
            {
                "concept": "Virtual Network Firewalls",
                "questionText": "What is the function of an AWS Security Group in cloud networking?",
                "options": ["A stateful virtual firewall controlling inbound and outbound traffic at the instance level", "A customer user directory for web applications", "An automated backup scheduler for EBS volumes", "A DNS routing registrar"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Security Groups operate statefully at the ENI/instance level, automatically allowing return traffic for outbound requests."
            }
        ],
        "MEDIUM": [
            {
                "concept": "Zero Trust Architecture",
                "questionText": "What is the primary architectural pillar behind Zero Trust Security in cloud environments?",
                "options": ["Never trust, always verify every access request regardless of network origin", "Trust all internal network traffic behind a perimeter firewall", "Disable encryption for verified internal microservices", "Grant full root rights to internal IP subnets"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Zero Trust assumes threats exist inside and outside the network, enforcing strict authentication, authorization, and continuous validation."
            },
            {
                "concept": "Envelope Encryption",
                "questionText": "How does an Envelope Encryption model protect sensitive cloud data using AWS KMS?",
                "options": ["Data is encrypted with a Data Encryption Key (DEK), which is itself encrypted under a KMS Key Encryption Key (KEK)", "Data is sent unencrypted over HTTP to KMS for storage", "Data encryption keys are hardcoded into source repositories", "KMS stores unencrypted plaintext in temporary cache"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Envelope encryption uses a plaintext Data Key to encrypt data locally, then encrypts the Data Key with a master KMS Key."
            },
            {
                "concept": "Security Groups vs NACLs",
                "questionText": "What is the key difference between AWS Security Groups and Network Access Control Lists (NACLs)?",
                "options": ["Security Groups are stateful at the instance level; NACLs are stateless at the subnet level", "Security Groups apply to VPC routers while NACLs apply to IAM roles", "NACLs are stateful while Security Groups are stateless", "Security Groups block outbound traffic by default"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Security Groups are stateful and evaluate rules per instance. NACLs are stateless subnet boundaries requiring explicit inbound/outbound rules."
            }
        ],
        "HARD": [
            {
                "concept": "Cloud Transit Gateways",
                "questionText": "How does a Cloud Transit Gateway simplify multi-VPC security monitoring?",
                "options": ["Acts as a central hub routing traffic across VPCs with consolidated firewall inspection", "Automatically generates SSL certificates for all endpoints", "Replaces load balancers with static DNS records", "Stores database encryption keys in memory"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Transit Gateways eliminate complex mesh VPC peering by acting as a central router with centralized traffic logging and inspection."
            },
            {
                "concept": "IMDSv2 SSRF Mitigation",
                "questionText": "What security control in AWS Instance Metadata Service Version 2 (IMDSv2) mitigates SSRF attacks?",
                "options": ["Requiring a session token via an HTTP PUT request prior to retrieving metadata", "Disabling HTTP access to all public endpoints", "Using static IP addresses for container pods", "Encrypting S3 bucket policies"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "IMDSv2 mandates a session token via HTTP PUT, preventing simple WAF/SSRF bypasses that cannot execute multi-header PUT calls."
            }
        ]
    },
    "digital marketing": {
        "EASY": [
            {
                "concept": "SEO Optimization",
                "questionText": "What is the primary function of a canonical URL tag (`rel='canonical'`) in Search Engine Optimization?",
                "options": ["Prevents duplicate content penalties by specifying the preferred master URL", "Increases page load speed on mobile devices", "Hides website content from search engine indexers", "Generates pay-per-click ad headlines automatically"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Canonical tags tell search engine crawlers which version of a URL represents the main copy when multiple pages have similar content."
            },
            {
                "concept": "PPC Metrics",
                "questionText": "In digital pay-per-click (PPC) advertising, how is Click-Through Rate (CTR) calculated?",
                "options": ["Total Clicks divided by Total Impressions multiplied by 100", "Total Conversions divided by Total Clicks", "Total Cost divided by Total Sales", "Total Impressions divided by Bounce Rate"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "CTR measures ad engagement ratio: `(Clicks / Impressions) * 100`."
            },
            {
                "concept": "Customer Acquisition",
                "questionText": "What metric measures the average marketing expense incurred to acquire a single paying customer?",
                "options": ["Customer Acquisition Cost (CAC)", "Return on Ad Spend (ROAS)", "Lifetime Value (LTV)", "Cost Per Mille (CPM)"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "CAC equals total marketing and sales expenditure over a period divided by total new customers acquired."
            }
        ],
        "MEDIUM": [
            {
                "concept": "A/B Split Testing",
                "questionText": "When conducting an A/B split test on a landing page, what is a fundamental rule for valid statistical results?",
                "options": ["Test only one single variable change (such as headline or CTA button color) at a time", "Change the headline, offer, and layout simultaneously", "Stop the test as soon as 10 visitors convert", "Run Variant A on weekdays and Variant B on weekends"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Isolating a single variable ensures that observed conversion rate differences can be confidently attributed to that specific element."
            },
            {
                "concept": "Google Ads Quality Score",
                "questionText": "Which three core factors determine the Google Ads Quality Score for a keyword?",
                "options": ["Expected CTR, Ad Relevance, and Landing Page Experience", "Daily Budget, Account Age, and Competitor Count", "Social Media Shares, Domain Age, and Image Size", "Keyword Length, CPC Bid, and Time of Day"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "Quality Score (1-10) is calculated from Expected CTR, Ad Relevance to user intent, and Landing Page Experience."
            }
        ],
        "HARD": [
            {
                "concept": "LTV to CAC Ratio Analysis",
                "questionText": "Why is a Customer Lifetime Value to Customer Acquisition Cost ratio (LTV:CAC) of 3:1 considered optimal for SaaS growth?",
                "options": ["It indicates sustainable profitability while maintaining healthy reinvestment capital for expansion", "It means the company is losing money on every acquired customer", "It proves that sales cycles take more than 12 months", "It requires zero spend on organic search acquisition"],
                "correctOptionIndex": 0,
                "conceptualExplanation": "An LTV:CAC ratio of 3:1 balances high customer return with effective sales acquisition spend without under-investing in growth."
            }
        ]
    }
}

def generate_dynamic_subject_questions(subject: str, difficulty: str, count: int = 4) -> List[QuestionItem]:
    items = []
    subj_clean = subject.strip()
    diff = difficulty.upper()
    
    templates_pool = [
        {
            "concept": f"{subj_clean} Architecture",
            "q_text": f"What is a fundamental design principle when building scalable modules in {subj_clean}?",
            "opts": [
                f"Ensuring loose coupling and modular separation of concerns across {subj_clean} components",
                f"Tightly coupling all business logic into a single monolithic script",
                f"Disabling exception handling during high-throughput processing",
                f"Hardcoding configuration parameters directly into production binaries"
            ],
            "idx": 0,
            "exp": f"Modular separation of concerns ensures maintainability and clean component isolation in {subj_clean} architectures."
        },
        {
            "concept": f"{subj_clean} Performance Optimization",
            "q_text": f"When optimizing latency and throughput in {subj_clean}, which approach yields the most reliable performance gains?",
            "opts": [
                f"Unbounded recursive iteration without memoization",
                f"Identifying performance bottlenecks via profiling and optimizing critical execution paths in {subj_clean}",
                f"Disabling caching layers across all service interfaces",
                f"Increasing hardware allocation without profiling underlying bottlenecks"
            ],
            "idx": 1,
            "exp": f"Targeted profiling pinpoints exact bottlenecks, allowing data-driven optimization in {subj_clean} workflows."
        },
        {
            "concept": f"{subj_clean} Error Handling & Resilience",
            "q_text": f"How should high-availability systems in {subj_clean} handle transient fault conditions?",
            "opts": [
                f"Ignoring errors and returning empty unvalidated payloads",
                f"Implementing exponential backoff retry strategies with circuit breakers in {subj_clean}",
                f"Terminating the host process immediately upon receiving a non-fatal warning",
                f"Bypassing input validation checks during peak traffic"
            ],
            "idx": 1,
            "exp": f"Exponential backoff and circuit breakers prevent cascading failures and handle transient disruptions resiliently in {subj_clean}."
        },
        {
            "concept": f"{subj_clean} Security & Data Integrity",
            "q_text": f"What practice is critical for protecting data integrity and access control within {subj_clean} workflows?",
            "opts": [
                f"Validating and sanitizing all inputs at system boundaries before processing in {subj_clean}",
                f"Storing API keys in client-side public assets",
                f"Disabling TLS encryption for internal microservices",
                f"Granting default administrative permissions to external client tokens"
            ],
            "idx": 0,
            "exp": f"Input validation and boundary sanitization prevent injection vulnerabilities and secure {subj_clean} operations."
        },
        {
            "concept": f"{subj_clean} Testing & Quality Assurance",
            "q_text": f"Why are automated unit and integration test suites vital when iterating on {subj_clean} codebases?",
            "opts": [
                f"They catch regression bugs early and verify that refactored code meets functional specifications in {subj_clean}",
                f"They slow down production deployment pipelines unnecessarily",
                f"They replace the need for code review and documentation",
                f"They increase memory usage of compiled production binaries"
            ],
            "idx": 0,
            "exp": f"Automated test suites provide rapid feedback, preventing regressions when adding features to {subj_clean} codebases."
        },
        {
            "concept": f"{subj_clean} State Management",
            "q_text": f"Which strategy maintains deterministic state transitions across complex workflows in {subj_clean}?",
            "opts": [
                f"Mutating global shared state concurrently without synchronization locks",
                f"Enforcing immutable data models and explicit state transition handlers in {subj_clean}",
                f"Persisting transient draft states directly to global production storage",
                f"Bypassing state validation checks during async dispatches"
            ],
            "idx": 1,
            "exp": f"Immutable state models and explicit transitions prevent race conditions and ensure predictable behavior in {subj_clean}."
        }
    ]

    for t in templates_pool:
        if not is_generic(t["q_text"]):
            items.append(QuestionItem(
                subject=subj_clean,
                concept=t["concept"],
                difficulty=diff,
                questionText=t["q_text"],
                options=t["opts"],
                correctOptionIndex=t["idx"],
                conceptualExplanation=t["exp"]
            ))

    return items[:count]

@app.post("/api/ai/generate-questions", response_model=List[QuestionItem])
def generate_questions(req: QuestionGenRequest):
    subj = req.subject.strip()
    diff = req.difficulty.upper() if req.difficulty else "EASY"
    excluded = req.exclude_questions or []
    count = req.count if req.count and req.count > 0 else 4
    
    subj_key = subj.lower()
    candidate_pool = []
    
    # 1. Lookup in curated domain knowledge banks
    if subj_key in DOMAIN_KNOWLEDGE_BANKS:
        domain_bank = DOMAIN_KNOWLEDGE_BANKS[subj_key]
        raw_items = domain_bank.get(diff, [])
        if not raw_items and "EASY" in domain_bank:
            raw_items = domain_bank["EASY"]
        if not raw_items and "MEDIUM" in domain_bank:
            raw_items = domain_bank["MEDIUM"]
            
        for r in raw_items:
            candidate_pool.append(QuestionItem(
                subject=subj,
                concept=r["concept"],
                difficulty=diff,
                questionText=r["questionText"],
                options=r["options"],
                correctOptionIndex=r["correctOptionIndex"],
                conceptualExplanation=r["conceptualExplanation"]
            ))
            
    # 2. Filter out duplicates or generic template matches
    filtered_items = []
    for item in candidate_pool:
        if not is_generic(item.questionText) and not is_duplicate(item.questionText, excluded):
            filtered_items.append(item)
            
    # 3. If candidate pool is insufficient, generate dynamic subject-relevant questions
    if len(filtered_items) < count:
        dynamic_items = generate_dynamic_subject_questions(subj, diff, count * 2)
        for dyn in dynamic_items:
            if not is_generic(dyn.questionText) and not is_duplicate(dyn.questionText, excluded) and not any(f.questionText == dyn.questionText for f in filtered_items):
                filtered_items.append(dyn)
                if len(filtered_items) >= count:
                    break

    return filtered_items[:count]

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)

