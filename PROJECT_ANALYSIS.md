# EduPilot AI - Complete Project Analysis

## Executive Summary

### Project Purpose
**EduPilot AI** is an AI-driven Personalized Learning & Academic Growth Operating System (OS). It fuses lifestyle metrics (sleep hours, screen time, study duration, physical exercise, stress index), academic diagnostics (CGPA targets, concept mastery levels across subjects), adaptive real-time quizzes, and machine learning performance prediction to calculate a **Student Growth Index (SGI: 0.0 - 10.0)**, predict academic risk levels (`LOW`, `MEDIUM`, `HIGH`), and generate real-time personalized recommendations and career guidance.

### Current Status
The project possesses a fully operational polyglot microservices architecture. Both online REST-based API communication (Spring Boot + FastAPI) and client-side offline fallback logic (LocalStorage) are implemented across the system. The frontend Vite migration from Next.js is 100% verified.

### Completion %
- **Overall Project Completion**: **78%**

### Architecture Summary
EduPilot AI utilizes a containerized, decoupled microservices architecture orchestrating five primary services:
1. **Frontend UI**: React 19, Vite 6, TypeScript, Tailwind CSS, React Router DOM v7 (Port `3000`).
2. **Backend API Gatekeeper**: Spring Boot 3.4.1, Spring Security, Spring Data MongoDB, Spring Data Redis (Port `8080`).
3. **AI Core Analytics Microservice**: FastAPI, Uvicorn, Scikit-Learn, Joblib, Pandas, NumPy (Port `8000`).
4. **Primary Database**: MongoDB 7.0 (Port `27017`).
5. **Cache Layer**: Redis Alpine (Port `6379`).

```
[ Frontend (React 19 + Vite) :3000 ]
             │
             ▼ (HTTP / REST)
[ Spring Boot API Gateway :8080 ]
        │             │
        ▼             ▼
  (MongoDB :27017) (Redis :6379)
        │
        ▼ (RestTemplate REST Relay)
[ FastAPI AI Microservice :8000 ]
        │
        ▼
[ Random Forest ML Model (.pkl) ]
```

### Technology Stack
- **Frontend**: React 19.0.0, Vite 6.4.3, TypeScript 5.7, Tailwind CSS 3.4, Framer Motion 12.4, Lucide React 0.475, Recharts 2.15, React Router DOM 7.1.
- **Backend API**: Java 17, Spring Boot 3.4.1, Spring Security, Spring Data MongoDB, Spring Data Redis, Lombok, Jackson.
- **AI Service**: Python 3.10+, FastAPI 0.110, Uvicorn, Scikit-Learn 1.4, Joblib 1.3, Pandas 2.2, NumPy 1.26, Pydantic 2.6.
- **ML Training Pipeline**: RandomForestClassifier (300 estimators, balanced class weights), LabelEncoders, SimpleImputer, qcut quantile split on 6,400+ student samples (`StudentPerformanceFactors.csv`).
- **Infrastructure & Orchestration**: Docker, Docker Compose (v3.8 specification).

---

## Folder Structure

