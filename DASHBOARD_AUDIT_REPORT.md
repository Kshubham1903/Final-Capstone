# EduPilot AI - Comprehensive Student Dashboard Audit Report

## Executive Summary

- **Audit Date**: August 4, 2026
- **Auditor**: Principal Product Architect & AI Systems Engineer
- **Audit Target**: EduPilot AI Student Dashboard (`frontend/src/app/dashboard/page.tsx`)
- **Core Principle Evaluated**: 100% data-driven platform integrity (No hardcoded values, no fabricated analytics, no fake recommendations, no generic mock placeholders).

### Audit Summary Rating: **NEEDS STRUCTURAL & DATA-FLOW REFINEMENT**
While the backend seeding has been cleaned and onboarding APIs are active, the Student Dashboard still relies on client-side mock fallbacks (`mockData.ts`) for lifestyle history and unlocked badges when MongoDB records are empty. Furthermore, initial concept mastery mapping uses generic placeholder strings (`"Core Concepts"`, `"Fundamentals"`) instead of dynamic concepts originating from the student's enrolled Engineering Branch, Semester, and Selected Subjects.

---

## 1. Component-by-Component Audit Review

### Component 1: Welcome Header & Goal Banner
- **Purpose**: Welcomes the authenticated student and displays their active enrolled program/semester.
- **Current Data Source**: `localStorage.getItem("edupilot_user")` and `profile.course` / `profile.semester`.
- **Backend API**: `GET /api/students/profile/{userId}`
- **MongoDB Collection**: `student_profiles` & `users`
- **AI Dependency**: None.
- **Business Value**: High (Personalization & session context).
- **User Value**: High (Confirms active user identity and active program).
- **Audit Verdict**: **KEEP & RETAIN**. Clean dynamic binding.

---

### Component 2: Student Growth Index (SGI) Ring & Metric Card
- **Purpose**: Displays the primary AI-calculated academic growth index score ($0.0 - 10.0$), goal CGPA, and predicted CGPA.
- **Current Data Source**: `profile.studentGrowthIndex`, `profile.targetCgpa`, `profile.predictedCgpa`.
- **Backend API**: `GET /api/students/profile/{userId}` & `GET /api/students/recommendations/{profileId}`
- **MongoDB Collection**: `student_profiles`
- **AI Dependency**: High (Calculated by Python FastAPI Random Forest microservice & SGI mathematical formula).
- **Business Value**: Critical (Core value proposition of EduPilot AI).
- **User Value**: Critical (Single unified score indicating overall growth velocity).
- **Audit Verdict**: **KEEP & RETAIN**. Ensure clear tooltip explaining SGI formula metrics.

---

### Component 3: Academic Risk Level Badge & Performance Level Card
- **Purpose**: Displays diagnostic academic risk classification (`LOW`, `MEDIUM`, `HIGH`) and predicted performance level.
- **Current Data Source**: `profile.academicRiskLevel` and `predPerformanceLevel` state.
- **Backend API**: `GET /api/students/recommendations/{profileId}`
- **MongoDB Collection**: `student_profiles`
- **AI Dependency**: High (Random Forest classification based on 19 ML lifestyle & academic features).
- **Business Value**: Critical (Early warning system for academic burnout or failure risk).
- **User Value**: Critical (Actionable diagnostic status).
- **Audit Verdict**: **KEEP & RETAIN**. Add explicit recommendations trigger link when risk is `MEDIUM` or `HIGH`.

---

### Component 4: Productivity Index & Lifestyle Score Card
- **Purpose**: Displays habit productivity score (0-100) and lifestyle score.
- **Current Data Source**: `profile.productivityScore` and `profile.lifestyleScore`.
- **Backend API**: `GET /api/students/profile/{userId}`
- **MongoDB Collection**: `student_profiles`
- **AI Dependency**: Medium (Derived from daily lifestyle logs & tutoring frequency).
- **Business Value**: Medium.
- **User Value**: High (Visualizes daily study habits and wellness balance).
- **Audit Verdict**: **RETAIN**. Needs explicit zero state indicator when 0 logs have been recorded.

---

