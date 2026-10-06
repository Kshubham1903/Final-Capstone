import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import {
  Settings,
  Users,
  Database,
  Cpu,
  Activity,
  RefreshCw,
  ShieldAlert,
  GraduationCap,
  TrendingUp,
  BookOpen,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  UserCheck,
  UserX
} from "lucide-react";
import { fetchAdminAnalyticsOverview, AdminAnalyticsOverview } from "../../services/api";

export default function AdminDashboard() {
  const navigate = useNavigate();

  // Overview Telemetry State
  const [analytics, setAnalytics] = useState<AdminAnalyticsOverview | null>(null);
  const [loadingAnalytics, setLoadingAnalytics] = useState(true);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);

  const loadOverview = async () => {
    setLoadingAnalytics(true);
    setAnalyticsError(null);
    try {
      const data = await fetchAdminAnalyticsOverview();
      if (data) {
        setAnalytics(data);
      } else {
        setAnalyticsError("Unable to retrieve overview metrics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err) {
      setAnalyticsError("Network error while connecting to Admin overview endpoint.");
    } finally {
      setLoadingAnalytics(false);
    }
  };

  useEffect(() => {
    loadOverview();
  }, []);

  const totalStudents = analytics?.totalStudents ?? 0;
  const activeStudents = analytics?.activeStudentsLast7Days ?? 0;
  const atRiskStudents = analytics?.atRiskStudentCount ?? 0;
  const inactiveStudents = Math.max(0, totalStudents - activeStudents - atRiskStudents);

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto pb-10">
        
        {/* Header & Quick Action Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2.5">
              <Settings className="h-8 w-8 text-purple-theme" />
              <span>Admin Control Panel</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              System health status, student enrollment metrics, and learning activity overview.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={loadOverview}
              disabled={loadingAnalytics}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
            >
              <RefreshCw className={`h-4 w-4 text-purple-theme ${loadingAnalytics ? "animate-spin" : ""}`} />
              <span>{loadingAnalytics ? "Refreshing..." : "Refresh Dashboard"}</span>
            </button>
          </div>
        </div>

        {/* Analytics Error Notification */}
        {analyticsError && (
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme text-xs flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <ShieldAlert className="h-5 w-5 shrink-0" />
              <span>{analyticsError}</span>
            </div>
            <button
              onClick={loadOverview}
              className="px-3 py-1 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs cursor-pointer"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {loadingAnalytics && !analytics && (
          <div className="space-y-6 animate-pulse">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="glass-panel p-6 rounded-2xl border border-white/5 h-28 bg-white/5" />
              ))}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-28 bg-white/5" />
              ))}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-28 bg-white/5" />
              ))}
            </div>
          </div>
        )}

        {analytics && (
          <>
            {/* SECTION 1 — SYSTEM STATUS */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Database className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">
                  System Health & Infrastructure
                </h2>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Database Status */}
                <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                    Database Status
                  </span>
                  <div className="text-xl font-bold text-emerald-400 flex items-center gap-2 pt-0.5">
                    <Database className="h-5 w-5" />
                    <span>MongoDB Online</span>
                  </div>
                  <p className="text-xs text-secondary-theme">Primary database operational and healthy.</p>
                </div>

                {/* AI Service Status */}
                <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                    AI Service Status
                  </span>
                  <div className="text-xl font-bold text-cyan-400 flex items-center gap-2 pt-0.5">
                    <Cpu className="h-5 w-5" />
                    <span>AI Engine Connected</span>
                  </div>
                  <p className="text-xs text-secondary-theme">FastAPI diagnostic and quiz services active.</p>
                </div>

                {/* Enrolled Students Overview */}
                <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                    Total Enrolled Students
                  </span>
                  <div className="text-2xl font-black text-purple-theme pt-0.5">
                    {totalStudents} Students
                  </div>
                  <p className="text-xs text-secondary-theme">Authenticated student accounts on the platform.</p>
                </div>
              </div>
            </div>

            {/* SECTION 2 — STUDENT OVERVIEW */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">
                  Student Enrollment & Status
                </h2>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Total Students */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Total Students
                    </span>
                    <Users className="h-4 w-4 text-purple-400" />
                  </div>
                  <div className="text-2xl font-black text-main-theme">
                    {totalStudents}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Total registered learners.</p>
                </div>

                {/* Active Students */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Active Students
                    </span>
                    <UserCheck className="h-4 w-4 text-emerald-400" />
                  </div>
                  <div className="text-2xl font-black text-emerald-400">
                    {activeStudents}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Active in the last 7 days.</p>
                </div>

                {/* At-Risk Students */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      At-Risk Students
                    </span>
                    <AlertTriangle className="h-4 w-4 text-amber-400" />
                  </div>
                  <div className="text-2xl font-black text-amber-400">
                    {atRiskStudents}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Inactive &gt; 7 days / low activity.</p>
                </div>

                {/* Inactive Students */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Inactive Students
                    </span>
                    <UserX className="h-4 w-4 text-rose-400" />
                  </div>
                  <div className="text-2xl font-black text-secondary-theme">
                    {inactiveStudents}
                  </div>
                  <p className="text-[10px] text-secondary-theme">No recent learning activity.</p>
                </div>
              </div>
            </div>

            {/* SECTION 3 — LEARNING OVERVIEW */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Activity className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">
                  Overall Learning Activity
                </h2>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Assessments Completed */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Assessments Completed
                    </span>
                    <BookOpen className="h-4 w-4 text-purple-400" />
                  </div>
                  <div className="text-2xl font-black text-purple-theme">
                    {analytics.totalAssessmentsCompleted}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Diagnostic & baseline evaluations.</p>
                </div>

                {/* Quizzes Completed */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Quizzes Completed
                    </span>
                    <GraduationCap className="h-4 w-4 text-cyan-400" />
                  </div>
                  <div className="text-2xl font-black text-cyan-theme">
                    {analytics.totalQuizzesCompleted}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Adaptive & verification quizzes.</p>
                </div>

                {/* Average Current Knowledge */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Average Current Knowledge
                    </span>
                    <TrendingUp className="h-4 w-4 text-emerald-400" />
                  </div>
                  <div className="text-2xl font-black text-emerald-400">
                    {analytics.cohortAverageCurrentKnowledge.toFixed(1)}%
                  </div>
                  <p className="text-[10px] text-secondary-theme">Current student mastery average.</p>
                </div>

                {/* Average Learning Gain */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
                      Average Learning Gain
                    </span>
                    <CheckCircle2 className="h-4 w-4 text-indigo-400" />
                  </div>
                  <div className="text-2xl font-black text-indigo-300">
                    {analytics.cohortAverageLearningGain >= 0 ? "+" : ""}
                    {analytics.cohortAverageLearningGain.toFixed(2)}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Observed knowledge gain index.</p>
                </div>
              </div>
            </div>

            {/* QUICK ACTIONS & MODULE ACCESS */}
            <div className="space-y-3 pt-2">
              <div className="flex items-center gap-2">
                <Settings className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">
                  Admin Analytics Modules
                </h2>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Subject Analytics Link Card */}
                <div
                  onClick={() => navigate("/admin/subject-analytics")}
                  className="glass-panel p-6 rounded-3xl border border-white/5 hover:border-purple-500/30 transition-all duration-300 cursor-pointer group flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="h-10 w-10 rounded-xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
                        <BookOpen className="h-5 w-5" />
                      </div>
                      <span className="text-xs font-bold text-purple-400 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                        <span>Open Module</span>
                        <ArrowRight className="h-3.5 w-3.5" />
                      </span>
                    </div>
                    <h3 className="text-base font-extrabold text-main-theme">Subject Analytics</h3>
                    <p className="text-xs text-secondary-theme">
                      View subject-wise learning performance, knowledge growth, concept mastery distribution, and weak concept breakdown.
                    </p>
                  </div>
                  <div className="pt-2 border-t border-white/5 text-[11px] text-purple-300 font-medium">
                    Curriculum metrics & subject growth &rarr;
                  </div>
                </div>

                {/* Student Directory Link Card */}
                <div
                  onClick={() => navigate("/admin/student-directory")}
                  className="glass-panel p-6 rounded-3xl border border-white/5 hover:border-cyan-500/30 transition-all duration-300 cursor-pointer group flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="h-10 w-10 rounded-xl bg-cyan-600/20 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
                        <Users className="h-5 w-5" />
                      </div>
                      <span className="text-xs font-bold text-cyan-400 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                        <span>Open Module</span>
                        <ArrowRight className="h-3.5 w-3.5" />
                      </span>
                    </div>
                    <h3 className="text-base font-extrabold text-main-theme">Student Directory</h3>
                    <p className="text-xs text-secondary-theme">
                      Browse enrolled students, search by branch or email, inspect diagnostic baselines, and access individual student learning analysis.
                    </p>
                  </div>
                  <div className="pt-2 border-t border-white/5 text-[11px] text-cyan-300 font-medium">
                    Student accounts & individual profiles &rarr;
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

      </div>
    </Layout>
  );
}