```
EduPilot AI Root (Capston/)
├── .git/                        # Git version control metadata
├── .vscode/                     # IDE workspace configurations
├── ai/                          # Python FastAPI Microservice
│   ├── __pycache__/             # Python bytecode cache
│   ├── Dockerfile               # Container spec for FastAPI (python:3.10-slim)
│   ├── main.py                  # Core FastAPI web server, SGI logic & endpoint routes
│   └── requirements.txt         # Python dependencies (FastAPI, scikit-learn, joblib, etc.)
├── backend/                     # Java Spring Boot Microservice
│   ├── .maven_portable/         # Portable Maven distribution wrapper
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/edupilot/
│   │   │   │   ├── config/
│   │   │   │   │   ├── DatabaseSeeder.java   # Seeds initial users, profiles & quiz bank
│   │   │   │   │   └── SecurityConfig.java   # Spring Security & CORS configuration
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java   # /api/auth/register & /api/auth/login
│   │   │   │   │   ├── QuizController.java   # /api/quizzes/questions & /submit
│   │   │   │   │   └── StudentController.java# /api/students/onboard, /profile, /lifestyle
│   │   │   │   ├── model/
│   │   │   │   │   ├── LifestyleData.java          # Daily lifestyle tracking log entity
│   │   │   │   │   ├── LifestyleQuestionnaire.java # 20-feature questionnaire model
│   │   │   │   │   ├── QuizQuestion.java           # Question bank entity
│   │   │   │   │   ├── StudentProfile.java         # Master student profile document
│   │   │   │   │   └── User.java                   # Auth user account entity
│   │   │   │   ├── repository/
│   │   │   │   │   ├── LifestyleDataRepository.java
│   │   │   │   │   ├── LifestyleQuestionnaireRepository.java
│   │   │   │   │   ├── QuizQuestionRepository.java
│   │   │   │   │   ├── StudentProfileRepository.java
│   │   │   │   │   └── UserRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AiServiceClient.java # RestTemplate client for FastAPI AI service
│   │   │   │   │   └── StudentService.java  # Business logic & background predictors
│   │   │   │   └── EduPilotApplication.java # Spring Boot entry point (@EnableCaching)
│   │   │   └── resources/
│   │   │       └── application.yml # MongoDB, Redis & AI service config properties
│   ├── Dockerfile               # Container spec for Spring Boot (eclipse-temurin:17)
│   ├── pom.xml                  # Maven POM file with Spring Boot dependencies
│   ├── run_backend.ps1          # PowerShell execution script for local backend dev
│   └── setup_maven.ps1          # Automated Maven environment setup script
├── frontend/                    # React 19 + Vite Web Application
│   ├── public/                  # Static web assets (favicon.ico)
│   ├── src/
│   │   ├── app/                 # Page route views
│   │   │   ├── admin/
│   │   │   │   └── page.tsx      # System Control & Admin Operations Dashboard
│   │   │   ├── dashboard/
│   │   │   │   ├── career/
│   │   │   │   │   └── page.tsx  # Career Guidance Center & AI Skill Gap Analyzer
│   │   │   │   ├── pomodoro/
│   │   │   │   │   └── page.tsx  # Pomodoro focus timer with Lofi audio generator
│   │   │   │   ├── quizzes/
│   │   │   │   │   └── page.tsx  # Adaptive quiz execution UI with instant feedback
│   │   │   │   └── page.tsx      # Student Analytics Dashboard (SGI, CGPA, radar charts)
│   │   │   ├── faculty/
│   │   │   │   ├── quiz-manager/
│   │   │   │   │   └── page.tsx  # Quiz Question Authoring & Management UI
│   │   │   │   └── page.tsx      # Faculty Class Performance & Risk Monitor
│   │   │   ├── onboarding/
│   │   │   │   └── page.tsx      # 5-Step diagnostic student onboarding wizard
│   │   │   ├── globals.css       # Global Tailwind CSS, theme tokens & custom glassmorphism
│   │   │   └── page.tsx          # Landing Page & Auth Modal (Login/Register)
│   │   ├── components/
│   │   │   └── Layout.tsx        # Responsive Layout frame, sidebar navigation, theme toggle
│   │   ├── services/
│   │   │   ├── api.ts            # REST client with backend detection & local fallback
│   │   │   └── mockData.ts       # Fallback mock data structures, question bank & SGI math
│   │   ├── App.tsx               # React Router DOM v7 route provider setup
│   │   └── main.tsx              # Vite DOM entry point
│   ├── Dockerfile               # Container spec for React Frontend (Node 20 Alpine)
│   ├── package.json             # NPM dependencies and scripts
│   ├── tsconfig.json            # TypeScript compiler configuration
│   └── vite.config.ts           # Vite build config with path aliases (@ -> ./src)
├── StudentGrowthMLmodel/        # Standalone ML Model Training Pipeline
│   ├── dataset/
│   │   └── StudentPerformanceFactors.csv # 6,419 rows dataset for ML training
│   ├── outputs/                 # Output directories for trained metrics
│   ├── src/                     # Modular ML engine scripts
│   │   ├── api.py               # ML API routes script
│   │   ├── compare_model.py     # Benchmark model evaluator (RF vs GB vs DT)
│   │   ├── eda.py               # Exploratory Data Analysis & visualizer
│   │   ├── preprocess.py        # Data cleaning & encoding script
│   │   ├── recommendation_engine.py # Rules & ML recommendation generator
│   │   ├── risk_engine.py       # Academic risk classifier logic
│   │   └── train_model.py       # Model training pipeline execution script
│   ├── label_encoders.pkl       # Serialized feature label encoders
│   ├── performance_prediction.py# Primary single-file training & model exporter script
│   ├── student_growth_model.pkl # Serialized Random Forest model (~64MB)
│   └── target_encoder.pkl       # Serialized target variable label encoder
├── docker-compose.yml           # Multi-service container orchestration config
├── projectflowtracker.md        # Tracked record of Vite migration tasks
├── report.md                    # React + Vite verification audit report
└── PROJECT_ANALYSIS.md          # Comprehensive Architecture Discovery Document
```

---

## System Architecture

