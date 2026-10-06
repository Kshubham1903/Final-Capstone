import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../../components/Layout";
import {
  Users,
  Search,
  X,
  ChevronRight,
  GraduationCap,
  Minus,
  CheckCircle2,
  RefreshCw,
  ShieldAlert,
  FileSpreadsheet,
  FileText,
  Loader2
} from "lucide-react";
import {
  fetchAdminStudentDirectory,
  AdminStudentDirectoryDTO,
  exportAdminResearchAnalyticsExcel,
  exportAdminResearchAnalyticsPdf,
  downloadBlob
} from "../../../services/api";

export default function StudentDirectoryPage() {
  const navigate = useNavigate();

  // Directory State
  const [students, setStudents] = useState<AdminStudentDirectoryDTO[]>([]);
  const [directoryLoading, setDirectoryLoading] = useState(true);
  const [directoryError, setDirectoryError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterActivity, setFilterActivity] = useState<string>("ALL");

  // Export State
  const [isExportingExcel, setIsExportingExcel] = useState(false);
  const [isExportingPdf, setIsExportingPdf] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const loadData = async () => {
    setDirectoryLoading(true);
    setDirectoryError(null);
    try {
      const data = await fetchAdminStudentDirectory();
      if (data) {
        setStudents(data);
      } else {
        setDirectoryError("Unable to retrieve student directory. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err: any) {
      setDirectoryError(err?.message || "Failed to load student directory data.");
    } finally {
      setDirectoryLoading(false);
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
      downloadBlob(blob, `edupilot-student-directory-${dateStr}.xlsx`);
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
      downloadBlob(blob, `edupilot-student-directory-${dateStr}.pdf`);
    } catch (err: any) {
      console.error("PDF export error:", err);
      setExportError(err?.message || "Failed to generate PDF export. Please try again.");
    } finally {
      setIsExportingPdf(false);
    }
  };

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

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2.5">
              <Users className="h-8 w-8 text-purple-theme" />
              <span>Student Directory</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              View students and access individual learning analytics.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleExportExcel}
              disabled={isExportingExcel || isExportingPdf}
              className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-xs font-bold text-emerald-300 disabled:opacity-50 transition-all cursor-pointer w-fit"
              title="Export student directory as Excel spreadsheet (.xlsx)"
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
              title="Export student directory as PDF document (.pdf)"
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
              disabled={directoryLoading}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
            >
              <RefreshCw className={`h-4 w-4 text-purple-theme ${directoryLoading ? "animate-spin" : ""}`} />
              <span>{directoryLoading ? "Refreshing..." : "Refresh"}</span>
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
                className="absolute right-3 top-1/2 -translate-y-1/2 text-secondary-theme hover:text-main-theme cursor-pointer"
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
              onClick={loadData}
              className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs cursor-pointer"
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
                className="px-4 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-main-theme border border-white/10 cursor-pointer"
              >
                Clear Filters
              </button>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
}
