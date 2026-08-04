# Onboarding HTTP 400 Bad Request Root Cause & Debug Report

## Executive Summary

- **Status**: **IDENTIFIED & RESOLVED**
- **Primary Issue**: Spring Boot backend rejected onboarding requests with `HTTP 400 Bad Request`.
- **Root Cause**:
  1. **Path Variable ID Lookup Mismatch**: Endpoints `/api/students/questionnaire/{profileId}` and `/api/students/lifestyle/{profileId}` were performing `profileRepository.findById(profileId)` (MongoDB document ID) instead of `findByUserId(userId)`. Since the frontend passes `userId` (the authenticated user's ID) before profile creation, `findById()` returned `Optional.empty()`, throwing a `RuntimeException("Student Profile not found!")` which triggered an HTTP 400 response.
  2. **Auto-Save Payload Type Casting & Uninitialized Profile Handling**: `saveOnboardingStep` and `onboardStudent` attempted unsafe type casting `(List<String>)` or `((Number) val).intValue()` on raw JSON maps. Missing or null payload attributes triggered `NullPointerException` / `ClassCastException`, caught as `ResponseEntity.badRequest()`.

---

## Technical Inspection Details

### 1. Failed Endpoint(s)
- `POST /api/students/questionnaire/{profileId}` -> `HTTP 400 Bad Request`
- `POST /api/students/onboard/step` -> `HTTP 400 Bad Request`

### 2. Request Details & Validation Failure
- **Endpoint**: `POST /api/students/questionnaire/65b...`
- **Request Payload**:
```json
{
  "studentProfileId": "65b...",
  "hoursStudied": 20,
  "attendance": 90,
  "parentalInvolvement": "Medium",
  "accessToResources": "High",
  "extracurricularActivities": "Yes",
  "sleepHours": 7.5,
  "previousScores": 85,
  "motivationLevel": "High",
  "internetAccess": "Yes",
  "tutoringSessions": 1,
  "familyIncome": "Medium",
  "teacherQuality": "Medium",
  "schoolType": "Public",
  "peerInfluence": "Positive",
  "physicalActivity": 3.5,
  "learningDisabilities": "No",
  "parentalEducationLevel": "College",
  "distanceFromHome": "Near",
  "gender": "Male"
}
```
- **Backend Log Trace**:
```
java.lang.RuntimeException: Student Profile not found!
	at com.edupilot.service.StudentService.submitQuestionnaireAndRunAnalytics(StudentService.java:275)
	at com.edupilot.controller.StudentController.submitQuestionnaire(StudentController.java:122)
```

---

## Fix Implementation Strategy

1. **Defensive Dual-Lookup Helper (`findOrCreateProfile`)**:
   - Updated `StudentService.java` to look up `StudentProfile` first by document primary key `findById()`, and if empty, by `findByUserId()`.
   - If profile still does not exist, automatically initialize and persist a new `StudentProfile` linked to `userId`.

2. **Safe Defensive Type Parsing Helpers**:
   - Implemented `parseDouble(Object val, double defaultVal)`, `parseInt(Object val, int defaultVal)`, and `parseList(Object val)` in `StudentService.java`.
   - Guaranteed that null or missing fields in auto-save JSON payloads never trigger `NullPointerException` or `ClassCastException`.

3. **19 ML Parameter Alignment**:
   - Verified that all 19 ML parameters (`Hours_Studied`, `Attendance`, `Previous_Scores`, `Tutoring_Sessions`, `Access_to_Resources`, `Internet_Access`, `School_Type`, `Teacher_Quality`, `Sleep_Hours`, `Physical_Activity`, `Extracurricular_Activities`, `Motivation_Level`, `Parental_Involvement`, `Family_Income`, `Peer_Influence`, `Parental_Education_Level`, `Distance_from_Home`, `Learning_Disabilities`, `Gender`) are safely parsed and preserved.

---

## Files Modified

- `backend/src/main/java/com/edupilot/service/StudentService.java`
- `backend/src/main/java/com/edupilot/controller/StudentController.java`

---

## Verification After Fix

- **Backend Maven Build**: `BUILD SUCCESS` (0 compilation errors).
- **HTTP Response Verification**: `POST /api/students/questionnaire/{userId}` now returns `200 OK` with updated profile.
- **Onboarding Flow**: Complete onboarding step submission succeeds with zero HTTP 400 errors.
