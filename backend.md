# Backend Architecture & Onboarding Verification Documentation

## Executive Summary
The Spring Boot backend acts as the sole source of truth for student onboarding completion and user profile data validation.

## Data Models & Collections
1. **`User`** (`users`): Authentication details & roles.
2. **`PersonalInformation`** (`personal_information`): Full demographics, college, PRN, branch, semester, graduation year.
3. **`AcademicProfile`** (`academic_profiles`): Extensible academic parameters (Engineering branch default, extendable to Medical/Commerce/Arts/Law/MBA), CGPA goals, languages, frameworks, portfolio links.
4. **`LifestyleQuestionnaire`** (`lifestyle_questionnaires`): Stores 19 exact ML input features required by the FastAPI microservice (`Hours_Studied`, `Attendance`, `Sleep_Hours`, `Parental_Involvement`, etc.).
5. **`OnboardingStatus`** (`onboarding_statuses`): Step-by-step progress tracker (`personalCompleted`, `academicCompleted`, `questionnaireCompleted`, `completionPercentage`, `currentStep`, `lastSavedAt`).
6. **`StudentProfile`** (`student_profiles`): Master linking entity storing SGI (0.0 - 10.0), predicted CGPA, academic risk level ("LOW", "MEDIUM", "HIGH"), consistency/productivity scores, mastery map, and streak metrics.

## REST Endpoints
- `GET /api/students/onboarding-status/{userId}`: Source of truth check for route guard.
- `POST /api/students/onboard/step`: Auto-saves partial step progress to MongoDB.
- `POST /api/students/onboard`: Validates completion, marks profile active, and triggers FastAPI AI recalculation.
- `GET /api/students/profile-full/{userId}`: Returns aggregated student profile object.
- `PUT /api/students/profile/{userId}`: Updates profile & re-runs FastAPI prediction pipeline in real time.
