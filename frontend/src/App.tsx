import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";

import Home from "./app/page";
import Onboarding from "./app/onboarding/page";
import StudentDashboard from "./app/dashboard/page";
import ProfilePage from "./app/dashboard/profile/page";
import Quizzes from "./app/dashboard/quizzes/page";
import FacultyDashboard from "./app/faculty/page";
import QuizManagerDashboard from "./app/faculty/quiz-manager/page";
import AdminDashboard from "./app/admin/page";
import SubjectAnalyticsPage from "./app/admin/subject-analytics/page";
import StudentDirectoryPage from "./app/admin/student-directory/page";
import IndividualStudentAnalyticsPage from "./app/admin/research-analytics/students/[userId]/page";
import ResearchAnalyticsPage from "./app/admin/research-analytics/page";
import AITutorPage from "./pages/AITutorPage";
import OnboardingGuard from "./components/OnboardingGuard";

import SubjectsPage from "./app/dashboard/subjects/page";

import StudentProgressPage from "./app/progress/page";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/onboarding" element={<Onboarding />} />
      <Route path="/dashboard" element={<OnboardingGuard><StudentDashboard /></OnboardingGuard>} />
      <Route path="/progress" element={<OnboardingGuard><StudentProgressPage /></OnboardingGuard>} />
      <Route path="/dashboard/progress" element={<OnboardingGuard><StudentProgressPage /></OnboardingGuard>} />
      <Route path="/dashboard/subjects" element={<OnboardingGuard><SubjectsPage /></OnboardingGuard>} />
      <Route path="/dashboard/subjects/:subjectCode" element={<OnboardingGuard><SubjectsPage /></OnboardingGuard>} />
      <Route path="/dashboard/profile" element={<OnboardingGuard><ProfilePage /></OnboardingGuard>} />
      <Route path="/dashboard/quizzes" element={<OnboardingGuard><Quizzes /></OnboardingGuard>} />
      <Route path="/dashboard/ai-tutor" element={<OnboardingGuard><AITutorPage /></OnboardingGuard>} />
      <Route path="/faculty" element={<FacultyDashboard />} />
      <Route path="/faculty/quiz-manager" element={<QuizManagerDashboard />} />
      <Route path="/admin" element={<AdminDashboard />} />
      <Route path="/admin/subject-analytics" element={<SubjectAnalyticsPage />} />
      <Route path="/admin/student-directory" element={<StudentDirectoryPage />} />
      <Route path="/admin/research-analytics" element={<ResearchAnalyticsPage />} />
      <Route path="/admin/research-analytics/students/:userId" element={<IndividualStudentAnalyticsPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
