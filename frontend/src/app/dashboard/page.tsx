import React, { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import { StudentProfile } from "../../services/mockData";
import { fetchProfile, getRecommendations, checkBackendConnection } from "../../services/api";

// Reusable Modular Dashboard Components
import DashboardHeader from "../../components/dashboard/DashboardHeader";
import WelcomeCard from "../../components/dashboard/WelcomeCard";
import AcademicProfileCard from "../../components/dashboard/AcademicProfileCard";
import AcademicCatalogCard from "../../components/dashboard/AcademicCatalogCard";
import DiagnosticAssessmentCard from "../../components/dashboard/DiagnosticAssessmentCard";
import AILearningInsightsCard from "../../components/dashboard/AILearningInsightsCard";
import TodaysLearningCard from "../../components/dashboard/TodaysLearningCard";
import QuickActionsCard from "../../components/dashboard/QuickActionsCard";
import LearningProgressCard from "../../components/dashboard/LearningProgressCard";
import KnowledgeProgressCard from "../../components/dashboard/KnowledgeProgressCard";
import RecentActivityCard from "../../components/dashboard/RecentActivityCard";
import AITutorCard from "../../components/dashboard/AITutorCard";

export default function StudentDashboard() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [aiInsights, setAiInsights] = useState<string[]>([]);
  const [predPerformanceLevel, setPredPerformanceLevel] = useState("Medium");
  const [isBackendConnected, setIsBackendConnected] = useState(false);
  const [isLoadingAI, setIsLoadingAI] = useState(true);
  const [aiError, setAiError] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoadingAI(true);
    setAiError(null);
    try {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      const activeProfile = await fetchProfile(activeUserId);
      setProfile(activeProfile);

      const conn = await checkBackendConnection();
      setIsBackendConnected(conn);

      const recsResult = await getRecommendations(activeProfile?.id || activeUserId);
      if (recsResult && recsResult.insights) {
        setAiInsights(recsResult.insights);
        if (recsResult.predicted_performance_level) {
          setPredPerformanceLevel(recsResult.predicted_performance_level);
        }
      }
    } catch (err: any) {
      console.error("Error loading AI dashboard data:", err);
      setAiError("Failed to synchronize with AI Prediction Engine. Operating on local fallback metrics.");
    } finally {
      setIsLoadingAI(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (!profile && isLoadingAI) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#05060b]">
        <div className="flex flex-col items-center gap-3">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-purple-500" />
          <span className="text-xs font-bold text-secondary-theme">Initializing AI Copilot...</span>
        </div>
      </div>
    );
  }

  const getFirstName = () => {
    let fullName = "";
    if (typeof window !== "undefined") {
      try {
        const userStr = localStorage.getItem("edupilot_user");
        if (userStr) {
          const userObj = JSON.parse(userStr);
          if (userObj && typeof userObj.fullName === "string" && userObj.fullName.trim() !== "") {
            fullName = userObj.fullName;
          }
        }
      } catch (err) {
        console.warn("Failed to parse edupilot_user from localStorage:", err);
      }
    }

    if (!fullName && profile && typeof profile.fullName === "string" && profile.fullName.trim() !== "") {
      fullName = profile.fullName;
    }

    if (!fullName) {
      fullName = "Student";
    }

    return fullName.split(" ")[0] || fullName;
  };

  const firstName = getFirstName();

  return (
    <Layout>
      <div className="space-y-6 animate-fade-in pb-8">
        
        {/* 1. Dashboard Header */}
        <DashboardHeader 
          streak={profile?.currentStreakCount ?? 0} 
          isBackendConnected={isBackendConnected} 
        />

        {/* 2. Welcome Section */}
        <WelcomeCard 
          firstName={firstName} 
          profile={profile} 
        />

        {/* 3. Academic Profile Identity Metrics */}
        <AcademicProfileCard 
          profile={profile} 
          predPerformanceLevel={predPerformanceLevel} 
        />

        {/* 4. Unified AI Learning Insights (7 Core AI Metrics & Recommendations) */}
        <AILearningInsightsCard 
          profile={profile}
          insights={aiInsights}
          predPerformanceLevel={predPerformanceLevel}
          isLoading={isLoadingAI}
          error={aiError}
          isBackendConnected={isBackendConnected}
          onRetry={loadData}
        />

        {/* 5. Diagnostic Assessment Evaluation Banner */}
        <DiagnosticAssessmentCard 
          profile={profile} 
        />

        {/* 6. Master Academic Catalog Browser */}
        <AcademicCatalogCard 
          currentBranch={profile?.branch || profile?.course} 
          currentSemester={profile?.semester ?? 1} 
        />

        {/* Grid Section: Today's Learning, Quick Actions & AI Tutor */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column (2/3 width) */}
          <div className="lg:col-span-2 space-y-6">
            {/* Today's Learning Focus */}
            <TodaysLearningCard profile={profile} />

            {/* Subject Mastery Chart */}
            <LearningProgressCard profile={profile} />
          </div>

          {/* Right Column (1/3 width) */}
          <div className="space-y-6">
            {/* Quick Actions */}
            <QuickActionsCard />

            {/* AI Tutor (Coming Soon) */}
            <AITutorCard />
          </div>
        </div>

        {/* Grid Section: Knowledge Matrix & Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column (2/3 width) */}
          <div className="lg:col-span-2">
            {/* Conceptual Knowledge Matrix */}
            <KnowledgeProgressCard profile={profile} />
          </div>

          {/* Right Column (1/3 width) */}
          <div>
            {/* Recent Activity & Unlocked Badges */}
            <RecentActivityCard profile={profile} />
          </div>
        </div>

      </div>
    </Layout>
  );
}
