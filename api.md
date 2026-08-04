# EduPilot AI - API Reference Documentation

## Student & Onboarding APIs

### 1. Check Onboarding Status (Source of Truth)
- **Endpoint**: `GET /api/students/onboarding-status/{userId}`
- **Response**:
```json
{
  "userId": "65b...",
  "isCompleted": false,
  "onboardingStatus": {
    "personalCompleted": true,
    "academicCompleted": false,
    "questionnaireCompleted": false,
    "completionPercentage": 33,
    "currentStep": 2,
    "lastSavedAt": "2026-08-04T14:00:00"
  }
}
```

### 2. Save Onboarding Step (Auto-Save)
- **Endpoint**: `POST /api/students/onboard/step`
- **Payload**:
```json
{
  "userId": "65b...",
  "step": 1,
  "data": {
    "fullName": "Alex Wright",
    "collegeName": "Institute of Technology"
  }
}
```

### 3. Complete Onboarding
- **Endpoint**: `POST /api/students/onboard`
- **Description**: Marks onboarding complete, persists MongoDB collections, and executes FastAPI Random Forest ML prediction.

### 4. Fetch Full Profile
- **Endpoint**: `GET /api/students/profile-full/{userId}`

### 5. Update Profile & Recalculate AI
- **Endpoint**: `PUT /api/students/profile/{userId}`
- **Description**: Updates profile details in MongoDB and re-executes AI SGI & predicted CGPA recalculation.