### Component 5: Target Focus & Milestone Card
- **Purpose**: Displays active career goal and quiz completion count.
- **Current Data Source**: `profile.careerGoals[0]` and `profile.completedQuizzesCount`.
- **Backend API**: `GET /api/students/profile/{userId}`
- **MongoDB Collection**: `student_profiles` & `academic_profiles`
- **AI Dependency**: Low.
- **Business Value**: Medium.
- **User Value**: Medium.
- **Audit Verdict**: **REDESIGN**. Currently displays only 1 line of text. Should be upgraded to show Target Career Role, Dream Company, and target target CGPA progress.

---

### Component 6: Interactive Analytics Chart (Lifestyle Logs vs Subject Mastery)
- **Purpose**: Visualizes historical sleep/productivity trends and subject mastery percentages.
- **Current Data Source**: `profile.lifestyleHistory` with fallback to `getStoredStudentProfile().lifestyleHistory` in `mockData.ts`!
- **Backend API**: `GET /api/students/profile/{userId}` & `lifestyle_data` collection queries.
- **MongoDB Collection**: `lifestyle_data` & `student_profiles`
- **AI Dependency**: Medium.
- **Business Value**: High (Longitudinal tracking).
- **User Value**: High (Visual progress tracking).
- **Audit Verdict**: **CRITICAL FIX REQUIRED**.
  - **Violation Found**: Line 94 in `page.tsx` uses `profile?.lifestyleHistory || getStoredStudentProfile().lifestyleHistory`. This causes fake sample chart data to display when no daily logs exist in MongoDB!
  - **Required Action**: Remove `getStoredStudentProfile().lifestyleHistory` fallback. If `lifestyleHistory` is empty (`[]`), render an explicit Empty State ("No habit logs recorded yet. Click 'Log Habits' to add your first entry!").

---

### Component 7: Daily AI Engine Guides (AI Recommendations Card)
- **Purpose**: Renders actionable AI copilot insights and study recommendations generated by the FastAPI microservice.
- **Current Data Source**: `aiInsights` state from `getRecommendations(profileId)`.
- **Backend API**: `GET /api/students/recommendations/{profileId}`
- **MongoDB Collection**: `student_profiles` & `lifestyle_questionnaires`
- **AI Dependency**: Critical (FastAPI microservice).
- **Business Value**: Critical.
- **User Value**: Critical.
- **Audit Verdict**: **REDESIGN EMPTY STATE**. When `aiInsights` is empty array `[]` (e.g. backend server starting or new profile), the card renders a blank box. Must display an elegant Empty State explaining how to generate insights.

---

### Component 8: Conceptual Mastery Mapping Card (Strong & Weak Concepts)
- **Purpose**: Displays specific concept mastery strengths and weaknesses per subject.
- **Current Data Source**: `profile.strongConcepts` and `profile.weakConcepts`.
- **Backend API**: `GET /api/students/profile/{userId}`
- **MongoDB Collection**: `student_profiles` & `quiz_questions`
- **AI Dependency**: High (Updated dynamically by `POST /api/quizzes/submit` based on adaptive quiz performance).
- **Business Value**: Critical (Targeted learning intervention).
- **User Value**: Critical (Tells student exactly what topics to study).
- **Audit Verdict**: **CRITICAL FIX REQUIRED**.
  - **Violation Found**: Initial baseline in `StudentService.java` populates `weakConcepts` with `"Core Concepts"` and `strongConcepts` with `"Fundamentals"`. These generic strings appear regardless of selected branch or subject.
  - **Required Action**: Concept names must be strictly derived from the student's branch, semester, and enrolled subjects (e.g. Computer Science → Sem 5 → *Data Structures & Algorithms* → *Binary Search Tree*, *Dynamic Programming*).

---

### Component 9: Unlocked Badges Hub
- **Purpose**: Gamification hub rewarding continuous study streaks and quiz achievements.
- **Current Data Source**: `profile.badges` with fallback to `getStoredStudentProfile().badges` in `mockData.ts`!
- **Backend API**: `GET /api/students/profile/{userId}`
- **MongoDB Collection**: `student_profiles`
- **AI Dependency**: Low.
- **Business Value**: Medium.
- **User Value**: High (Engagement & retention).
- **Audit Verdict**: **CRITICAL FIX REQUIRED**.
  - **Violation Found**: Line 95 in `page.tsx` falls back to `getStoredStudentProfile().badges`, rendering fake default badges ("Early Bird", "Consistency King") for brand new users.
  - **Required Action**: Remove `mockData.ts` fallback. Render real earned badges from MongoDB, or display a clean Empty State when 0 badges are unlocked.

---

