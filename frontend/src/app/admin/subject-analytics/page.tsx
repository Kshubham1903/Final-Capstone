import React, { useState, useEffect } from "react";
import Layout from "../../../components/Layout";
import {
  BookOpen,
  RefreshCw,
  ShieldAlert,
  Users,
  CheckCircle2,
  Activity,
  TrendingUp,
  BarChart2,
  GraduationCap,
  Layers,
  FileSpreadsheet,
  FileText,
  Loader2,
  Info
} from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  Legend,
  CartesianGrid
} from "recharts";
import {
  fetchAdminSubjectAnalytics,
  AdminCohortSubjectAnalyticsDTO,
  exportAdminResearchAnalyticsExcel,
  exportAdminResearchAnalyticsPdf,
  downloadBlob
} from "../../../services/api";

export default function SubjectAnalyticsPage() {
  // Subject Analytics State
  const [subjectsData, setSubjectsData] = useState<AdminCohortSubjectAnalyticsDTO | null>(null);
  const [subjectsLoading, setSubjectsLoading] = useState(true);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);

  // Export State
  const [isExportingExcel, setIsExportingExcel] = useState(false);
  const [isExportingPdf, setIsExportingPdf] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const loadData = async () => {
    setSubjectsLoading(true);
    setSubjectsError(null);
    try {
      const data = await fetchAdminSubjectAnalytics();
      if (data) {
        setSubjectsData(data);
      } else {
        setSubjectsError("Unable to retrieve subject analytics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err: any) {
      setSubjectsError(err?.message || "Failed to load subject analytics data.");
    } finally {
      setSubjectsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleExportExcel = async () => {
    if (isExportingExcel || isExportingPdf) return;
    setIsExportingExcel(true);
    setExportError(null);
    try {
      const blob = await exportAdminResearchAnalyticsExcel();
      const dateStr = new Date().toISOString().split("T")[0];
      downloadBlob(blob, `edupilot-subject-analytics-${dateStr}.xlsx`);
    } catch (err: any) {
      console.error("Excel export error:", err);
      setExportError(err?.message || "Failed to generate Excel export. Please try again.");
    } finally {
      setIsExportingExcel(false);
    }
  };

  const handleExportPdf = async () => {
    if (isExportingExcel || isExportingPdf) return;
    setIsExportingPdf(true);
    setExportError(null);
    try {
      const blob = await exportAdminResearchAnalyticsPdf();
      const dateStr = new Date().toISOString().split("T")[0];
      downloadBlob(blob, `edupilot-subject-analytics-${dateStr}.pdf`);
    } catch (err: any) {
      console.error("PDF export error:", err);
      setExportError(err?.message || "Failed to generate PDF export. Please try again.");
    } finally {
      setIsExportingPdf(false);
    }
  };

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2.5">
              <BookOpen className="h-8 w-8 text-purple-theme" />
              <span>Subject Analytics</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              Subject-wise learning performance and knowledge growth.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleExportExcel}
              disabled={isExportingExcel || isExportingPdf}
              className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-xs font-bold text-emerald-300 disabled:opacity-50 transition-all cursor-pointer w-fit"
              title="Export subject analytics as Excel spreadsheet (.xlsx)"
            >
              {isExportingExcel ? (
                <Loader2 className="h-4 w-4 text-emerald-400 animate-spin" />
              ) : (
                <FileSpreadsheet className="h-4 w-4 text-emerald-400" />
              )}
              <span>{isExportingExcel ? "Exporting..." : "Export Excel"}</span>
            </button>

            <button
              onClick={handleExportPdf}
              disabled={isExportingExcel || isExportingPdf}
              className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/30 text-xs font-bold text-indigo-300 disabled:opacity-50 transition-all cursor-pointer w-fit"
              title="Export subject analytics as PDF document (.pdf)"
            >
              {isExportingPdf ? (
                <Loader2 className="h-4 w-4 text-indigo-400 animate-spin" />
              ) : (
                <FileText className="h-4 w-4 text-indigo-400" />
              )}
              <span>{isExportingPdf ? "Generating..." : "Export PDF"}</span>
            </button>

            <button
              onClick={loadData}
              disabled={subjectsLoading}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
            >
              <RefreshCw className={`h-4 w-4 text-purple-theme ${subjectsLoading ? "animate-spin" : ""}`} />
              <span>{subjectsLoading ? "Refreshing..." : "Refresh"}</span>
            </button>
          </div>
        </div>

        {/* Export Error Notification */}
        {exportError && (
          <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 flex items-center justify-between gap-3 text-sm">
            <div className="flex items-center gap-3">
              <ShieldAlert className="h-5 w-5 shrink-0 text-rose-400" />
              <span>{exportError}</span>
            </div>
            <button
              onClick={() => setExportError(null)}
              className="text-xs text-rose-300 hover:text-white underline font-medium"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Error State */}
        {subjectsError && (
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-sm">
            <div className="flex items-center gap-3">
              <ShieldAlert className="h-5 w-5 shrink-0" />
              <span>{subjectsError}</span>
            </div>
            <button
              onClick={loadData}
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
          <div className="space-y-6">
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
          </div>
        )}
      </div>
    </Layout>
  );
}