```
                               ┌─────────────────────────────────────────┐
                               │             USER BROWSER                │
                               └────────────────────┬────────────────────┘
                                                    │
                                                    ▼
                               ┌─────────────────────────────────────────┐
                               │   Frontend Service (Vite + React 19)    │
                               │               Port 3000                 │
                               └────────────────────┬────────────────────┘
                                                    │ HTTP REST Requests
                                                    ▼
                               ┌─────────────────────────────────────────┐
                               │    Backend API Service (Spring Boot)    │
                               │               Port 8080                 │
                               └─────────┬──────────┬──────────┬─────────┘
                                         │          │          │
                     ┌───────────────────┘          │          └───────────────────┐
                     │                              │                              │
                     ▼                              ▼                              ▼
     ┌──────────────────────────────┐   ┌──────────────────────┐   ┌──────────────────────────────┐
     │   MongoDB Database (:27017)  │   │  Redis Cache (:6379) │   │ FastAPI AI Service (:8000)   │
     │ - users                      │   │ (Configured for HTTP │   │ - predict-performance        │
     │ - student_profiles           │   │  response caching)   │   │ - predict-student-development│
     │ - lifestyle_data             │   └──────────────────────┘   │ - adaptive-quiz              │
     │ - lifestyle_questionnaires   │                              └──────────────┬───────────────┘
     │ - quiz_questions             │                                             │
     └──────────────────────────────┘                                             ▼
                                                                   ┌──────────────────────────────┐
                                                                   │ Random Forest Model (.pkl)   │
                                                                   └──────────────────────────────┘
```

### Communication Protocols
1. **Frontend → Backend**: Asynchronous HTTP REST calls via standard `fetch` API. JSON payload exchange. Automatic offline detection switches transparently to local browser storage if the Spring Boot backend is unreachable.
2. **Backend → MongoDB**: Synchronous Spring Data Repositories operating over MongoDB wire protocol on TCP port `27017`.
3. **Backend → Redis**: Spring Data Redis connection configured on TCP port `6379` (Caching enabled via `@EnableCaching`).
4. **Backend → FastAPI**: Synchronous REST integration using Spring `RestTemplate`. The backend translates Spring models into Pydantic-compliant request bodies for FastAPI on HTTP port `8000`.
5. **FastAPI → ML Model**: Local in-memory inference executing joblib-loaded Scikit-Learn `RandomForestClassifier` and label encoders.

---

## Frontend Analysis

### Pages Overview
- **Landing & Auth (`src/app/page.tsx`)**: Hero section featuring feature highlights, dark/light aesthetics, interactive modal supporting student/faculty/admin authentication with toggleable login & registration modes.
- **Onboarding (`src/app/onboarding/page.tsx`)**: 5-step wizard capturing course details, subjects, career goals, target CGPA, study hours, sleep habits, stress levels, exercise time, and preferred learning style.
- **Student Dashboard (`src/app/dashboard/page.tsx`)**: Primary analytics view featuring SGI radial gauge, predicted CGPA progress cards, radar chart breakdown of mastery vs lifestyle, weekly activity bar charts, daily lifestyle logging drawer, and AI copilot insight list.
- **Career Center (`src/app/dashboard/career/page.tsx`)**: AI skill gap analyzer comparing current student competencies against target industry roles (e.g., AI/ML Specialist, Full Stack Developer, Data Engineer), required skill checklists, and recommended certifications.
- **Pomodoro Focus Timer (`src/app/dashboard/pomodoro/page.tsx`)**: Customizable Pomodoro timer (25m work / 5m short break / 15m long break), integrated web audio oscillator engine producing ambient Lofi beats and white noise, and focus log tracker.
- **Adaptive Quizzes (`src/app/dashboard/quizzes/page.tsx`)**: Quiz engine that dynamically fetches questions based on subject and difficulty. Adjusts difficulty in real-time based on accuracy and timer speed.
- **Faculty Dashboard (`src/app/faculty/page.tsx`)**: Class performance analytics, student risk distribution pie charts, filterable student roster with risk level tags (`LOW`, `MEDIUM`, `HIGH`), and intervention request tools.
- **Quiz Manager (`src/app/faculty/quiz-manager/page.tsx`)**: Question authoring interface allowing faculty to author multiple-choice questions with conceptual explanations and target difficulty tiers.
- **Admin Dashboard (`src/app/admin/page.tsx`)**: System control console displaying cluster node metrics, DB sync status, global user management tables, and emergency broadcast creation.

### Components & Services
- `Layout.tsx`: Common shell with dynamic sidebar links according to active user role (`STUDENT`, `FACULTY`, `ADMIN`), responsive mobile navbar, top header bar, light/dark theme switch, streak counter, and notification dropdown.
- `api.ts`: Central API access module managing connection checks, REST invocations, and fallback switches.
- `mockData.ts`: Client-side fallback state manager with static seed question banks and LocalStorage persistence logic.

