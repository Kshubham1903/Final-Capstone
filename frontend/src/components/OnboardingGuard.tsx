import React, { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { fetchOnboardingStatus } from "../services/api";

export default function OnboardingGuard({ children }: { children: React.ReactNode }) {
  const [checking, setChecking] = useState(true);
  const [isCompleted, setIsCompleted] = useState(false);
  const location = useLocation();

  useEffect(() => {
    async function verifyStatus() {
      const userId = localStorage.getItem("edupilot_user_id") || "";
      if (!userId) {
        // If not logged in, allow home page auth or let router handle
        setChecking(false);
        setIsCompleted(false);
        return;
      }

      const status = await fetchOnboardingStatus(userId);
      setIsCompleted(Boolean(status && status.isCompleted));
      setChecking(false);
    }

    verifyStatus();
  }, [location.pathname]);

  if (checking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#05060b]">
        <div className="flex flex-col items-center gap-3">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-purple-500" />
          <span className="text-xs font-bold text-secondary-theme uppercase tracking-wider">
            Verifying Student Onboarding Status...
          </span>
        </div>
      </div>
    );
  }

  // If student onboarding is incomplete and user tries to access protected dashboard routes, redirect to onboarding
  const userId = localStorage.getItem("edupilot_user_id") || "";
  const role = localStorage.getItem("edupilot_role") || "STUDENT";

  if (userId && role === "STUDENT" && !isCompleted && location.pathname !== "/onboarding") {
    return <Navigate to="/onboarding" replace />;
  }

  return <>{children}</>;
}
