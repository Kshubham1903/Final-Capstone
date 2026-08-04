# Frontend Architecture Documentation (React 19 + Vite)

## Overview & Route Protection
The frontend is built with React 19, Vite, Tailwind CSS v4, Framer Motion, and React Router DOM v7.
All protected dashboard routes (`/dashboard`, `/dashboard/profile`, `/dashboard/career`, `/dashboard/pomodoro`, `/dashboard/quizzes`) are wrapped with `<OnboardingGuard>`.

## Onboarding Architecture
- **7-Step Paginated Wizard** (`/onboarding`):
  - Step 1: Personal & Demographic Information
  - Step 2: Academic Profile & Engineering Targets
  - Step 3: Lifestyle Assessment - Academic Habits
  - Step 4: Lifestyle Assessment - Study Environment
  - Step 5: Lifestyle Assessment - Sleep & Physical Wellness
  - Step 6: Lifestyle Assessment - Motivation & Social Circle
  - Step 7: Final Demographic Baseline & AI Launch
- **UX Features**:
  - Auto-saves step progress to MongoDB via `saveOnboardingStep()`.
  - Resumes interrupted onboarding at `currentStep`.
  - Top progress bar, step counter, estimated time remaining indicator.
  - Smooth Framer Motion step transitions.

## Profile Management Module (`/dashboard/profile`)
- Allows students to view and edit personal, academic, and lifestyle parameters.
- Saving edits triggers real-time Spring Boot → FastAPI AI recalculation, updating SGI and predicted CGPA live on the dashboard.