### Routing & State Management
- Routing handled entirely by `react-router-dom` v7 (`BrowserRouter` in `src/main.tsx`, `<Routes>` in `src/App.tsx`).
- React state managed via standard `useState` and `useEffect` combined with `localStorage` persistence.

### Completion Percentage
- **Frontend Completion**: **90%**

---

## Backend Analysis

### Core Infrastructure
- **Framework**: Spring Boot 3.4.1 running on Java 17.
- **Security Configuration (`SecurityConfig.java`)**: Configures CORS (`allowedOrigins("*")`) and disables CSRF. Currently sets `anyRequest().permitAll()`.
- **Database Seeder (`DatabaseSeeder.java`)**: Implements `CommandLineRunner` to seed default accounts (`student_1`, `faculty_1`, `admin_1`), default `StudentProfile`, and initial `QuizQuestion` set upon application boot.

### Controllers & Endpoints

#### 1. `AuthController` (`/api/auth`)
- `POST /api/auth/register`
  - **Purpose**: Registers a new user.
  - **Input**: `User` object (JSON: `email`, `password`, `fullName`, `role`).
  - **Output**: `200 OK` with `userId` and `role`, or `400 Bad Request` if email exists.
  - **Logic**: Saves user to MongoDB `users` collection. *Note: Passwords are saved in plain text.*
- `POST /api/auth/login`
  - **Purpose**: Authenticates user credentials.
  - **Input**: JSON map containing `email` and `password`.
  - **Output**: `200 OK` with mock JWT token (`eyJhbGci...`), `role`, `fullName`, `email`, `userId`, or `401 Unauthorized`.

#### 2. `StudentController` (`/api/students`)
- `GET /api/students/onboarding-status/{userId}`
  - **Purpose**: Backend source of truth guard endpoint verifying profile completion.
  - **Output**: JSON with `isCompleted` boolean and step progress breakdown (`personalCompleted`, `academicCompleted`, `questionnaireCompleted`, `completionPercentage`, `currentStep`).
- `POST /api/students/onboard/step`
  - **Purpose**: Auto-saves partial step progress to MongoDB during onboarding.
  - **Input**: `userId`, `step` (1-7), and section `data`.
- `POST /api/students/onboard`
  - **Input**: Payload with `userId`, `course`, `semester`, `subjects`, `careerGoals`, `preferredStudyHoursPerDay`, `targetCgpa`, `sleepHours`, `stressLevel`, `exerciseMinutes`, `learningStyle`.
  - **Output**: `StudentProfile` entity.
  - **Logic**: Marks profile complete, saves normalized MongoDB collections, runs background ML prediction, saves profile.
- `GET /api/students/profile/{userId}`
  - **Output**: `StudentProfile` matching `userId`. Returns `404 Not Found` if missing.
- `GET /api/students/profile-full/{userId}`
  - **Output**: Aggregated student object containing personalInfo, academicProfile, lifestyleQuestionnaire, onboardingStatus, and studentProfile.
- `PUT /api/students/profile/{userId}`
  - **Purpose**: Updates profile details in MongoDB and re-executes AI SGI & predicted CGPA recalculation live.
- `POST /api/students/lifestyle/{profileId}`
  - **Input**: `LifestyleData` object.
  - **Logic**: Persists log into `lifestyle_data` collection, triggers background AI analytics update on profile, returns updated profile.
- `POST /api/students/questionnaire/{profileId}`
  - **Input**: `LifestyleQuestionnaire` object (19 ML features).
  - **Logic**: Saves questionnaire to `lifestyle_questionnaires`, triggers FastAPI prediction sync, updates profile metrics.
- `GET /api/students/questionnaire/{profileId}`
  - **Output**: Returns saved `LifestyleQuestionnaire` for the given profile ID.
- `GET /api/students/recommendations/{profileId}`
  - **Output**: Fetches AI recommendation insights, predicted CGPA, SGI score from FastAPI or returns rule-based fallback.
- `GET /api/students/health`
  - **Output**: `{"status": "UP"}`.

#### 3. `QuizController` (`/api/quizzes`)
- `GET /api/quizzes/questions`
  - **Query Params**: `subject`, `difficulty`.
  - **Output**: List of up to 4 shuffled `QuizQuestion` objects matching subject and difficulty.
- `POST /api/quizzes/submit`
  - **Input**: JSON with `profileId`, `subject`, `concept`, `difficulty`, `isCorrect`, `responseTimeSeconds`.
  - **Logic**: Calls FastAPI `adaptive-quiz` endpoint for difficulty adjustment, updates `conceptMastery` (+8.0 on correct, -3.0 on incorrect), updates weak/strong concept lists, triggers background predictor update.
  - **Output**: Next recommended difficulty tier and conceptual reasoning.
