import React, { useState } from "react";
import {
  Users,
  Calendar,
  Layers,
  GraduationCap,
  BookOpen,
  Activity,
  CheckCircle2,
  AlertTriangle,
  Info,
  TrendingUp,
  ArrowRight,
  RotateCcw,
  Sparkles,
  BarChart3,
  Star
} from "lucide-react";
import {
  AdminResearchAnalyticsFilters,
  AdminCohortComparisonDTO,
  AdminCohortComparisonRequest,
  compareAdminCohorts
} from "../../services/api";

interface AdminCohortComparisonViewProps {
  availableBranches?: string[];
  availableSubjects?: Array<{ code: string; name: string }>;
}

export const AdminCohortComparisonView: React.FC<AdminCohortComparisonViewProps> = ({
  availableBranches = [],
  availableSubjects = []
}) => {
  // Shared observation window
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");

  // Cohort A Filters
  const [branchA, setBranchA] = useState<string>("");
  const [semesterA, setSemesterA] = useState<string>("");
  const [subjectA, setSubjectA] = useState<string>("");
  const [activityStatusA, setActivityStatusA] = useState<string>("");
  const [hasBaselineA, setHasBaselineA] = useState<string>("");

  // Cohort B Filters
  const [branchB, setBranchB] = useState<string>("");
  const [semesterB, setSemesterB] = useState<string>("");
  const [subjectB, setSubjectB] = useState<string>("");
  const [activityStatusB, setActivityStatusB] = useState<string>("");
  const [hasBaselineB, setHasBaselineB] = useState<string>("");

  // UI State
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [comparison, setComparison] = useState<AdminCohortComparisonDTO | null>(null);

  const handleReset = () => {
    setStartDate("");
    setEndDate("");
    setBranchA("");
    setSemesterA("");
    setSubjectA("");
    setActivityStatusA("");
    setHasBaselineA("");
    setBranchB("");
    setSemesterB("");
    setSubjectB("");
    setActivityStatusB("");
    setHasBaselineB("");
    setError(null);
    setComparison(null);
  };

  const handleCompare = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (startDate && endDate && startDate > endDate) {
      setError("Start date cannot be after end date.");
      return;
    }

    setError(null);
    setIsLoading(true);

    const cohortAFilters: AdminResearchAnalyticsFilters = {};
    if (branchA.trim() && branchA.trim().toUpperCase() !== "ALL") cohortAFilters.branch = branchA.trim();
    if (semesterA.trim() && semesterA.trim().toUpperCase() !== "ALL") {
      const num = parseInt(semesterA.trim(), 10);
      if (!isNaN(num)) cohortAFilters.semester = num;
    }
    if (subjectA.trim() && subjectA.trim().toUpperCase() !== "ALL") cohortAFilters.subjectCode = subjectA.trim();
    if (activityStatusA.trim() && activityStatusA.trim().toUpperCase() !== "ALL") cohortAFilters.activityStatus = activityStatusA.trim();
    if (hasBaselineA === "true") cohortAFilters.hasAuthenticBaseline = true;
    if (hasBaselineA === "false") cohortAFilters.hasAuthenticBaseline = false;

    const cohortBFilters: AdminResearchAnalyticsFilters = {};
    if (branchB.trim() && branchB.trim().toUpperCase() !== "ALL") cohortBFilters.branch = branchB.trim();
    if (semesterB.trim() && semesterB.trim().toUpperCase() !== "ALL") {
      const num = parseInt(semesterB.trim(), 10);
      if (!isNaN(num)) cohortBFilters.semester = num;
    }
    if (subjectB.trim() && subjectB.trim().toUpperCase() !== "ALL") cohortBFilters.subjectCode = subjectB.trim();
    if (activityStatusB.trim() && activityStatusB.trim().toUpperCase() !== "ALL") cohortBFilters.activityStatus = activityStatusB.trim();
    if (hasBaselineB === "true") cohortBFilters.hasAuthenticBaseline = true;
    if (hasBaselineB === "false") cohortBFilters.hasAuthenticBaseline = false;

    const req: AdminCohortComparisonRequest = {
      cohortA: cohortAFilters,
      cohortB: cohortBFilters,
      startDate: startDate.trim() || undefined,
      endDate: endDate.trim() || undefined
    };

    try {
      const res = await compareAdminCohorts(req);
      if (res) {
        setComparison(res);
      } else {
        setError("Failed to generate cohort comparison. Please ensure backend services are active.");
      }
    } catch (err: any) {
      setError(err?.message || "An unexpected error occurred during cohort comparison.");
    } finally {
      setIsLoading(false);
    }
  };

  const formatDiff = (val: number | null | undefined, suffix = "") => {
    if (val === null || val === undefined) return <span className="text-gray-400 font-mono">N/A</span>;
    const sign = val > 0 ? "+" : "";
    const colorClass =
      val > 0.0001
        ? "text-emerald-600 dark:text-emerald-400 font-semibold"
        : val < -0.0001
        ? "text-amber-600 dark:text-amber-400 font-semibold"
        : "text-gray-600 dark:text-gray-300";
    return <span className={colorClass}>{sign}{val.toFixed(2)}{suffix}</span>;
  };

  const formatRawScore = (val: number | null | undefined, suffix = "") => {
    if (val === null || val === undefined) return <span className="text-gray-400 font-mono">Unavailable</span>;
    return <span>{val.toFixed(2)}{suffix}</span>;
  };

  return (
    <div className="space-y-6">
      {/* Research Disclaimer Header Banner */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950/40 dark:to-indigo-950/30 border border-blue-200/80 dark:border-blue-900/60 rounded-xl p-4 sm:p-5 flex items-start gap-3">
        <Info className="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" />
        <div className="text-xs sm:text-sm text-blue-900 dark:text-blue-200/90 leading-relaxed">
          <p className="font-semibold mb-1">Descriptive Cohort Comparison</p>
          <p className="text-blue-800/80 dark:text-blue-300/80">
            Descriptive cohort comparison based on persisted diagnostic assessments and learning activity.
            Differences are observational and may reflect cohort composition, curriculum variation, missing data,
            or sample-size differences. They should not be interpreted as causal effects.
          </p>
        </div>
      </div>

      {/* Filter Matrix Formulation */}
      <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-2xl p-5 shadow-sm space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-gray-100 dark:border-gray-800">
          <div>
            <h3 className="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <BarChart3 className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              Cohort Comparison Setup
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
              Specify independent filters for Cohort A and Cohort B under a shared observation window.
            </p>
          </div>
          <button
            type="button"
            onClick={handleReset}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-300 bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors w-fit"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Reset Setup
          </button>
        </div>

        {/* Dual Cohort Form Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {/* Cohort A Column */}
          <div className="bg-blue-50/40 dark:bg-blue-950/20 border border-blue-200/60 dark:border-blue-900/40 rounded-xl p-4 space-y-3.5">
            <div className="flex items-center gap-2 text-blue-700 dark:text-blue-300 font-semibold text-sm">
              <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center font-bold">A</span>
              Cohort A Parameters
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Branch A */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <Layers className="w-3.5 h-3.5 text-blue-500" /> Branch
                </label>
                <select
                  value={branchA}
                  onChange={(e) => setBranchA(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">All Branches</option>
                  {availableBranches.map((b) => (
                    <option key={`a-branch-${b}`} value={b}>{b}</option>
                  ))}
                </select>
              </div>

              {/* Semester A */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <GraduationCap className="w-3.5 h-3.5 text-blue-500" /> Semester
                </label>
                <select
                  value={semesterA}
                  onChange={(e) => setSemesterA(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">All Semesters</option>
                  {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                    <option key={`a-sem-${s}`} value={s}>Semester {s}</option>
                  ))}
                </select>
              </div>

              {/* Subject A */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <BookOpen className="w-3.5 h-3.5 text-blue-500" /> Subject Filter
                </label>
                <select
                  value={subjectA}
                  onChange={(e) => setSubjectA(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">All Subjects</option>
                  {availableSubjects.map((subj) => (
                    <option key={`a-subj-${subj.code}`} value={subj.code}>
                      {subj.name} ({subj.code})
                    </option>
                  ))}
                </select>
              </div>

              {/* Activity Status A */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <Activity className="w-3.5 h-3.5 text-blue-500" /> Current Activity
                </label>
                <select
                  value={activityStatusA}
                  onChange={(e) => setActivityStatusA(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">All Activity Levels</option>
                  <option value="ACTIVE">Active (≤ 7 days)</option>
                  <option value="AT_RISK">At Risk (8–14 days)</option>
                  <option value="INACTIVE">Inactive (&gt; 14 days)</option>
                  <option value="NO_ACTIVITY">No Observed Activity</option>
                </select>
              </div>

              {/* Authentic Baseline A */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5 text-blue-500" /> Diagnostic Baseline Requirement
                </label>
                <select
                  value={hasBaselineA}
                  onChange={(e) => setHasBaselineA(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">All Students (With & Without Baseline)</option>
                  <option value="true">Authentic Baseline Only (K0 Verified)</option>
                  <option value="false">Without Authentic Baseline</option>
                </select>
              </div>
            </div>
          </div>

          {/* Cohort B Column */}
          <div className="bg-emerald-50/40 dark:bg-emerald-950/20 border border-emerald-200/60 dark:border-emerald-900/40 rounded-xl p-4 space-y-3.5">
            <div className="flex items-center gap-2 text-emerald-700 dark:text-emerald-300 font-semibold text-sm">
              <span className="w-6 h-6 rounded-full bg-emerald-600 text-white text-xs flex items-center justify-center font-bold">B</span>
              Cohort B Parameters
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Branch B */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <Layers className="w-3.5 h-3.5 text-emerald-500" /> Branch
                </label>
                <select
                  value={branchB}
                  onChange={(e) => setBranchB(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">All Branches</option>
                  {availableBranches.map((b) => (
                    <option key={`b-branch-${b}`} value={b}>{b}</option>
                  ))}
                </select>
              </div>

              {/* Semester B */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <GraduationCap className="w-3.5 h-3.5 text-emerald-500" /> Semester
                </label>
                <select
                  value={semesterB}
                  onChange={(e) => setSemesterB(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">All Semesters</option>
                  {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                    <option key={`b-sem-${s}`} value={s}>Semester {s}</option>
                  ))}
                </select>
              </div>

              {/* Subject B */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <BookOpen className="w-3.5 h-3.5 text-emerald-500" /> Subject Filter
                </label>
                <select
                  value={subjectB}
                  onChange={(e) => setSubjectB(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">All Subjects</option>
                  {availableSubjects.map((subj) => (
                    <option key={`b-subj-${subj.code}`} value={subj.code}>
                      {subj.name} ({subj.code})
                    </option>
                  ))}
                </select>
              </div>

              {/* Activity Status B */}
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <Activity className="w-3.5 h-3.5 text-emerald-500" /> Current Activity
                </label>
                <select
                  value={activityStatusB}
                  onChange={(e) => setActivityStatusB(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">All Activity Levels</option>
                  <option value="ACTIVE">Active (≤ 7 days)</option>
                  <option value="AT_RISK">At Risk (8–14 days)</option>
                  <option value="INACTIVE">Inactive (&gt; 14 days)</option>
                  <option value="NO_ACTIVITY">No Observed Activity</option>
                </select>
              </div>

              {/* Authentic Baseline B */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" /> Diagnostic Baseline Requirement
                </label>
                <select
                  value={hasBaselineB}
                  onChange={(e) => setHasBaselineB(e.target.value)}
                  className="w-full text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-2 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  <option value="">All Students (With & Without Baseline)</option>
                  <option value="true">Authentic Baseline Only (K0 Verified)</option>
                  <option value="false">Without Authentic Baseline</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Shared Observation Window & Action Bar */}
        <div className="bg-gray-50 dark:bg-gray-800/60 border border-gray-200 dark:border-gray-700/60 rounded-xl p-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-gray-700 dark:text-gray-300">
              <Calendar className="w-4 h-4 text-indigo-500" />
              Shared Observation Window:
            </div>
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-1.5 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-indigo-500 outline-none"
                placeholder="Start date"
              />
              <span className="text-gray-400 text-xs">to</span>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg px-2.5 py-1.5 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-indigo-500 outline-none"
                placeholder="End date"
              />
            </div>
            <span className="text-[11px] text-gray-500 dark:text-gray-400 hidden xl:inline">
              (K0 remains globally anchored)
            </span>
          </div>

          <button
            type="button"
            onClick={() => handleCompare()}
            disabled={isLoading}
            className="w-full md:w-auto inline-flex items-center justify-center gap-2 px-6 py-2 text-sm font-semibold text-white bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 hover:from-blue-700 hover:via-indigo-700 hover:to-purple-700 disabled:opacity-50 rounded-xl shadow-sm transition-all focus:ring-2 focus:ring-indigo-400 outline-none cursor-pointer"
          >
            {isLoading ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Calculating Cohorts...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                Run Cohort Comparison
              </>
            )}
          </button>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-xl p-3.5 flex items-center gap-2 text-xs sm:text-sm text-red-700 dark:text-red-300">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}
      </div>

      {/* Comparison Results */}
      {comparison && (
        <div className="space-y-6">
          {/* Data Sufficiency Alerts */}
          {comparison.dataSufficiency.warnings && comparison.dataSufficiency.warnings.length > 0 && (
            <div className="bg-amber-50 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/60 rounded-xl p-4 space-y-1.5">
              <div className="flex items-center gap-2 font-semibold text-xs text-amber-900 dark:text-amber-200">
                <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                Data Sufficiency &amp; Sample Size Warnings
              </div>
              <ul className="list-disc list-inside text-xs text-amber-800/90 dark:text-amber-300/90 space-y-0.5 pl-1">
                {comparison.dataSufficiency.warnings.map((warn, i) => (
                  <li key={`warn-${i}`}>{warn}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Side-by-Side Demographic & Sample Size Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {/* Total Enrolled Card */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl p-4 shadow-sm">
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Total Enrolled (N_total)</div>
              <div className="mt-2 flex items-baseline justify-between">
                <div>
                  <span className="text-xs text-blue-600 dark:text-blue-400 font-medium">A: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortA.metrics.totalEnrolled}</span>
                </div>
                <div>
                  <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">B: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortB.metrics.totalEnrolled}</span>
                </div>
              </div>
              <div className="mt-2 pt-2 border-t border-gray-100 dark:border-gray-800 text-xs text-gray-500 flex justify-between">
                <span>Diff (A − B):</span>
                <span>{comparison.differences.totalEnrolledDiff != null ? (comparison.differences.totalEnrolledDiff > 0 ? `+${comparison.differences.totalEnrolledDiff}` : comparison.differences.totalEnrolledDiff) : "N/A"}</span>
              </div>
            </div>

            {/* Evaluated Baseline Card */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl p-4 shadow-sm">
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Baseline Verified (N_baseline)</div>
              <div className="mt-2 flex items-baseline justify-between">
                <div>
                  <span className="text-xs text-blue-600 dark:text-blue-400 font-medium">A: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortA.metrics.baselineSampleSize}</span>
                </div>
                <div>
                  <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">B: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortB.metrics.baselineSampleSize}</span>
                </div>
              </div>
              <div className="mt-2 pt-2 border-t border-gray-100 dark:border-gray-800 text-xs text-gray-500 flex justify-between">
                <span>Diff (A − B):</span>
                <span>{comparison.differences.evaluatedCohortSizeDiff != null ? (comparison.differences.evaluatedCohortSizeDiff > 0 ? `+${comparison.differences.evaluatedCohortSizeDiff}` : comparison.differences.evaluatedCohortSizeDiff) : "N/A"}</span>
              </div>
            </div>

            {/* Current Knowledge Sample Card */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl p-4 shadow-sm">
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Current Knowledge (N_current)</div>
              <div className="mt-2 flex items-baseline justify-between">
                <div>
                  <span className="text-xs text-blue-600 dark:text-blue-400 font-medium">A: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortA.metrics.currentKnowledgeSampleSize}</span>
                </div>
                <div>
                  <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">B: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortB.metrics.currentKnowledgeSampleSize}</span>
                </div>
              </div>
              <div className="mt-2 pt-2 border-t border-gray-100 dark:border-gray-800 text-xs text-gray-500 flex justify-between">
                <span>Diff (A − B):</span>
                <span>{comparison.cohortA.metrics.currentKnowledgeSampleSize - comparison.cohortB.metrics.currentKnowledgeSampleSize > 0 ? `+${comparison.cohortA.metrics.currentKnowledgeSampleSize - comparison.cohortB.metrics.currentKnowledgeSampleSize}` : comparison.cohortA.metrics.currentKnowledgeSampleSize - comparison.cohortB.metrics.currentKnowledgeSampleSize}</span>
              </div>
            </div>

            {/* Learning Gain Sample Card */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl p-4 shadow-sm">
              <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Learning Gain (N_gain)</div>
              <div className="mt-2 flex items-baseline justify-between">
                <div>
                  <span className="text-xs text-blue-600 dark:text-blue-400 font-medium">A: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortA.metrics.learningGainSampleSize}</span>
                </div>
                <div>
                  <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">B: </span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{comparison.cohortB.metrics.learningGainSampleSize}</span>
                </div>
              </div>
              <div className="mt-2 pt-2 border-t border-gray-100 dark:border-gray-800 text-xs text-gray-500 flex justify-between">
                <span>Diff (A − B):</span>
                <span>{comparison.cohortA.metrics.learningGainSampleSize - comparison.cohortB.metrics.learningGainSampleSize > 0 ? `+${comparison.cohortA.metrics.learningGainSampleSize - comparison.cohortB.metrics.learningGainSampleSize}` : comparison.cohortA.metrics.learningGainSampleSize - comparison.cohortB.metrics.learningGainSampleSize}</span>
              </div>
            </div>
          </div>

          {/* Primary Comparative Metric Table */}
          <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 sm:p-5 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between">
              <h4 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                Descriptive Learning &amp; Performance Metrics
              </h4>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs sm:text-sm">
                <thead>
                  <tr className="bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-800 text-gray-600 dark:text-gray-400">
                    <th className="py-3 px-4 font-semibold">Metric</th>
                    <th className="py-3 px-4 font-semibold text-blue-600 dark:text-blue-400">Cohort A</th>
                    <th className="py-3 px-4 font-semibold text-emerald-600 dark:text-emerald-400">Cohort B</th>
                    <th className="py-3 px-4 font-semibold text-right">Difference (A − B)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-800 text-gray-800 dark:text-gray-200 font-medium">
                  {/* Mean Baseline Knowledge K0 */}
                  <tr>
                    <td className="py-3.5 px-4 font-normal">
                      <div className="font-semibold text-gray-900 dark:text-white">Mean Baseline Knowledge (K0)</div>
                      <div className="text-[11px] text-gray-500">Globally anchored diagnostic accuracy</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortA.metrics.meanBaselineKnowledge, "%")}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortB.metrics.meanBaselineKnowledge, "%")}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      {formatDiff(comparison.differences.baselineKnowledgeDiffPp, " pp")}
                    </td>
                  </tr>

                  {/* Mean Current Knowledge Kt */}
                  <tr>
                    <td className="py-3.5 px-4 font-normal">
                      <div className="font-semibold text-gray-900 dark:text-white">Mean Current Knowledge (Kt)</div>
                      <div className="text-[11px] text-gray-500">Mastery score aggregated over evaluated concepts</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortA.metrics.meanCurrentKnowledge, "%")}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortB.metrics.meanCurrentKnowledge, "%")}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      {formatDiff(comparison.differences.currentKnowledgeDiffPp, " pp")}
                    </td>
                  </tr>

                  {/* Mean Normalized Learning Gain g */}
                  <tr>
                    <td className="py-3.5 px-4 font-normal">
                      <div className="font-semibold text-gray-900 dark:text-white">Mean Normalized Learning Gain (g)</div>
                      <div className="text-[11px] text-gray-500">Hake gain: g = (Kt − K0) / (100 − K0)</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortA.metrics.meanNormalizedGain)}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {formatRawScore(comparison.cohortB.metrics.meanNormalizedGain)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      {formatDiff(comparison.differences.normalizedGainDiff)}
                    </td>
                  </tr>

                  {/* Satisfaction Average Rating */}
                  <tr>
                    <td className="py-3.5 px-4 font-normal">
                      <div className="font-semibold text-gray-900 dark:text-white">Student Satisfaction</div>
                      <div className="text-[11px] text-gray-500">Mean satisfaction rating (1–5 scale)</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {comparison.cohortA.metrics.satisfaction?.averageRating != null && comparison.cohortA.metrics.satisfaction.totalReviews > 0 ? (
                        <span>{comparison.cohortA.metrics.satisfaction.averageRating.toFixed(2)} ★ <span className="text-[11px] text-gray-400 font-normal">({comparison.cohortA.metrics.satisfaction.totalReviews})</span></span>
                      ) : (
                        <span className="text-gray-400 font-mono">No reviews</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold">
                      {comparison.cohortB.metrics.satisfaction?.averageRating != null && comparison.cohortB.metrics.satisfaction.totalReviews > 0 ? (
                        <span>{comparison.cohortB.metrics.satisfaction.averageRating.toFixed(2)} ★ <span className="text-[11px] text-gray-400 font-normal">({comparison.cohortB.metrics.satisfaction.totalReviews})</span></span>
                      ) : (
                        <span className="text-gray-400 font-mono">No reviews</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">
                      {formatDiff(comparison.differences.satisfactionDiff, " ★")}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Growth Distribution Stratification */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* Cohort A Growth Distribution */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-2xl p-5 shadow-sm space-y-4">
              <h4 className="text-sm font-bold text-blue-700 dark:text-blue-400 flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center font-bold">A</span>
                Cohort A: Growth Distribution
              </h4>
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-emerald-600 dark:text-emerald-400">Improved (Δ &gt; 0)</span>
                    <span className="font-mono">{comparison.cohortA.metrics.growthDistribution.improvedCount} students ({comparison.cohortA.metrics.growthDistribution.improvedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortA.metrics.growthDistribution.improvedPercentage))}%` }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-gray-600 dark:text-gray-400">Unchanged (Δ = 0)</span>
                    <span className="font-mono">{comparison.cohortA.metrics.growthDistribution.unchangedCount} students ({comparison.cohortA.metrics.growthDistribution.unchangedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-gray-400 dark:bg-gray-600 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortA.metrics.growthDistribution.unchangedPercentage))}%` }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-rose-600 dark:text-rose-400">Declined (Δ &lt; 0)</span>
                    <span className="font-mono">{comparison.cohortA.metrics.growthDistribution.declinedCount} students ({comparison.cohortA.metrics.growthDistribution.declinedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-rose-500 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortA.metrics.growthDistribution.declinedPercentage))}%` }} />
                  </div>
                </div>
              </div>
            </div>

            {/* Cohort B Growth Distribution */}
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-2xl p-5 shadow-sm space-y-4">
              <h4 className="text-sm font-bold text-emerald-700 dark:text-emerald-400 flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-emerald-600 text-white text-xs flex items-center justify-center font-bold">B</span>
                Cohort B: Growth Distribution
              </h4>
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-emerald-600 dark:text-emerald-400">Improved (Δ &gt; 0)</span>
                    <span className="font-mono">{comparison.cohortB.metrics.growthDistribution.improvedCount} students ({comparison.cohortB.metrics.growthDistribution.improvedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortB.metrics.growthDistribution.improvedPercentage))}%` }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-gray-600 dark:text-gray-400">Unchanged (Δ = 0)</span>
                    <span className="font-mono">{comparison.cohortB.metrics.growthDistribution.unchangedCount} students ({comparison.cohortB.metrics.growthDistribution.unchangedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-gray-400 dark:bg-gray-600 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortB.metrics.growthDistribution.unchangedPercentage))}%` }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-medium mb-1">
                    <span className="text-rose-600 dark:text-rose-400">Declined (Δ &lt; 0)</span>
                    <span className="font-mono">{comparison.cohortB.metrics.growthDistribution.declinedCount} students ({comparison.cohortB.metrics.growthDistribution.declinedPercentage.toFixed(1)}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-rose-500 rounded-full" style={{ width: `${Math.min(100, Math.max(0, comparison.cohortB.metrics.growthDistribution.declinedPercentage))}%` }} />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Methodology Note */}
          <div className="bg-gray-50 dark:bg-gray-800/40 border border-gray-200/80 dark:border-gray-800 rounded-xl p-4 text-xs text-gray-600 dark:text-gray-400 leading-relaxed">
            <span className="font-semibold text-gray-800 dark:text-gray-300">Methodology Note: </span>
            {comparison.methodologyNote}
          </div>
        </div>
      )}
    </div>
  );
};