## 2. Conceptual Mastery & Hierarchy Audit

```
Engineering (Root Stream)
 └── Branch (e.g., Computer Science & Engineering)
      └── Semester (e.g., Semester 5)
           └── Selected Subjects (e.g., Data Structures & Algorithms, DBMS, AI)
                └── Specific Syllabus Concepts (e.g., Graph Traversal, B-Trees, Backpropagation)
                     └── Quiz Performance Attempt (Correct +8% / Incorrect -3%)
```

### Key Conceptual Mapping Rules:
1. **No Generic Placeholders**: Never display `"Fundamentals"`, `"Core Concepts"`, or `"Conceptual Foundations"` unless they represent an actual syllabus topic.
2. **Subject Grouping**: Every concept displayed on the dashboard MUST explicitly display its parent subject and belong to the student's enrolled semester subjects.
3. **Adaptive Quiz Synchronization**: Concepts in the Weak list MUST automatically link to the Adaptive Quiz generator (`/dashboard/quizzes?concept=...`), allowing students to launch a targeted quiz with a single click.

---

## 3. Empty State Inventory & Data Integrity Standard

| Dashboard Component | Current Empty Behavior | Required Production Behavior |
| :--- | :--- | :--- |
| **Lifestyle Logs Chart** | Falls back to mock data (`mockData.ts`) | Display Empty State: *"No habit logs recorded yet. Click 'Log Habits' to record your first daily entry!"* |
| **Unlocked Badges** | Falls back to mock badges (`mockData.ts`) | Display Empty State: *"No badges earned yet. Complete daily study goals and quizzes to unlock achievements!"* |
| **AI Copilot Guides** | Renders blank empty container | Display Empty State: *"Your AI Copilot is analyzing your profile. Complete a quiz or habit log to generate insights."* |
| **Weak Concepts List** | Displays generic `"Core Concepts"` | Display Empty State: *"No weak concepts identified yet. Take an adaptive quiz to diagnose topic mastery!"* |

---

## 4. Information Hierarchy & User Journey Questions

The updated dashboard layout must directly answer the 6 core questions every student asks:

1. **How am I performing?** → SGI Score Ring ($0.0 - 10.0$) & Predicted CGPA.
2. **Am I at academic risk?** → Diagnostic Risk Profile Badge (`LOW`, `MEDIUM`, `HIGH`).
3. **What is my habit trend?** → Lifestyle & Productivity Area Chart.
4. **What should I study next?** → Daily AI Copilot Recommendations.
5. **What are my weak topics?** → Conceptual Mastery Mapping grouped by Subject.
6. **What action should I take right now?** → One-click triggers for "Log Habits" and "Start Adaptive Quiz".

---

## 5. Implementation Roadmap by Priority

### Priority 1: CRITICAL (Data Flow & Mock Elimination)
1. **Remove `mockData.ts` fallbacks** in `page.tsx` for `lifestyleHistory` and `badges`.
2. **Implement Explicit Empty States** for Lifestyle Chart, Unlocked Badges, AI Guides, and Weak/Strong Concepts.
3. **Fix Concept Naming**: Replace generic `"Core Concepts"` / `"Fundamentals"` baseline in `StudentService.java` with subject-specific syllabus concepts (e.g. *Data Structures* → *Arrays & Linked Lists*, *Recursion*).

### Priority 2: HIGH (Subject & Concept Hierarchy Alignment)
1. **Group Concepts by Enrolled Subject**: Ensure Weak/Strong concept cards cleanly categorize topics under the student's active semester subjects.
2. **One-Click Remediation**: Add a "Practice Concept" button next to weak concepts that navigates directly to `/dashboard/quizzes` pre-filtered for that concept.

### Priority 3: MEDIUM (UX & Visual Hierarchy Polish)
1. **Upgrade Target Focus Card**: Display target career role, dream target company, and weekly coding hours.
2. **Enhance AI Copilot Guides Card**: Add action tags and priority color coding to AI recommendations.

### Priority 4: LOW (Gamification & Badges)
1. **Dynamic Badge Engine**: Implement automated badge unlocks based on real MongoDB quiz completions and streak counts.

---

## Final Verdict & Recommendation

Do NOT apply UI code changes until this audit report is reviewed. The recommendations above will transition the EduPilot AI Student Dashboard into a 100% production-ready, data-driven interface with zero reliance on fabricated fallback data.