- `POST /api/quizzes`
  - **Input**: `QuizQuestion` entity.
  - **Logic**: Saves new question authored by faculty/admin to MongoDB.

### Completion Percentage
- **Backend Completion**: **80%**

---

## AI Analysis

### Architecture & Service Design
The AI Engine is built using **FastAPI** (`ai/main.py`). It exposes microservice endpoints for ML predictions, SGI profiling, and adaptive quiz logic.

### Model Loading & Fallback Safety
Upon startup, FastAPI attempts to load:
1. `StudentGrowthMLmodel/student_growth_model.pkl` (Random Forest classifier)
2. `StudentGrowthMLmodel/label_encoders.pkl` (Feature encoders)
3. `StudentGrowthMLmodel/target_encoder.pkl` (Target encoder)

If model files are missing or unreadable, the service gracefully switches to deterministic rule-based fallback calculations without throwing runtime crashes.

### Algorithm & Formulas

#### 1. Student Growth Index (SGI) Formula
SGI is calculated on a scale of `0.0` to `10.0`:
$$\text{SGI} = \left( \text{AcademicScore} \times 0.40 \right) + \left( \text{ConceptMastery} \times 0.30 \right) + \left( \text{LifestyleScore} \times 0.20 \right) + \left( \text{ConsistencyScore} \times 0.10 \right)$$
- **AcademicScore**: Normalized CGPA $(\text{CGPA} / 10.0) \times 100$.
- **ConceptMastery**: Mean value of subject mastery scores.
- **LifestyleScore**: Average score across sleep adequacy (optimal 8h), stress mitigation, exercise duration, low screen time, and study time.
- **ConsistencyScore**: Quiz completion velocity.

#### 2. ML Prediction Flow (`/api/ai/predict-student-development`)
Constructs a Pandas DataFrame with 19 encoded features:
`Hours_Studied`, `Attendance`, `Parental_Involvement`, `Access_to_Resources`, `Extracurricular_Activities`, `Sleep_Hours`, `Previous_Scores`, `Motivation_Level`, `Internet_Access`, `Tutoring_Sessions`, `Family_Income`, `Teacher_Quality`, `School_Type`, `Peer_Influence`, `Physical_Activity`, `Learning_Disabilities`, `Parental_Education_Level`, `Distance_from_Home`, `Gender`.

Passes DataFrame to `rf_model.predict()` to obtain performance level (`Low`, `Medium`, `High`), which maps directly to academic risk levels (`HIGH`, `MEDIUM`, `LOW`) and predicted CGPA scores.

#### 3. Adaptive Quiz Algorithm (`/api/ai/adaptive-quiz`)
- Correct answer & response time $< 15\text{s}$: Escalate difficulty tier immediately (`EASY` $\rightarrow$ `MEDIUM` $\rightarrow$ `HARD`).
- Incorrect answer & response time $> 45\text{s}$: Reduce difficulty tier immediately to prevent frustration and reinforce core fundamentals.

### Completion Percentage
- **AI Service Completion**: **85%**

---

## Machine Learning Model Analysis

### Training Pipeline (`StudentGrowthMLmodel/`)
- **Dataset**: `StudentPerformanceFactors.csv` containing 6,419 records.
- **Target Engineering**: Original continuous `Exam_Score` is converted into 3 balanced quantile tiers using `pd.qcut`: `Low`, `Medium`, `High`.
- **Preprocessing (`preprocess.py`)**:
  - Missing numeric values imputed using median strategy (`SimpleImputer(strategy="median")`).
  - Missing categorical values imputed using most-frequent strategy (`SimpleImputer(strategy="most_frequent")`).
  - Categorical variables encoded using `LabelEncoder`.
- **Model Classifier**:
  - Algorithm: `RandomForestClassifier`
  - Hyperparameters: `n_estimators=300`, `class_weight="balanced"`, `random_state=42`.
  - Artifact Size: `student_growth_model.pkl` (~64.2 MB).

### Feature Importance Top Ranking
1. `Hours_Studied`
2. `Attendance`
3. `Previous_Scores`
4. `Tutoring_Sessions`
5. `Sleep_Hours`

---

## Database Analysis

### MongoDB Collections & Schemas

1. **`users`**
   - Fields: `_id`, `email`, `password`, `fullName`, `role` (`STUDENT`, `FACULTY`, `ADMIN`), `createdAt`.
   - Index: Unique index on `email`.
