import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../../components/Layout";
import {
  FlaskConical,
  RefreshCw,
  ShieldAlert,
  Users,
  CheckCircle2,
  Activity,
  AlertTriangle,
  UserX,
  TrendingUp,
  BarChart2,
  Star,
  Bot,
  Sparkles,
  BookOpen,
  Info,
  ArrowUpRight,
  Minus,
  ArrowDownRight,
  Search,
  X,
  ChevronRight,
  GraduationCap
} from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell
} from "recharts";
import {
  fetchAdminCohortAnalytics,
  fetchAdminStudentDirectory,
  AdminCohortAnalytics,
  AdminStudentDirectoryDTO
} from "../../../services/api";

export default function ResearchAnalyticsPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<"cohort" | "directory">("cohort");

  // Cohort state
  const [cohort, setCohort] = useState<AdminCohortAnalytics | null>(null);
  const [cohortLoading, setCohortLoading] = useState(true);
  const [cohortError, setCohortError] = useState<string | null>(null);

  // Directory state
  const [students, setStudents] = useState<AdminStudentDirectoryDTO[]>([]);
  const [directoryLoading, setDirectoryLoading] = useState(false);
  const [directoryError, setDirectoryError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterActivity, setFilterActivity] = useState<string>("ALL");

  const loadCohortAnalytics = async () => {
    setCohortLoading(true);
    setCohortError(null);
    try {
      const data = await fetchAdminCohortAnalytics();
      if (data) {
        setCohort(data);
      } else {
        setCohortError("Unable to retrieve cohort research analytics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err) {
      setCohortError("Network error while communicating with the cohort analytics service.");
    } finally {
      setCohortLoading(false);
    }
  };

  const loadStudentDirectory = async () => {
    setDirectoryLoading(true);
    setDirectoryError(null);
    try {
      const list = await fetchAdminStudentDirectory();
      setStudents(list);
    } catch (err) {
      setDirectoryError("Network error while communicating with the student directory service.");
    } finally {
      setDirectoryLoading(false);
    }
  };

  useEffect(() => {
    loadCohortAnalytics();
    loadStudentDirectory();
  }, []);

  const filteredStudents = students.filter((s) => {
    const matchesSearch =
      (s.fullName && s.fullName.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (s.email && s.email.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (s.branch && s.branch.toLowerCase().includes(searchQuery.toLowerCase()));

    if (!matchesSearch) return false;

    if (filterActivity === "ALL") return true;
    if (filterActivity === "BASELINE") return s.hasAuthenticBaseline;
    return s.activityStatus === filterActivity;
  });

  const chartData = cohort ? [
    {
      name: "Improved",
      count: cohort.growthDistribution.improvedCount,
      percentage: cohort.growthDistribution.improvedPercentage,
      fill: "#10b981"
    },
    {
      name: "Unchanged",
      count: cohort.growthDistribution.unchangedCount,
      percentage: cohort.growthDistribution.unchangedPercentage,
      fill: "#6366f1"
    },
    {
      name: "Declined",
      count: cohort.growthDistribution.declinedCount,
      percentage: cohort.growthDistribution.declinedPercentage,
      fill: "#f43f5e"
    }
  ] : [];

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto">
        
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2.5">
              <FlaskConical className="h-8 w-8 text-purple-theme" />
              <span>Research & Analytics</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              Cohort-level learning growth and individual student research profiles
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* View Tab Switcher */}
            <div className="flex items-center p-1 rounded-xl bg-white/5 border border-white/10">
              <button
                onClick={() => setActiveTab("cohort")}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "cohort"
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-500/20"
                    : "text-secondary-theme hover:text-main-theme"
                }`}
              >
                <FlaskConical className="h-3.5 w-3.5" />
                <span>Cohort Analytics</span>
              </button>
              <button
                onClick={() => setActiveTab("directory")}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "directory"
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-500/20"
                    : "text-secondary-theme hover:text-main-theme"
                }`}
              >
                <Users className="h-3.5 w-3.5" />
                <span>Student Directory</span>
                {students.length > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full bg-white/20 text-[10px] font-extrabold">
                    {students.length}
                  </span>
                )}
              </button>
            </div>

            <button
              onClick={activeTab === "cohort" ? loadCohortAnalytics : loadStudentDirectory}
              disabled={activeTab === "cohort" ? cohortLoading : directoryLoading}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
            >
              <RefreshCw className={`h-4 w-4 text-purple-theme ${(activeTab === "cohort" ? cohortLoading : directoryLoading) ? "animate-spin" : ""}`} />
              <span>{(activeTab === "cohort" ? cohortLoading : directoryLoading) ? "Refreshing..." : "Refresh"}</span>
            </button>
          </div>
        </div>

        {/* TAB 1: COHORT ANALYTICS */}
        {activeTab === "cohort" && (
          <>
            {/* Error Notification with Retry */}
            {cohortError && (
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-3">
                  <ShieldAlert className="h-5 w-5 shrink-0" />
                  <span>{cohortError}</span>
                </div>
                <button
                  onClick={loadCohortAnalytics}
                  className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs transition-colors cursor-pointer"
                >
                  Retry
                </button>
              </div>
            )}

            {/* Loading Skeleton */}
            {cohortLoading && !cohort && (
              <div className="space-y-8 animate-pulse">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 space-y-3 h-28 bg-white/5" />
                  ))}
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="glass-panel p-6 rounded-2xl border border-white/5 space-y-3 h-36 bg-white/5" />
                  ))}
                </div>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                  <div className="glass-panel p-6 rounded-2xl border border-white/5 h-64 bg-white/5" />
                  <div className="glass-panel p-6 rounded-2xl border border-white/5 h-64 bg-white/5" />
                </div>
              </div>
            )}

            {/* Loaded Content */}
            {cohort && (
              <>
                {/* SECTION A — Cohort Overview */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-purple-theme" />
                    <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">Section A — Cohort Overview</h2>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5">
                    {/* 1. Total Enrolled */}
                    <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Total Enrolled</span>
                        <Users className="h-4 w-4 text-purple-400" />
                      </div>
                      <div className="text-2xl font-black text-main-theme">
                        {cohort.totalEnrolled}
                      </div>
                      <p className="text-[10px] text-secondary-theme">Canonical STUDENT users.</p>
                    </div>

                    {/* 2. Evaluated Cohort */}
                    <div className="glass-panel p-5 rounded-2xl border border-purple-500/20 bg-purple-500/5 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-purple-300 uppercase font-extrabold tracking-wider">Evaluated Cohort</span>
                        <CheckCircle2 className="h-4 w-4 text-purple-400" />
                      </div>
                      <div className="text-2xl font-black text-purple-theme">
                        {cohort.evaluatedCohortSize} <span className="text-sm font-semibold text-secondary-theme">/ {cohort.totalEnrolled}</span>
                      </div>
                      <p className="text-[10px] text-purple-300/70">
                        {cohort.totalEnrolled > 0
                          ? `${((cohort.evaluatedCohortSize / cohort.totalEnrolled) * 100).toFixed(1)}% verified diagnostics`
                          : "No enrolled students"}
                      </p>
                    </div>

                    {/* 3. Active Last 7 Days */}
                    <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Active (7 Days)</span>
                        <Activity className="h-4 w-4 text-emerald-400" />
                      </div>
                      <div className="text-2xl font-black text-emerald-theme">
                        {cohort.activeLast7Days}
                      </div>
                      <p className="text-[10px] text-secondary-theme">
                        {cohort.totalEnrolled > 0 ? `${((cohort.activeLast7Days / cohort.totalEnrolled) * 100).toFixed(1)}% of enrolled` : "Recent activity"}
                      </p>
                    </div>

                    {/* 4. At Risk */}
                    <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">At Risk</span>
                        <AlertTriangle className="h-4 w-4 text-amber-400" />
                      </div>
                      <div className="text-2xl font-black text-amber-theme">
                        {cohort.atRiskStudents}
                      </div>
                      <p className="text-[10px] text-secondary-theme">Inactive 8–14 days.</p>
                    </div>

                    {/* 5. Inactive */}
                    <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Inactive</span>
                        <UserX className="h-4 w-4 text-pink-500" />
                      </div>
                      <div className="text-2xl font-black text-pink-500">
                        {cohort.inactiveStudents}
                      </div>
                      <p className="text-[10px] text-secondary-theme">&gt; 14 days or no activity.</p>
                    </div>
                  </div>
                </div>

                {/* SECTION B — Knowledge Growth */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-purple-theme" />
                    <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">Section B — Knowledge Growth</h2>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {/* Mean Baseline Knowledge K0 */}
                    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-secondary-theme uppercase font-extrabold tracking-wider">Mean Baseline Knowledge (K₀)</span>
                        <BarChart2 className="h-5 w-5 text-indigo-400" />
                      </div>
                      <div className="text-3xl font-black text-indigo-400">
                        {cohort.meanBaselineKnowledge.toFixed(1)}%
                      </div>
                      <p className="text-xs text-secondary-theme">
                        Diagnostic baseline percentage across evaluated cohort.
                      </p>
                    </div>

                    {/* Mean Current Knowledge Kt */}
                    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-secondary-theme uppercase font-extrabold tracking-wider">Mean Current Knowledge (Kₜ)</span>
                        <TrendingUp className="h-5 w-5 text-emerald-400" />
                      </div>
                      <div className="text-3xl font-black text-emerald-theme">
                        {cohort.meanCurrentKnowledge.toFixed(1)}%
                      </div>
                      <p className="text-xs text-secondary-theme">
                        Current concept mastery percentage across evaluated cohort.
                      </p>
                    </div>

                    {/* Mean Normalized Gain */}
                    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-secondary-theme uppercase font-extrabold tracking-wider">Mean Normalized Gain (g)</span>
                        <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                      </div>
                      <div className="text-3xl font-black text-emerald-400">
                        {cohort.meanNormalizedGain.toFixed(2)}
                      </div>
                      <p className="text-xs text-secondary-theme">
                        Normalized gain (Hake): (Post − Pre) / (100 − Pre).
                      </p>
                    </div>
                  </div>
                </div>

                {/* SECTION C & SECTION D GRID */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                  {/* SECTION C — Growth Distribution */}
                  <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 flex flex-col justify-between">
                    <div>
                      <div className="flex items-center justify-between border-b border-white/5 pb-3">
                        <div>
                          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Section C — Growth Distribution</h3>
                          <p className="text-xs text-secondary-theme mt-0.5">Individual student knowledge delta (Kₜ − K₀)</p>
                        </div>
                      </div>

                      {/* Summary Cards */}
                      <div className="grid grid-cols-3 gap-3 my-4">
                        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-center space-y-1">
                          <div className="flex items-center justify-center gap-1 text-emerald-400 text-xs font-bold">
                            <ArrowUpRight className="h-3.5 w-3.5" />
                            <span>Improved</span>
                          </div>
                          <div className="text-xl font-black text-emerald-400">
                            {cohort.growthDistribution.improvedCount}
                          </div>
                          <div className="text-[10px] text-emerald-300/80 font-bold">
                            {cohort.growthDistribution.improvedPercentage.toFixed(1)}%
                          </div>
                        </div>

                        <div className="p-3.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-center space-y-1">
                          <div className="flex items-center justify-center gap-1 text-indigo-400 text-xs font-bold">
                            <Minus className="h-3.5 w-3.5" />
                            <span>Unchanged</span>
                          </div>
                          <div className="text-xl font-black text-indigo-400">
                            {cohort.growthDistribution.unchangedCount}
                          </div>
                          <div className="text-[10px] text-indigo-300/80 font-bold">
                            {cohort.growthDistribution.unchangedPercentage.toFixed(1)}%
                          </div>
                        </div>

                        <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-center space-y-1">
                          <div className="flex items-center justify-center gap-1 text-rose-400 text-xs font-bold">
                            <ArrowDownRight className="h-3.5 w-3.5" />
                            <span>Declined</span>
                          </div>
                          <div className="text-xl font-black text-rose-400">
                            {cohort.growthDistribution.declinedCount}
                          </div>
                          <div className="text-[10px] text-rose-300/80 font-bold">
                            {cohort.growthDistribution.declinedPercentage.toFixed(1)}%
                          </div>
                        </div>
                      </div>

                      {/* Recharts Bar Chart */}
                      <div className="h-44 w-full pt-2">
                        <ResponsiveContainer width="100%" height="100%">
                          <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                            <XAxis dataKey="name" stroke="#71717a" fontSize={11} tickLine={false} />
                            <YAxis stroke="#71717a" fontSize={11} tickLine={false} allowDecimals={false} />
                            <Tooltip
                              contentStyle={{
                                backgroundColor: "#0d0f1e",
                                borderColor: "rgba(255,255,255,0.1)",
                                borderRadius: "12px",
                                fontSize: "12px"
                              }}
                              formatter={(value: any, name: any, item: any) => [
                                `${value} students (${item.payload.percentage.toFixed(1)}%)`,
                                "Count"
                              ]}
                            />
                            <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                              {chartData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={entry.fill} />
                              ))}
                            </Bar>
                          </BarChart>
                        </ResponsiveContainer>
                      </div>
                    </div>

                    <p className="text-[10px] text-secondary-theme text-center">
                      Percentages calculated using evaluated cohort size ({cohort.evaluatedCohortSize}) as denominator.
                    </p>
                  </div>

                  {/* SECTION D — Satisfaction */}
                  <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 flex flex-col justify-between">
                    <div>
                      <div className="flex items-center justify-between border-b border-white/5 pb-3">
                        <div>
                          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Section D — Student Satisfaction</h3>
                          <p className="text-xs text-secondary-theme mt-0.5">Persisted rating surveys and category breakdown</p>
                        </div>
                        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-300 font-bold text-xs">
                          <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                          <span>{cohort.satisfaction.averageRating.toFixed(1)} / 5.0</span>
                        </div>
                      </div>

                      {/* Overall stats */}
                      <div className="grid grid-cols-2 gap-4 my-4">
                        <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                          <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Average Rating</span>
                          <div className="text-2xl font-black text-amber-theme flex items-center gap-1.5">
                            <Star className="h-5 w-5 fill-amber-400 text-amber-400" />
                            <span>{cohort.satisfaction.averageRating.toFixed(1)}</span>
                            <span className="text-xs font-semibold text-secondary-theme">/ 5.0</span>
                          </div>
                          <p className="text-[10px] text-secondary-theme">Aggregated survey score.</p>
                        </div>

                        <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                          <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Total Reviews</span>
                          <div className="text-2xl font-black text-cyan-theme">
                            {cohort.satisfaction.totalReviews}
                          </div>
                          <p className="text-[10px] text-secondary-theme">Submitted feedback entries.</p>
                        </div>
                      </div>

                      {/* Category Ratings Breakdown */}
                      <div className="space-y-2.5 pt-1">
                        <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider block">Category Ratings Breakdown</span>

                        {/* AI Tutor */}
                        <div className="flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/5">
                          <div className="flex items-center gap-2.5">
                            <Bot className="h-4 w-4 text-purple-400" />
                            <span className="text-xs font-semibold text-main-theme">AI Tutor</span>
                          </div>
                          <span className="text-xs font-bold text-main-theme">
                            {cohort.satisfaction.byCategory.AI_TUTOR != null
                              ? `${cohort.satisfaction.byCategory.AI_TUTOR.toFixed(1)} / 5.0`
                              : "No data"}
                          </span>
                        </div>

                        {/* Recommendation */}
                        <div className="flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/5">
                          <div className="flex items-center gap-2.5">
                            <Sparkles className="h-4 w-4 text-cyan-400" />
                            <span className="text-xs font-semibold text-main-theme">Recommendation</span>
                          </div>
                          <span className="text-xs font-bold text-main-theme">
                            {cohort.satisfaction.byCategory.RECOMMENDATION != null
                              ? `${cohort.satisfaction.byCategory.RECOMMENDATION.toFixed(1)} / 5.0`
                              : "No data"}
                          </span>
                        </div>

                        {/* Learning Activity */}
                        <div className="flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/5">
                          <div className="flex items-center gap-2.5">
                            <BookOpen className="h-4 w-4 text-emerald-400" />
                            <span className="text-xs font-semibold text-main-theme">Learning Activity</span>
                          </div>
                          <span className="text-xs font-bold text-main-theme">
                            {cohort.satisfaction.byCategory.LEARNING_ACTIVITY != null
                              ? `${cohort.satisfaction.byCategory.LEARNING_ACTIVITY.toFixed(1)} / 5.0`
                              : "No data"}
                          </span>
                        </div>
                      </div>
                    </div>

                    <p className="text-[10px] text-secondary-theme text-center">
                      Unreviewed categories display "No data" without fabrication.
                    </p>
                  </div>

                </div>

                {/* SECTION E — Research Data Sufficiency */}
                <div className="glass-panel p-5 rounded-2xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-4">
                  <div className="p-2 rounded-xl bg-indigo-500/20 text-indigo-400 shrink-0 mt-0.5">
                    <Info className="h-5 w-5" />
                  </div>
                  <div className="space-y-1">
                    <h4 className="text-xs font-bold text-indigo-300 uppercase tracking-wider">Research Data Sufficiency</h4>
                    <p className="text-sm font-semibold text-main-theme">
                      {cohort.dataSufficiencyNote}
                    </p>
                    <p className="text-xs text-secondary-theme">
                      Research integrity rule: Only authentic baseline diagnostics and confirmed concept mastery evaluations contribute to research cohort knowledge averages.
                    </p>
                  </div>
                </div>
              </>
            )}
          </>
        )}

        {/* TAB 2: STUDENT DIRECTORY */}
        {activeTab === "directory" && (
          <div className="space-y-6">
            {/* Directory Controls */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 glass-panel p-5 rounded-2xl border border-white/5">
              {/* Search input */}
              <div className="relative flex-1">
                <Search className="h-4 w-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-secondary-theme" />
                <input
                  type="text"
                  placeholder="Search students by name, email, branch..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-main-theme placeholder:text-secondary-theme focus:outline-none focus:border-purple-500/50"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery("")}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-secondary-theme hover:text-main-theme"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              {/* Filter Chips */}
              <div className="flex items-center gap-1.5 flex-wrap">
                {[
                  { key: "ALL", label: "All Students" },
                  { key: "BASELINE", label: "Verified Baseline" },
                  { key: "ACTIVE", label: "Active" },
                  { key: "AT_RISK", label: "At Risk" },
                  { key: "INACTIVE", label: "Inactive" }
                ].map((tab) => (
                  <button
                    key={tab.key}
                    onClick={() => setFilterActivity(tab.key)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                      filterActivity === tab.key
                        ? "bg-purple-600/30 border border-purple-500/50 text-purple-300"
                        : "bg-white/5 border border-white/5 text-secondary-theme hover:text-main-theme"
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Directory Error */}
            {directoryError && (
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex items-center justify-between text-sm">
                <div className="flex items-center gap-3">
                  <ShieldAlert className="h-5 w-5 shrink-0" />
                  <span>{directoryError}</span>
                </div>
                <button
                  onClick={loadStudentDirectory}
                  className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs"
                >
                  Retry
                </button>
              </div>
            )}

            {/* Directory Loading Skeleton */}
            {directoryLoading && students.length === 0 && (
              <div className="space-y-3 animate-pulse">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-20 bg-white/5" />
                ))}
              </div>
            )}

            {/* Students Table */}
            {!directoryLoading && filteredStudents.length > 0 && (
              <div className="glass-panel rounded-2xl border border-white/5 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="border-b border-white/5 bg-white/[0.02] text-[10px] uppercase font-extrabold tracking-wider text-secondary-theme">
                        <th className="p-4 pl-6">Student</th>
                        <th className="p-4">Academic Info</th>
                        <th className="p-4">Activity Status</th>
                        <th className="p-4">Baseline Diagnostic</th>
                        <th className="p-4 text-right">Baseline (K₀)</th>
                        <th className="p-4 text-right">Current (Kₜ)</th>
                        <th className="p-4 text-right">Growth (ΔK)</th>
                        <th className="p-4 pr-6 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5 text-xs">
                      {filteredStudents.map((s) => (
                        <tr key={s.userId} className="hover:bg-white/[0.02] transition-colors">
                          <td className="p-4 pl-6">
                            <div className="font-bold text-main-theme">{s.fullName || "Student"}</div>
                            <div className="text-[11px] text-secondary-theme">{s.email}</div>
                          </td>
                          <td className="p-4">
                            <div className="text-main-theme font-medium">{s.branch || "General"}</div>
                            <div className="text-[11px] text-secondary-theme">
                              {s.semester ? `Semester ${s.semester}` : "Semester N/A"}
                            </div>
                          </td>
                          <td className="p-4">
                            {s.activityStatus === "ACTIVE" && (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                                ACTIVE
                              </span>
                            )}
                            {s.activityStatus === "AT_RISK" && (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
                                AT RISK
                              </span>
                            )}
                            {s.activityStatus === "INACTIVE" && (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                                <span className="w-1.5 h-1.5 rounded-full bg-rose-400" />
                                INACTIVE
                              </span>
                            )}
                            {s.activityStatus === "NO_ACTIVITY" && (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-zinc-500/10 text-zinc-400 border border-zinc-500/20">
                                <span className="w-1.5 h-1.5 rounded-full bg-zinc-400" />
                                NO ACTIVITY
                              </span>
                            )}
                          </td>
                          <td className="p-4">
                            {s.hasAuthenticBaseline ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-purple-500/10 text-purple-300 border border-purple-500/20">
                                <CheckCircle2 className="h-3 w-3 text-purple-400" />
                                Verified Baseline
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-white/5 text-secondary-theme border border-white/10">
                                <Minus className="h-3 w-3" />
                                No Baseline
                              </span>
                            )}
                          </td>
                          <td className="p-4 text-right font-mono font-semibold text-main-theme">
                            {s.baselineKnowledge != null ? `${s.baselineKnowledge.toFixed(1)}%` : <span className="text-secondary-theme">N/A</span>}
                          </td>
                          <td className="p-4 text-right font-mono font-semibold text-emerald-theme">
                            {s.currentKnowledge != null ? `${s.currentKnowledge.toFixed(1)}%` : <span className="text-secondary-theme">N/A</span>}
                          </td>
                          <td className="p-4 text-right font-mono font-bold">
                            {s.growthPp != null ? (
                              <span className={s.growthPp >= 0 ? "text-emerald-400" : "text-rose-400"}>
                                {s.growthPp >= 0 ? `+${s.growthPp.toFixed(1)} pp` : `${s.growthPp.toFixed(1)} pp`}
                              </span>
                            ) : (
                              <span className="text-secondary-theme">N/A</span>
                            )}
                          </td>
                          <td className="p-4 pr-6 text-right">
                            <button
                              onClick={() => navigate(`/admin/research-analytics/students/${encodeURIComponent(s.userId)}`)}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 border border-purple-500/30 text-xs font-bold transition-all cursor-pointer"
                            >
                              <span>View Analysis</span>
                              <ChevronRight className="h-3.5 w-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Empty State */}
            {!directoryLoading && filteredStudents.length === 0 && (
              <div className="glass-panel p-12 rounded-2xl border border-white/5 text-center space-y-3">
                <GraduationCap className="h-10 w-10 text-secondary-theme mx-auto opacity-50" />
                <h3 className="text-sm font-bold text-main-theme">No Students Found</h3>
                <p className="text-xs text-secondary-theme max-w-md mx-auto">
                  {searchQuery
                    ? `No students match the search criteria "${searchQuery}".`
                    : "No students are currently enrolled in the directory."}
                </p>
                {searchQuery && (
                  <button
                    onClick={() => {
                      setSearchQuery("");
                      setFilterActivity("ALL");
                    }}
                    className="px-4 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-main-theme border border-white/10"
                  >
                    Clear Filters
                  </button>
                )}
              </div>
            )}
          </div>
        )}

      </div>
    </Layout>
  );
}
