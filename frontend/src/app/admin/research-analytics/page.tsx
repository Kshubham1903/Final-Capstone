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
  GraduationCap,
  Calendar,
  Layers
} from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  Legend,
  CartesianGrid
} from "recharts";
import {
  fetchAdminCohortAnalytics,
  fetchAdminStudentDirectory,
  fetchAdminResearchTrends,
  fetchAdminSubjectAnalytics,
  AdminCohortAnalytics,
  AdminStudentDirectoryDTO,
  AdminResearchTrendsDTO,
  AdminCohortSubjectAnalyticsDTO,
  SubjectResearchSummaryDTO,
  AdminResearchAnalyticsFilters
} from "../../../services/api";
import { AdminResearchFilterBar } from "../../../components/admin/AdminResearchFilterBar";

export default function ResearchAnalyticsPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<"cohort" | "trends" | "subjects" | "directory">("cohort");

  // Filter State
  const [filters, setFilters] = useState<AdminResearchAnalyticsFilters>({});
  const [isFilterApplying, setIsFilterApplying] = useState(false);

  // Persistent Reference Lists for Filter Selectors (Unfiltered)
  const [allAvailableBranches, setAllAvailableBranches] = useState<string[]>([]);
  const [allAvailableSubjects, setAllAvailableSubjects] = useState<Array<{ code: string; name: string }>>([]);

  // Cohort state
  const [cohort, setCohort] = useState<AdminCohortAnalytics | null>(null);
  const [cohortLoading, setCohortLoading] = useState(true);
  const [cohortError, setCohortError] = useState<string | null>(null);

  // Historical Trends state
  const [trends, setTrends] = useState<AdminResearchTrendsDTO | null>(null);
  const [trendsLoading, setTrendsLoading] = useState(false);
  const [trendsError, setTrendsError] = useState<string | null>(null);

  // Subject Research Analytics state
  const [subjectsData, setSubjectsData] = useState<AdminCohortSubjectAnalyticsDTO | null>(null);
  const [subjectsLoading, setSubjectsLoading] = useState(false);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);

  // Directory state
  const [students, setStudents] = useState<AdminStudentDirectoryDTO[]>([]);
  const [directoryLoading, setDirectoryLoading] = useState(false);
  const [directoryError, setDirectoryError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterActivity, setFilterActivity] = useState<string>("ALL");

  const loadCohortAnalytics = async (f: AdminResearchAnalyticsFilters = filters) => {
    setCohortLoading(true);
    setCohortError(null);
    try {
      const data = await fetchAdminCohortAnalytics(f);
      if (data) {
        setCohort(data);
      } else {
        setCohortError("Unable to retrieve cohort research analytics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err: any) {
      setCohortError(err?.message || "Invalid research analytics filter. Please check the selected filters.");
    } finally {
      setCohortLoading(false);
    }
  };

  const loadResearchTrends = async (f: AdminResearchAnalyticsFilters = filters) => {
    setTrendsLoading(true);
    setTrendsError(null);
    try {
      const data = await fetchAdminResearchTrends(f);
      if (data) {
        setTrends(data);
      } else {
        setTrendsError("Unable to retrieve historical research trends. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err: any) {
      setTrendsError(err?.message || "Invalid research analytics filter. Please check the selected filters.");
    } finally {
      setTrendsLoading(false);
    }
  };

  const loadSubjectAnalytics = async (f: AdminResearchAnalyticsFilters = filters) => {
    setSubjectsLoading(true);
    setSubjectsError(null);
    try {
      const data = await fetchAdminSubjectAnalytics(f);
      if (data) {
        setSubjectsData(data);

        // Extract and preserve unfiltered subjects reference list
        const isUnfiltered = !f || Object.keys(f).length === 0;
        if (isUnfiltered && data.subjects && data.subjects.length > 0) {
          const subjects = data.subjects.map((sub) => ({
            code: sub.subjectCode,
            name: sub.subjectName
          }));
          setAllAvailableSubjects((prev) => {
            const existingKeys = new Set(prev.map((p) => p.code || p.name));
            const newOnes = subjects.filter((s) => !existingKeys.has(s.code || s.name));
            return [...prev, ...newOnes];
          });
        }
      } else {
        setSubjectsError("Unable to retrieve subject research analytics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err: any) {
      setSubjectsError(err?.message || "Invalid research analytics filter. Please check the selected filters.");
    } finally {
      setSubjectsLoading(false);
    }
  };

  const loadStudentDirectory = async (f: AdminResearchAnalyticsFilters = filters) => {
    setDirectoryLoading(true);
    setDirectoryError(null);
    try {
      const list = await fetchAdminStudentDirectory(f);
      setStudents(list);

      // Extract and preserve unfiltered branches reference list
      const isUnfiltered = !f || Object.keys(f).length === 0;
      if (isUnfiltered && list.length > 0) {
        const branches = Array.from(
          new Set(
            list
              .map((s) => s.branch)
              .filter((b): b is string => Boolean(b && b.trim()))
          )
        );
        if (branches.length > 0) {
          setAllAvailableBranches((prev) => Array.from(new Set([...prev, ...branches])));
        }
      }
    } catch (err: any) {
      setDirectoryError(err?.message || "Invalid research analytics filter. Please check the selected filters.");
    } finally {
      setDirectoryLoading(false);
    }
  };

  const loadAllAnalytics = async (f: AdminResearchAnalyticsFilters = filters) => {
    setIsFilterApplying(true);
    try {
      await Promise.allSettled([
        loadCohortAnalytics(f),
        loadResearchTrends(f),
        loadSubjectAnalytics(f),
        loadStudentDirectory(f)
      ]);
    } finally {
      setIsFilterApplying(false);
    }
  };

  const handleApplyFilters = (newFilters: AdminResearchAnalyticsFilters) => {
    setFilters(newFilters);
    loadAllAnalytics(newFilters);
  };

  const handleClearFilters = () => {
    setFilters({});
    loadAllAnalytics({});
  };

  useEffect(() => {
    loadAllAnalytics(filters);
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
              Cohort-level learning growth, empirical historical trends, and individual student profiles
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
                onClick={() => setActiveTab("trends")}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "trends"
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-500/20"
                    : "text-secondary-theme hover:text-main-theme"
                }`}
              >
                <TrendingUp className="h-3.5 w-3.5" />
                <span>Historical Trends</span>
                {trends && trends.totalObservations > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full bg-white/20 text-[10px] font-extrabold">
                    {trends.totalObservations}
                  </span>
                )}
              </button>
              <button
                onClick={() => setActiveTab("subjects")}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  activeTab === "subjects"
                    ? "bg-purple-600 text-white shadow-lg shadow-purple-500/20"
                    : "text-secondary-theme hover:text-main-theme"
                }`}
              >
                <BookOpen className="h-3.5 w-3.5" />
                <span>Subject Analytics</span>
                {subjectsData && subjectsData.totalSubjectsCount > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full bg-white/20 text-[10px] font-extrabold">
                    {subjectsData.totalSubjectsCount}
                  </span>
                )}
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
              onClick={() => {
                if (activeTab === "cohort") loadCohortAnalytics(filters);
                else if (activeTab === "trends") loadResearchTrends(filters);
                else if (activeTab === "subjects") loadSubjectAnalytics(filters);
                else loadStudentDirectory(filters);
              }}
              disabled={
                activeTab === "cohort" ? cohortLoading :
                activeTab === "trends" ? trendsLoading :
                activeTab === "subjects" ? subjectsLoading :
                directoryLoading
              }
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
            >
              <RefreshCw className={`h-4 w-4 text-purple-theme ${(
                activeTab === "cohort" ? cohortLoading :
                activeTab === "trends" ? trendsLoading :
                activeTab === "subjects" ? subjectsLoading :
                directoryLoading
              ) ? "animate-spin" : ""}`} />
              <span>{(
                activeTab === "cohort" ? cohortLoading :
                activeTab === "trends" ? trendsLoading :
                activeTab === "subjects" ? subjectsLoading :
                directoryLoading
              ) ? "Refreshing..." : "Refresh"}</span>
            </button>
          </div>
        </div>

        {/* REUSABLE RESEARCH FILTER BAR */}
        <AdminResearchFilterBar
          filters={filters}
          onApplyFilters={handleApplyFilters}
          onClearFilters={handleClearFilters}
          isLoading={isFilterApplying || cohortLoading || trendsLoading || subjectsLoading || directoryLoading}
          availableBranches={allAvailableBranches}
          availableSubjects={allAvailableSubjects}
        />

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
                  onClick={() => loadCohortAnalytics(filters)}
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

        {/* TAB 2: HISTORICAL RESEARCH TRENDS */}
        {activeTab === "trends" && (
          <div className="space-y-6">
            {/* Error State */}
            {trendsError && (
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-3">
                  <ShieldAlert className="h-5 w-5 shrink-0" />
                  <span>{trendsError}</span>
                </div>
                <button
                  onClick={() => loadResearchTrends(filters)}
                  className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs transition-colors cursor-pointer"
                >
                  Retry
                </button>
              </div>
            )}

            {/* Loading Skeleton */}
            {trendsLoading && !trends && (
              <div className="space-y-6 animate-pulse">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-28 bg-white/5" />
                  ))}
                </div>
                <div className="glass-panel p-6 rounded-3xl border border-white/5 h-80 bg-white/5" />
                <div className="glass-panel p-6 rounded-2xl border border-white/5 h-64 bg-white/5" />
              </div>
            )}

            {/* Loaded Trends Content */}
            {trends && (
              <>
                {/* 1. Summary Overview Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                  {/* Total Assessments */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Total Assessments</span>
                      <Layers className="h-4 w-4 text-purple-400" />
                    </div>
                    <div className="text-2xl font-black text-main-theme">
                      {trends.totalAssessments}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Persisted assessment results ({trends.totalObservations} total events).</p>
                  </div>

                  {/* Represented Students */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Evaluated Students</span>
                      <Users className="h-4 w-4 text-emerald-400" />
                    </div>
                    <div className="text-2xl font-black text-emerald-theme">
                      {trends.uniqueStudentsCount}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Distinct learners with evaluation data.</p>
                  </div>

                  {/* Baseline Validated */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Verified Baselines</span>
                      <CheckCircle2 className="h-4 w-4 text-indigo-400" />
                    </div>
                    <div className="text-2xl font-black text-indigo-400">
                      {trends.evaluatedStudentsWithBaseline}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Students with authentic diagnostic baseline.</p>
                  </div>

                  {/* Observation Span */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Milestone Dates</span>
                      <Calendar className="h-4 w-4 text-cyan-400" />
                    </div>
                    <div className="text-2xl font-black text-cyan-400">
                      {trends.observations.length}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Distinct calendar dates with evaluation data.</p>
                  </div>
                </div>

                {/* 2. Longitudinal Trend Chart */}
                <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/5 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
                    <div>
                      <h3 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                        <TrendingUp className="h-4 w-4 text-purple-theme" />
                        <span>Empirical Observed Assessment Performance</span>
                      </h3>
                      <p className="text-xs text-secondary-theme mt-0.5">
                        Chronological progression of observed knowledge evaluation performance (AssessmentResult records only)
                      </p>
                    </div>

                    <div className="flex items-center gap-2 text-xs flex-wrap">
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        <span className="w-2 h-2 rounded-full bg-emerald-400" />
                        Observed Assessment Score
                      </span>
                      {trends.observations.some(o => o.meanQuizAccuracy != null) && (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                          <span className="w-2 h-2 rounded-full bg-cyan-400" />
                          Quiz Accuracy (Separate)
                        </span>
                      )}
                    </div>
                  </div>

                  {trends.observations.length > 0 ? (
                    <div className="h-72 w-full pt-4">
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={trends.observations} margin={{ top: 10, right: 20, left: -20, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                          <XAxis
                            dataKey="date"
                            stroke="#71717a"
                            fontSize={11}
                            tickLine={false}
                            tickFormatter={(val) => {
                              const d = new Date(val);
                              return !isNaN(d.getTime()) ? d.toLocaleDateString(undefined, { month: "short", day: "numeric" }) : val;
                            }}
                          />
                          <YAxis stroke="#71717a" fontSize={11} tickLine={false} domain={[0, 100]} />
                          <Tooltip
                            contentStyle={{
                              backgroundColor: "#0d0f1e",
                              borderColor: "rgba(255,255,255,0.1)",
                              borderRadius: "12px",
                              fontSize: "12px"
                            }}
                            formatter={(value: any, name: any) => [value != null ? `${value}%` : "N/A", name]}
                            labelFormatter={(label: any) => `Date: ${label}`}
                          />
                          <Legend />
                          <Line
                            type="monotone"
                            dataKey="meanAssessmentScore"
                            name="Observed Assessment Performance (%)"
                            stroke="#10b981"
                            strokeWidth={2.5}
                            connectNulls={true}
                            dot={{ fill: "#10b981", r: 4 }}
                          />
                          {trends.observations.some(o => o.meanQuizAccuracy != null) && (
                            <Line
                              type="monotone"
                              dataKey="meanQuizAccuracy"
                              name="Quiz Accuracy (% separate)"
                              stroke="#06b6d4"
                              strokeWidth={1.5}
                              strokeDasharray="4 4"
                              connectNulls={true}
                              dot={{ fill: "#06b6d4", r: 3 }}
                            />
                          )}
                          {trends.observations.some(o => o.meanEngagementScore != null) && (
                            <Line
                              type="monotone"
                              dataKey="meanEngagementScore"
                              name="Engagement Index (% separate)"
                              stroke="#6366f1"
                              strokeWidth={1.5}
                              strokeDasharray="2 2"
                              connectNulls={true}
                              dot={{ fill: "#6366f1", r: 3 }}
                            />
                          )}
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  ) : (
                    <div className="py-16 text-center text-xs text-secondary-theme">
                      No historical empirical evaluation records found for student cohort.
                    </div>
                  )}
                </div>

                {/* 3. Observed Longitudinal Milestones Table */}
                {trends.observations.length > 0 && (
                  <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                    <div className="border-b border-white/5 pb-3">
                      <h3 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-purple-theme" />
                        <span>Empirical Observation Milestones</span>
                      </h3>
                      <p className="text-xs text-secondary-theme mt-0.5">
                        Chronological breakdown of observation events, evaluated learners, and segregated metrics by date
                      </p>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs">
                        <thead>
                          <tr className="border-b border-white/5 text-[10px] uppercase font-extrabold text-secondary-theme">
                            <th className="pb-3 pl-2">Observation Date</th>
                            <th className="pb-3 text-right">Assessment Score</th>
                            <th className="pb-3 text-right">Quiz Accuracy</th>
                            <th className="pb-3 text-right">Engagement</th>
                            <th className="pb-3 text-right">Total Events</th>
                            <th className="pb-3 text-right">Students</th>
                            <th className="pb-3 text-right">Assessments</th>
                            <th className="pb-3 text-right">Quizzes</th>
                            <th className="pb-3 text-right pr-2">Snapshots</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                          {trends.observations.map((pt, idx) => (
                            <tr key={pt.date || idx} className="hover:bg-white/[0.02]">
                              <td className="py-3 pl-2 font-mono font-semibold text-main-theme">{pt.date}</td>
                              <td className="py-3 text-right font-mono font-bold text-emerald-400">
                                {pt.meanAssessmentScore != null ? `${pt.meanAssessmentScore.toFixed(1)}%` : <span className="text-secondary-theme font-normal">N/A</span>}
                              </td>
                              <td className="py-3 text-right font-mono text-cyan-400">
                                {pt.meanQuizAccuracy != null ? `${pt.meanQuizAccuracy.toFixed(1)}%` : <span className="text-secondary-theme font-normal">N/A</span>}
                              </td>
                              <td className="py-3 text-right font-mono text-indigo-300">
                                {pt.meanEngagementScore != null ? `${pt.meanEngagementScore.toFixed(1)}%` : <span className="text-secondary-theme font-normal">N/A</span>}
                              </td>
                              <td className="py-3 text-right font-mono font-bold text-main-theme">{pt.observationCount}</td>
                              <td className="py-3 text-right font-mono text-purple-300">{pt.studentCount}</td>
                              <td className="py-3 text-right font-mono text-secondary-theme">{pt.assessmentCount}</td>
                              <td className="py-3 text-right font-mono text-secondary-theme">{pt.quizCount}</td>
                              <td className="py-3 text-right pr-2 font-mono text-secondary-theme">{pt.snapshotCount}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* 4. Research Data Sufficiency Banner */}
                <div className="glass-panel p-5 rounded-2xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-4">
                  <div className="p-2 rounded-xl bg-indigo-500/20 text-indigo-400 shrink-0 mt-0.5">
                    <Info className="h-5 w-5" />
                  </div>
                  <div className="space-y-1 text-xs">
                    <h4 className="font-bold text-indigo-300 uppercase tracking-wider">Research Data Sufficiency & Authenticity</h4>
                    <p className="text-sm font-semibold text-main-theme">
                      {trends.dataSufficiencyNote}
                    </p>
                    <p className="text-secondary-theme">
                      Research integrity rule: Historical trends plot only genuine evaluation records. Dates without observations are not interpolated, and no artificial baseline estimations are generated. Primary knowledge trend strictly reflects AssessmentResult records.
                    </p>
                  </div>
                </div>
              </>
            )}
          </div>
        )}

        {/* TAB 3: SUBJECT RESEARCH ANALYTICS */}
        {activeTab === "subjects" && (
          <div className="space-y-6">
            {/* Error State */}
            {subjectsError && (
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-3">
                  <ShieldAlert className="h-5 w-5 shrink-0" />
                  <span>{subjectsError}</span>
                </div>
                <button
                  onClick={() => loadSubjectAnalytics(filters)}
                  className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs transition-colors cursor-pointer"
                >
                  Retry
                </button>
              </div>
            )}

            {/* Loading Skeleton */}
            {subjectsLoading && !subjectsData && (
              <div className="space-y-6 animate-pulse">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-28 bg-white/5" />
                  ))}
                </div>
                <div className="glass-panel p-6 rounded-3xl border border-white/5 h-80 bg-white/5" />
                <div className="glass-panel p-6 rounded-2xl border border-white/5 h-64 bg-white/5" />
              </div>
            )}

            {/* Loaded Subject Analytics Content */}
            {subjectsData && (
              <>
                {/* 1. Summary Overview Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                  {/* Total Subjects */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Total Subjects</span>
                      <BookOpen className="h-4 w-4 text-purple-400" />
                    </div>
                    <div className="text-2xl font-black text-main-theme">
                      {subjectsData.totalSubjectsCount}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Persisted curriculum subjects catalogued.</p>
                  </div>

                  {/* Cohort Students Enrolled */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Cohort Enrolled</span>
                      <Users className="h-4 w-4 text-cyan-400" />
                    </div>
                    <div className="text-2xl font-black text-cyan-theme">
                      {subjectsData.totalEnrolledStudents}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Students registered across curriculum.</p>
                  </div>

                  {/* Verified Baseline Students */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Baseline Evaluated</span>
                      <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                    </div>
                    <div className="text-2xl font-black text-emerald-theme">
                      {subjectsData.evaluatedStudentsWithBaseline}
                    </div>
                    <p className="text-[10px] text-secondary-theme">Students with authentic diagnostic K0.</p>
                  </div>

                  {/* Verified Baseline Subjects */}
                  <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Baseline Subjects</span>
                      <GraduationCap className="h-4 w-4 text-indigo-400" />
                    </div>
                    <div className="text-2xl font-black text-indigo-300">
                      {subjectsData.subjects.filter((s) => s.studentsWithAuthenticBaseline > 0).length}
                      <span className="text-xs font-semibold text-secondary-theme ml-1.5">/ {subjectsData.totalSubjectsCount}</span>
                    </div>
                    <p className="text-[10px] text-secondary-theme">Subjects with authentic K0 diagnostic.</p>
                  </div>
                </div>

                {/* 2. Subject Performance Comparison Table */}
                <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/5 pb-4">
                    <div>
                      <h3 className="text-base font-extrabold text-main-theme flex items-center gap-2">
                        <BookOpen className="h-4 w-4 text-purple-400" />
                        <span>Subject Research Performance & Learning Gains</span>
                      </h3>
                      <p className="text-xs text-secondary-theme mt-0.5">
                        Comparison of authentic diagnostic baseline (K0), observed current knowledge (Kt), percentage point growth (ΔK), and normalized gain (g)
                      </p>
                    </div>
                    <span className="px-3 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 font-bold text-xs shrink-0">
                      {subjectsData.subjects.length} Subjects Evaluated
                    </span>
                  </div>

                  {subjectsData.subjects.length === 0 ? (
                    <div className="py-12 text-center text-secondary-theme text-xs">
                      No subject-level research records found.
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs">
                        <thead>
                          <tr className="border-b border-white/10 text-[10px] font-extrabold uppercase tracking-wider text-secondary-theme">
                            <th className="pb-3 pl-2">Subject / Code</th>
                            <th className="pb-3 text-center">Enrolled</th>
                            <th className="pb-3 text-center">Baseline N</th>
                            <th className="pb-3 text-right">Mean K0 (Baseline)</th>
                            <th className="pb-3 text-right">Mean Kt (Current)</th>
                            <th className="pb-3 text-right">Growth (ΔK)</th>
                            <th className="pb-3 text-right">Gain (g)</th>
                            <th className="pb-3 text-center">Weak Concepts</th>
                            <th className="pb-3 text-right">Quiz Acc.</th>
                            <th className="pb-3 text-right pr-2">Roadmap %</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                          {subjectsData.subjects.map((s, idx) => (
                            <tr key={s.subjectCode || idx} className="hover:bg-white/[0.02] transition-colors">
                              <td className="py-3.5 pl-2">
                                <div className="font-bold text-main-theme">{s.subjectName}</div>
                                {s.subjectCode && s.subjectCode !== s.subjectName && (
                                  <div className="text-[10px] font-mono text-purple-400 mt-0.5">{s.subjectCode}</div>
                                )}
                              </td>
                              <td className="py-3.5 text-center font-mono text-main-theme">
                                {s.studentsRepresented}
                              </td>
                              <td className="py-3.5 text-center">
                                {s.studentsWithAuthenticBaseline > 0 ? (
                                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-mono font-bold text-[11px]">
                                    N={s.studentsWithAuthenticBaseline}
                                  </span>
                                ) : (
                                  <span className="text-secondary-theme text-[11px]">None</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right font-mono font-semibold text-main-theme">
                                {s.meanBaselineKnowledge != null ? (
                                  `${s.meanBaselineKnowledge.toFixed(1)}%`
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right font-mono font-bold text-main-theme">
                                {s.meanCurrentKnowledge != null ? (
                                  `${s.meanCurrentKnowledge.toFixed(1)}%`
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right font-mono font-bold">
                                {s.meanGrowthPp != null ? (
                                  <span
                                    className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] ${
                                      s.meanGrowthPp > 0
                                        ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                                        : s.meanGrowthPp < 0
                                        ? "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                                        : "bg-white/5 text-secondary-theme border border-white/10"
                                    }`}
                                  >
                                    {s.meanGrowthPp > 0 ? "+" : ""}
                                    {s.meanGrowthPp.toFixed(1)} pp
                                  </span>
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right font-mono font-semibold">
                                {s.meanNormalizedGain != null ? (
                                  <span className="px-2 py-0.5 rounded-md bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 font-bold text-[11px]">
                                    {s.meanNormalizedGain.toFixed(2)}
                                  </span>
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                              <td className="py-3.5 text-center">
                                {s.weakConceptCount > 0 ? (
                                  <span className="px-2 py-0.5 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-400 font-mono font-bold text-[11px]">
                                    {s.weakConceptCount} / {s.conceptCount}
                                  </span>
                                ) : s.conceptCount > 0 ? (
                                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-mono font-bold text-[11px]">
                                    0 / {s.conceptCount}
                                  </span>
                                ) : (
                                  <span className="text-secondary-theme font-normal">0</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right font-mono text-cyan-400">
                                {s.meanQuizAccuracy != null ? (
                                  `${s.meanQuizAccuracy.toFixed(1)}%`
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                              <td className="py-3.5 text-right pr-2 font-mono text-purple-300">
                                {s.roadmapCompletionPercentage != null ? (
                                  `${s.roadmapCompletionPercentage.toFixed(1)}%`
                                ) : (
                                  <span className="text-secondary-theme font-normal">N/A</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* 3. Visual Charts Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Chart A: Subject Baseline vs Current Knowledge */}
                  <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                    <div className="flex items-center justify-between border-b border-white/5 pb-3">
                      <div>
                        <h4 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                          <BarChart2 className="h-4 w-4 text-purple-400" />
                          <span>Knowledge Comparison (K0 vs Kt)</span>
                        </h4>
                        <p className="text-xs text-secondary-theme mt-0.5">
                          Authentic Baseline vs Observed Current Knowledge by Subject
                        </p>
                      </div>
                    </div>

                    <div className="h-72 w-full pt-2">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart
                          data={subjectsData.subjects
                            .filter((s) => s.meanBaselineKnowledge != null || s.meanCurrentKnowledge != null)
                            .map((s) => ({
                              subject: s.subjectName.length > 14 ? `${s.subjectName.substring(0, 12)}...` : s.subjectName,
                              fullName: s.subjectName,
                              K0: s.meanBaselineKnowledge,
                              Kt: s.meanCurrentKnowledge
                            }))}
                          margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                          <XAxis
                            dataKey="subject"
                            stroke="#94a3b8"
                            fontSize={10}
                            tickLine={false}
                            interval={0}
                            angle={-15}
                            textAnchor="end"
                          />
                          <YAxis stroke="#94a3b8" fontSize={10} domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                          <Tooltip
                            content={({ active, payload }) => {
                              if (active && payload && payload.length) {
                                const data = payload[0].payload;
                                return (
                                  <div className="p-3 rounded-xl bg-slate-900/95 border border-white/10 shadow-2xl backdrop-blur-md text-xs space-y-1.5">
                                    <div className="font-extrabold text-main-theme">{data.fullName}</div>
                                    <div className="flex items-center justify-between gap-4 text-purple-300 font-mono">
                                      <span>Baseline (K0):</span>
                                      <span className="font-bold">{data.K0 != null ? `${data.K0.toFixed(1)}%` : "N/A"}</span>
                                    </div>
                                    <div className="flex items-center justify-between gap-4 text-emerald-400 font-mono">
                                      <span>Current (Kt):</span>
                                      <span className="font-bold">{data.Kt != null ? `${data.Kt.toFixed(1)}%` : "N/A"}</span>
                                    </div>
                                  </div>
                                );
                              }
                              return null;
                            }}
                          />
                          <Legend
                            wrapperStyle={{ fontSize: 11, paddingTop: 10 }}
                            formatter={(value) => (value === "K0" ? "Diagnostic Baseline (K0)" : "Current Knowledge (Kt)")}
                          />
                          <Bar dataKey="K0" fill="#8b5cf6" radius={[4, 4, 0, 0]} maxBarSize={30} />
                          <Bar dataKey="Kt" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={30} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  {/* Chart B: Mean Growth (ΔK pp) by Subject */}
                  <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                    <div className="flex items-center justify-between border-b border-white/5 pb-3">
                      <div>
                        <h4 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                          <TrendingUp className="h-4 w-4 text-emerald-400" />
                          <span>Observed Learning Growth (ΔK pp)</span>
                        </h4>
                        <p className="text-xs text-secondary-theme mt-0.5">
                          Mean percentage-point growth across evaluated students
                        </p>
                      </div>
                    </div>

                    <div className="h-72 w-full pt-2">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart
                          data={subjectsData.subjects
                            .filter((s) => s.meanGrowthPp != null)
                            .map((s) => ({
                              subject: s.subjectName.length > 14 ? `${s.subjectName.substring(0, 12)}...` : s.subjectName,
                              fullName: s.subjectName,
                              growth: s.meanGrowthPp,
                              gain: s.meanNormalizedGain
                            }))}
                          margin={{ top: 10, right: 10, left: -20, bottom: 20 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                          <XAxis
                            dataKey="subject"
                            stroke="#94a3b8"
                            fontSize={10}
                            tickLine={false}
                            interval={0}
                            angle={-15}
                            textAnchor="end"
                          />
                          <YAxis stroke="#94a3b8" fontSize={10} tickFormatter={(v) => `${v > 0 ? "+" : ""}${v} pp`} />
                          <Tooltip
                            content={({ active, payload }) => {
                              if (active && payload && payload.length) {
                                const data = payload[0].payload;
                                return (
                                  <div className="p-3 rounded-xl bg-slate-900/95 border border-white/10 shadow-2xl backdrop-blur-md text-xs space-y-1.5">
                                    <div className="font-extrabold text-main-theme">{data.fullName}</div>
                                    <div className="flex items-center justify-between gap-4 font-mono">
                                      <span className="text-secondary-theme">Mean Growth (ΔK):</span>
                                      <span
                                        className={`font-bold ${
                                          data.growth > 0 ? "text-emerald-400" : data.growth < 0 ? "text-rose-400" : "text-white"
                                        }`}
                                      >
                                        {data.growth > 0 ? "+" : ""}
                                        {data.growth?.toFixed(1)} pp
                                      </span>
                                    </div>
                                    {data.gain != null && (
                                      <div className="flex items-center justify-between gap-4 font-mono text-indigo-300">
                                        <span>Normalized Gain (g):</span>
                                        <span className="font-bold">{data.gain.toFixed(2)}</span>
                                      </div>
                                    )}
                                  </div>
                                );
                              }
                              return null;
                            }}
                          />
                          <Bar dataKey="growth" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={36}>
                            {subjectsData.subjects
                              .filter((s) => s.meanGrowthPp != null)
                              .map((entry, idx) => (
                                <Cell key={`cell-${idx}`} fill={(entry.meanGrowthPp || 0) >= 0 ? "#10b981" : "#f43f5e"} />
                              ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>

                {/* 4. Concept Mastery Breakdown & Activity Cards */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Concept Mastery Distribution Grid */}
                  <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                    <div className="flex items-center justify-between border-b border-white/5 pb-3">
                      <div>
                        <h4 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                          <Layers className="h-4 w-4 text-purple-400" />
                          <span>Concept Mastery Distribution</span>
                        </h4>
                        <p className="text-xs text-secondary-theme mt-0.5">
                          Evaluated concept competency tiers across enrolled cohort
                        </p>
                      </div>
                    </div>

                    <div className="space-y-3 pt-1">
                      {subjectsData.subjects
                        .filter((s) => s.conceptCount > 0)
                        .slice(0, 5)
                        .map((s) => {
                          const dist = s.masteryDistribution || {};
                          const total = (dist.BEGINNER || 0) + (dist.INTERMEDIATE || 0) + (dist.PROFICIENT || 0) + (dist.MASTER || 0);
                          return (
                            <div key={s.subjectCode} className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="font-bold text-xs text-main-theme">{s.subjectName}</span>
                                <span className="text-[10px] text-secondary-theme font-mono">
                                  {s.conceptCount} concepts • {s.weakConceptCount} weak
                                </span>
                              </div>
                              {total > 0 ? (
                                <div className="w-full h-2 rounded-full overflow-hidden flex bg-white/5">
                                  <div style={{ width: `${((dist.BEGINNER || 0) / total) * 100}%` }} className="bg-rose-500" title={`Beginner: ${dist.BEGINNER || 0}`} />
                                  <div style={{ width: `${((dist.INTERMEDIATE || 0) / total) * 100}%` }} className="bg-amber-500" title={`Intermediate: ${dist.INTERMEDIATE || 0}`} />
                                  <div style={{ width: `${((dist.PROFICIENT || 0) / total) * 100}%` }} className="bg-blue-500" title={`Proficient: ${dist.PROFICIENT || 0}`} />
                                  <div style={{ width: `${((dist.MASTER || 0) / total) * 100}%` }} className="bg-emerald-500" title={`Master: ${dist.MASTER || 0}`} />
                                </div>
                              ) : (
                                <div className="text-[10px] text-secondary-theme">No concept evaluation records</div>
                              )}
                              <div className="flex items-center justify-between text-[10px] font-mono text-secondary-theme pt-0.5">
                                <span className="text-rose-400">Beg: {dist.BEGINNER || 0}</span>
                                <span className="text-amber-400">Int: {dist.INTERMEDIATE || 0}</span>
                                <span className="text-blue-400">Prof: {dist.PROFICIENT || 0}</span>
                                <span className="text-emerald-400">Mst: {dist.MASTER || 0}</span>
                              </div>
                            </div>
                          );
                        })}
                      {subjectsData.subjects.filter((s) => s.conceptCount > 0).length === 0 && (
                        <div className="py-8 text-center text-secondary-theme text-xs">
                          No concept mastery records observed for catalogued subjects.
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Segregated Quiz & Roadmap Activity Breakdown */}
                  <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
                    <div className="flex items-center justify-between border-b border-white/5 pb-3">
                      <div>
                        <h4 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                          <Activity className="h-4 w-4 text-cyan-400" />
                          <span>Segregated Practice & Roadmap Analytics</span>
                        </h4>
                        <p className="text-xs text-secondary-theme mt-0.5">
                          Practice activity and roadmap progress recorded independently from knowledge scores
                        </p>
                      </div>
                    </div>

                    <div className="space-y-3 pt-1">
                      {subjectsData.subjects.slice(0, 5).map((s) => (
                        <div key={s.subjectCode} className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-xs text-main-theme">{s.subjectName}</span>
                            <span className="text-[10px] font-mono text-cyan-400 font-semibold">
                              Quiz Acc: {s.meanQuizAccuracy != null ? `${s.meanQuizAccuracy.toFixed(1)}%` : "N/A"}
                            </span>
                          </div>
                          <div className="grid grid-cols-2 gap-2 text-[11px] pt-1">
                            <div className="p-2 rounded-xl bg-white/5 space-y-0.5">
                              <span className="text-[9px] uppercase font-bold text-secondary-theme">Quiz Sessions</span>
                              <div className="font-mono font-bold text-main-theme">
                                {s.quizSessionsCount} sessions ({s.totalQuizQuestions} qns)
                              </div>
                            </div>
                            <div className="p-2 rounded-xl bg-white/5 space-y-0.5">
                              <span className="text-[9px] uppercase font-bold text-secondary-theme">Roadmap Progress</span>
                              <div className="font-mono font-bold text-purple-300">
                                {s.roadmapCompletionPercentage != null ? `${s.roadmapCompletionPercentage.toFixed(1)}%` : "No roadmap"}
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                {/* 5. Research Data Sufficiency Banner */}
                <div className="glass-panel p-5 rounded-2xl border border-indigo-500/20 bg-indigo-500/5 flex items-start gap-4">
                  <div className="p-2 rounded-xl bg-indigo-500/20 text-indigo-400 shrink-0 mt-0.5">
                    <Info className="h-5 w-5" />
                  </div>
                  <div className="space-y-1 text-xs">
                    <h4 className="font-bold text-indigo-300 uppercase tracking-wider">Research Data Sufficiency & Subject Baseline Methodology</h4>
                    <p className="text-sm font-semibold text-main-theme">
                      {subjectsData.dataSufficiencyNote}
                    </p>
                    <p className="text-secondary-theme">
                      Research integrity rule: Subject baseline K0 is extracted strictly from each student's earliest authentic diagnostic assessment containing evidence for that subject. Missing baselines remain null without fallback default interpolation.
                    </p>
                  </div>
                </div>
              </>
            )}
          </div>
        )}

        {/* TAB 4: STUDENT DIRECTORY */}
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
                  onClick={() => loadStudentDirectory(filters)}
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