2. **`student_profiles`**
   - Fields: `_id`, `userId`, `course`, `semester`, `subjects`, `careerGoals`, `preferredStudyHoursPerDay`, `targetCgpa`, `consistencyScore`, `productivityScore`, `lifestyleScore`, `learningStyle`, `currentStreakCount`, `studentGrowthIndex`, `conceptMastery` (Map), `weakConcepts` (Map), `strongConcepts` (Map), `completedQuizzesCount`, `predictedCgpa`, `academicRiskLevel`, demographic questionnaire fields.
   - Foreign Key: `userId` references `users._id`.
3. **`lifestyle_data`**
   - Fields: `_id`, `studentProfileId`, `date`, `sleepHours`, `screenTimeHours`, `stressLevel`, `exerciseMinutes`, `studyMinutes`, `attendanceRate`, `productivityRating`.
   - Foreign Key: `studentProfileId` references `student_profiles._id`.
4. **`lifestyle_questionnaires`**
   - Fields: `_id`, `studentProfileId`, 20 detailed lifestyle and socioeconomic indicator fields.
   - Foreign Key: `studentProfileId` references `student_profiles._id`.
5. **`quiz_questions`**
   - Fields: `_id`, `subject`, `concept`, `difficulty` (`EASY`, `MEDIUM`, `HARD`), `questionText`, `options` (List), `correctOptionIndex`, `conceptualExplanation`.

### Redis Caching Status
- Spring Boot main class has `@EnableCaching`.
- **Current Gap**: Redis host and port configuration properties exist in `application.yml`, but `@Cacheable` and `@CacheEvict` annotations are not yet placed on service methods.

---

## Authentication & Authorization Analysis

### Current Implementation State
- **User Registration**: Accepts user registration and stores record in MongoDB. Passwords are saved as unhashed plain text.
- **User Login**: Validates password matching. Generates a static mock string token appended with user ID (`eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mockTokenForEduPilot.<userId>`).
- **Authorization**: `SecurityConfig` sets `anyRequest().permitAll()`. All API endpoints are publicly accessible without checking bearer tokens.
- **Client Access Control**: Role-based navigation is enforced client-side inside `Layout.tsx` by filtering sidebar navigation tabs based on user selection.

---

## API Documentation & Flow

```
[ Frontend Client ] ──(1) POST /api/auth/login ────────► [ Backend AuthController ]
                    ◄──(2) Token & User Profile ───────┘

[ Frontend Client ] ──(3) POST /api/students/lifestyle ──► [ Backend StudentController ]
                                                                     │
                                                               (4) Save Log to Mongo
                                                                     │
                                                                     ▼
[ FastAPI AI ] ◄─────(5) POST /api/ai/predict-performance ── [ StudentService ]
               ──────(6) SGI & Risk Predictions ─────────►
```

### Complete Endpoint Documentation Table

| Endpoint | Method | Component | Auth Required | Purpose |
|---|---|---|---|---|
| `/api/auth/register` | `POST` | Backend | Public | Register new user account |
| `/api/auth/login` | `POST` | Backend | Public | Authenticate user & issue token |
| `/api/students/onboard` | `POST` | Backend | Public | Initialize student academic & lifestyle profile |
| `/api/students/profile/{userId}` | `GET` | Backend | Public | Retrieve student profile details |
| `/api/students/lifestyle/{profileId}` | `POST` | Backend | Public | Submit daily lifestyle metric log |
| `/api/students/questionnaire/{profileId}` | `POST` | Backend | Public | Submit 20-feature diagnostic questionnaire |
| `/api/students/questionnaire/{profileId}` | `GET` | Backend | Public | Fetch questionnaire response |
| `/api/students/recommendations/{profileId}` | `GET` | Backend | Public | Fetch AI copilot recommendations |
| `/api/students/health` | `GET` | Backend | Public | Backend service health check |
| `/api/quizzes/questions` | `GET` | Backend | Public | Fetch questions by subject & difficulty |
| `/api/quizzes/submit` | `POST` | Backend | Public | Submit quiz response & calculate adaptation |
| `/api/quizzes` | `POST` | Backend | Public | Author new quiz question |
| `/api/ai/predict-performance` | `POST` | AI Service | Internal | Calculate SGI & predicted CGPA |
| `/api/ai/predict-student-development`| `POST` | AI Service | Internal | Run Random Forest inference on questionnaire |
| `/api/ai/adaptive-quiz` | `POST` | AI Service | Internal | Compute difficulty scaling logic |

---

## Docker & Deployment Analysis

### Service Ports & Container Map

