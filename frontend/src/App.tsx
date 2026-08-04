import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";

import Home from "./app/page";
import Onboarding from "./app/onboarding/page";
import StudentDashboard from "./app/dashboard/page";
import ProfilePage from "./app/dashboard/profile/page";
import CareerCenter from "./app/dashboard/career/page";
import Pomodoro from "./app/dashboard/pomodoro/page";
import Quizzes from "./app/dashboard/quizzes/page";
import FacultyDashboard from "./app/faculty/page";
import QuizManagerDashboard from "./app/faculty/quiz-manager/page";
import AdminDashboard from "./app/admin/page";
import OnboardingGuard from "./components/OnboardingGuard";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/onboarding" element={<Onboarding />} />
      <Route path="/dashboard" element={<OnboardingGuard><StudentDashboard /></OnboardingGuard>} />
      <Route path="/dashboard/profile" element={<OnboardingGuard><ProfilePage /></OnboardingGuard>} />
      <Route path="/dashboard/career" element={<OnboardingGuard><CareerCenter /></OnboardingGuard>} />
      <Route path="/dashboard/pomodoro" element={<OnboardingGuard><Pomodoro /></OnboardingGuard>} />
      <Route path="/dashboard/quizzes" element={<OnboardingGuard><Quizzes /></OnboardingGuard>} />
      <Route path="/faculty" element={<FacultyDashboard />} />
      <Route path="/faculty/quiz-manager" element={<QuizManagerDashboard />} />
      <Route path="/admin" element={<AdminDashboard />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
