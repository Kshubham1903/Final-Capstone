import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { 
  GraduationCap, 
  Lightbulb, 
  Sparkles, 
  ExternalLink 
} from "lucide-react";
import Layout from "../../components/Layout";
import { StudentProfile } from "../../services/mockData";
import { fetchProfile, getRecommendations, checkBackendConnection } from "../../services/api";

// Reusable Modular Dashboard Components
import DashboardHeader from "../../components/dashboard/DashboardHeader";
import WelcomeCard from "../../components/dashboard/WelcomeCard";
import TodaysLearningCard from "../../components/dashboard/TodaysLearningCard";
import LearningProgressCard from "../../components/dashboard/LearningProgressCard";

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

        {/* 3. Hero Action CTA Banner */}
        <div className="glass-panel p-6 rounded-2xl border border-purple-500/20 bg-gradient-to-r from-purple-500/10 via-transparent to-pink-500/10 flex flex-col md:flex-row md:items-center justify-between gap-5">
          <div className="space-y-1">
            <h3 className="text-base font-extrabold text-main-theme">What should you focus on next?</h3>
            <p className="text-xs text-secondary-theme leading-relaxed">
              Step into your adaptive quiz session or consult your explanation tutor to log student growth index (SGI) metrics.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link 
              to="/dashboard/quizzes" 
              className="px-5 py-3 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 text-white font-extrabold text-xs tracking-wider shadow-lg shadow-purple-500/20 hover:scale-105 transition-all cursor-pointer flex items-center justify-center"
            >
              🎯 Start Adaptive Quiz
            </Link>
            <Link 
              to="/dashboard/ai-tutor" 
              className="px-5 py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-main-theme font-bold text-xs tracking-wider transition-all cursor-pointer flex items-center justify-center"
            >
              🤖 Consult AI Tutor
            </Link>
          </div>
        </div>

        {/* 4. Two-Column Workspace Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Left Column (2/3 width) - Focus areas & Mastery progress */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Today's Learning Focus (Primary Highlight) */}
            <TodaysLearningCard profile={profile} />

            {/* Subject Mastery Progress chart */}
            <LearningProgressCard profile={profile} />

          </div>

          {/* Right Column (1/3 width) - Compact academic status & top insights */}
          <div className="space-y-6">
            
            {/* Compact Academic Profile Card */}
            <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-4 bg-gradient-to-br from-purple-900/5 to-pink-900/5">
              <div className="flex items-center justify-between border-b border-white/5 pb-3">
                <div className="flex items-center gap-2">
                  <GraduationCap className="h-5 w-5 text-purple-theme" />
                  <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">Academic Summary</h4>
                </div>
                <Link 
                  to="/dashboard/profile" 
                  className="text-[10px] font-bold text-purple-theme hover:text-purple-300 flex items-center gap-1"
                >
                  <span>Edit Profile</span>
                  <ExternalLink className="h-3 w-3" />
                </Link>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="glass-panel p-3.5 rounded-xl border border-white/5 space-y-0.5">
                  <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Current CGPA</span>
                  <div className="flex items-baseline gap-1">
                    <span className="text-xl font-black text-main-theme">{(profile?.currentCgpa ?? 8.0).toFixed(1)}</span>
                    <span className="text-[9px] text-secondary-theme">/10.0</span>
                  </div>
                </div>

                <div className="glass-panel p-3.5 rounded-xl border border-white/5 space-y-0.5">
                  <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Target CGPA</span>
                  <span className="text-xl font-black text-emerald-theme">{(profile?.targetCgpa ?? 8.5).toFixed(1)}</span>
                </div>

                <div className="glass-panel p-3.5 rounded-xl border border-white/5 space-y-0.5">
                  <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Growth SGI</span>
                  <span className="text-xl font-black text-cyan-theme">{(profile?.studentGrowthIndex ?? 0.0).toFixed(1)}</span>
                </div>

                <div className="glass-panel p-3.5 rounded-xl border border-white/5 space-y-0.5">
                  <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Performance</span>
                  <span className="text-xs font-black text-amber-theme block mt-1 uppercase truncate">Tier: {predPerformanceLevel}</span>
                </div>
              </div>
            </div>

            {/* Key AI Insights Card */}
            <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-4">
              <div className="flex items-center gap-2 border-b border-white/5 pb-3">
                <Lightbulb className="h-5 w-5 text-cyan-theme" />
                <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">Key AI Insights</h4>
              </div>

              <div className="space-y-3">
                {aiInsights && aiInsights.length > 0 ? (
                  aiInsights.slice(0, 3).map((insight, idx) => (
                    <div 
                      key={idx} 
                      className="p-3.5 rounded-xl bg-white/5 border border-white/5 hover:border-purple-500/20 transition-all flex items-start gap-2.5 group animate-fade-in"
                    >
                      <div className="h-6 w-6 rounded-lg bg-purple-500/10 flex items-center justify-center mt-0.5 shrink-0 group-hover:bg-purple-500/20 transition-colors">
                        <Sparkles className="h-3 w-3 text-purple-theme" />
                      </div>
                      <p className="text-[11px] text-main-theme leading-relaxed font-medium">{insight}</p>
                    </div>
                  ))
                ) : (
                  <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center space-y-1.5">
                    <Sparkles className="h-5 w-5 text-purple-theme/50 mx-auto animate-pulse" />
                    <p className="text-[10px] text-secondary-theme">Generating customized insights from AI Engine...</p>
                  </div>
                )}
              </div>
            </div>

          </div>

        </div>

      </div>
    </Layout>
  );
}