| Service Name | Container Name | Base Image | Exposed Port | Dependencies |
|---|---|---|---|---|
| `mongodb` | `edupilot-mongodb` | `mongo:latest` | `27017:27017` | None |
| `redis` | `edupilot-redis` | `redis:alpine` | `6379:6379` | None |
| `ai-service` | `edupilot-ai` | `python:3.10-slim` | `8000:8000` | `mongodb` |
| `backend-api` | `edupilot-backend` | `eclipse-temurin:17` | `8080:8080` | `mongodb`, `redis`, `ai-service` |
| `frontend` | `edupilot-frontend` | `node:20-alpine` | `3000:3000` | `backend-api` |

### Environment Variables
- `ai-service`: `MONGO_URI=mongodb://mongodb:27017/edupilot`
- `backend-api`: `SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/edupilot`, `SPRING_DATA_REDIS_HOST=redis`, `SPRING_DATA_REDIS_PORT=6379`, `AI_SERVICE_URL=http://ai-service:8000`
- `frontend`: `VITE_API_URL=http://backend-api:8080`

---

## Feature Inventory

| Feature | Category | Status | Completion % | Notes |
|---|---|---|---|---|
| React 19 + Vite Frontend | Frontend | Completed | 100% | Migrated from Next.js with zero errors |
| Student Dashboard UI | Frontend | Completed | 95% | Radar charts, SGI gauge & lifestyle loggers working |
| Faculty Dashboard UI | Frontend | Completed | 90% | Student risk overview & intervention tools |
| Admin Dashboard UI | Frontend | Completed | 85% | Cluster node monitors & system controls |
| Pomodoro Timer with Audio | Frontend | Completed | 95% | Oscillator Web Audio Lofi beat generator |
| Career Skill Gap Analyzer | Frontend | Completed | 90% | Role target matching & cert recommendations |
| Adaptive Quiz Engine UI | Frontend | Completed | 90% | Real-time timers & score adjustments |
| Onboarding Wizard UI | Frontend | Completed | 95% | 5-step diagnostic data collection |
| Dual-mode API Fallback | Frontend | Completed | 100% | Seamless LocalStorage fallback if offline |
| MongoDB Entity Models | Backend | Completed | 100% | User, Profile, Lifestyle, Question entities |
| Data Seeder | Backend | Completed | 100% | Automatic boot seeding for users & quiz bank |
| REST Endpoints | Backend | Completed | 85% | Auth, Profile, Lifestyle & Quiz APIs active |
| AI Service Relay | Backend | Completed | 85% | RestTemplate client communication active |
| FastAPI Web Microservice | AI | Completed | 90% | Pydantic routes for performance & adaptive quiz |
| Random Forest ML Model | AI/ML | Completed | 85% | 300-tree model trained on 6,400+ samples |
| SGI Mathematical Formula | AI/ML | Completed | 95% | Multi-factor weighted index calculation |
| Fallback Diagnostic Rules | AI/ML | Completed | 100% | Safe rule-based calculation if ML offline |
| Docker Compose Environment | Infra | Completed | 90% | 5 containers orchestrated with network links |
| JWT Bearer Token Security | Security | Partial | 30% | Mock token issued, HTTP filter chain unconfigured |
| Password Encryption | Security | Missing | 0% | Plain text stored in database |
| Redis Query Caching | Backend | Partial | 25% | Redis active in compose, `@Cacheable` unplaced |
| Automated Test Suite | Testing | Missing | 10% | Basic spring boot context test only |

---

## Code Quality Audit

### Strengths
1. **Clean Decoupled Architecture**: Clear separation of concerns between presentation, orchestration, and ML computation layers.
2. **Robust Fallback Resilience**: Highly resilient dual-mode design ensures the UI remains fully functional even if backend or AI microservices are offline.
3. **Modern Styling Engine**: Custom glassmorphism, responsive Tailwind utility design, dark/light theme tokens, and dynamic CSS variables.

### Technical Debt & Weaknesses
1. **Security Vulnerabilities**: Plain text password storage and hardcoded mock JWT tokens.
2. **Synchronous REST Latency**: `QuizController` makes a blocking HTTP request via `RestTemplate` to the AI service during quiz answer submissions.
3. **Main JS Chunk Bundle Size**: `index-DPNQAbFa.js` is ~760 kB. Needs dynamic `React.lazy()` route-based code splitting for Recharts and Framer Motion.

---

## Performance Audit

### Current Bottlenecks
- **Frontend Initial Load**: Single large JS bundle chunk containing all route components and heavy chart libraries.
- **Backend Latency**: Synchronous RestTemplate calls to FastAPI add network overhead to HTTP requests.
- **Database Query Overhead**: Frequent profile queries without explicit MongoDB field indexing on secondary fields like `userId`.

### Optimization Opportunities
1. **Route Code Splitting**: Wrap routes in `App.tsx` with React `lazy()` and `Suspense`.
2. **Redis Integration**: Apply `@Cacheable(value = "quizQuestions")` on `QuizQuestionRepository`.
3. **Async Predictive Execution**: Annotate background analytics update calls with `@Async` in Spring Boot.

---

## Security Audit

### Critical Findings
> [!CAUTION]
> **Plain Text Passwords**: `AuthController.java` stores user passwords directly in MongoDB without hashing (`BCryptPasswordEncoder`).

> [!WARNING]
> **Mock JWT Implementation**: Authentication issues a static mock string token (`eyJhbGci...`). No JWT validation filter exists in Spring Security chain.

> [!IMPORTANT]
> **Permissive CORS & Open Routes**: `SecurityConfig.java` permits all requests (`anyRequest().permitAll()`) and allows CORS from `*`.

---

## Risks

### Current Project Risks
- **Security Vulnerability Risk**: Authentication bypass and password exposure if deployed publicly in current state.
- **AI Latency Risk**: Downtime or slowness in the Python service will delay Spring Boot responses if timeouts are not managed.

### Future Operational Risks
- **Model Drift**: ML model requires periodic retraining as student study habits evolve over time.
- **Dataset Bias**: Quantile split on `Exam_Score` depends on specific distribution properties of `StudentPerformanceFactors.csv`.

---

## Missing Features Categorized

### Critical (Must Fix Before Production)
1. Hash passwords using `BCryptPasswordEncoder`.
2. Implement real JWT token generation and validation filter in Spring Security.
3. Secure REST API endpoints using role-based authority checks (`@PreAuthorize`).

### High Priority
1. Implement route-based dynamic lazy loading on frontend (`React.lazy`).
2. Add Redis caching annotations (`@Cacheable`) for quiz questions and student profiles.
3. Execute background AI predictions asynchronously (`@Async`).

### Medium Priority
1. Expand quiz question bank across additional subject domains.
2. Add automated unit test suites for Spring Boot controllers and FastAPI endpoints.
3. Add MongoDB field indexes on `userId` and `studentProfileId`.

### Low Priority
1. Build export functionality for PDF student growth reports.
2. Integrate WebSockets for real-time faculty notifications.

---

## Recommended Development Order

```
┌────────────────────────────────────────────────────────┐
│ Phase 1: Security & Authentication Hardening           │
│ - BCrypt password hashing                             │
│ - JWT filter chain implementation                      │
│ - Endpoint role authorization                          │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ Phase 2: Performance & Caching Optimization            │
│ - Frontend route lazy-loading                          │
│ - Redis caching for questions and profiles             │
│ - Async Spring background tasks                        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ Phase 3: Domain & Analytics Expansion                  │
│ - Subject question bank expansion                      │
│ - MongoDB indexing                                     │
│ - Unit & Integration test suite creation               │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ Phase 4: Production Deployment & CI/CD                 │
│ - Multi-stage Nginx frontend build                     │
│ - Automated GitHub Actions pipeline                    │
└────────────────────────────────────────────────────────┘
```

---

## Overall Completion Breakdown

| Domain | Completion % | Status |
|---|---|---|
| Frontend | 90% | Highly Functional |
| Backend API | 80% | Functional / Needs Security |
| AI Service | 85% | Operational with ML & Fallbacks |
| Database Layer | 75% | Schema Active / Needs Indexing & Redis Caching |
| Docker & Deployment | 90% | Fully Orchestrated |
| Security | 30% | Requires Security Hardening |
| Testing | 10% | Needs Unit & Integration Tests |
| Documentation | 95% | Comprehensive |
| **Overall Project** | **78%** | **Ready for Security & Feature Refinement** |

---

## Final Assessment

### Current Project State
EduPilot AI is a well-structured, modern, polyglot software application. The frontend UI transition to React 19 + Vite is complete, visually appealing, and highly responsive. The core analytics pipeline—spanning Spring Boot, FastAPI, and Scikit-Learn—is functional with built-in fallbacks.

### Continuation Readiness
**YES**. The project is in a prime position for continued development. The architectural foundations are established, and system components are cleanly isolated.

### Next 5 Highest-Impact Tasks
1. **Enforce BCrypt Password Hashing**: Update `AuthController.java` to hash passwords before saving.
2. **Implement Real JWT Authentication**: Add `JwtTokenProvider` and `JwtAuthenticationFilter` to Spring Security.
3. **Enable Redis Caching**: Annotate `QuizQuestionRepository` methods with `@Cacheable`.
4. **Implement Frontend Code Splitting**: Update `App.tsx` routes with `React.lazy()` to reduce JS bundle size.
5. **Add Async Prediction Handling**: Annotate `StudentService.runSilentBackgroundPrediction` with `@Async` to unblock HTTP response threads.
