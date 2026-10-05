import React, { useState, useEffect } from "react";
import {
  Filter,
  Calendar,
  Layers,
  GraduationCap,
  BookOpen,
  Activity,
  CheckCircle2,
  RotateCcw,
  X,
  AlertCircle
} from "lucide-react";
import { AdminResearchAnalyticsFilters } from "../../services/api";

interface AdminResearchFilterBarProps {
  filters: AdminResearchAnalyticsFilters;
  onApplyFilters: (filters: AdminResearchAnalyticsFilters) => void;
  onClearFilters: () => void;
  isLoading?: boolean;
  availableBranches?: string[];
  availableSubjects?: Array<{ code: string; name: string }>;
}

export const AdminResearchFilterBar: React.FC<AdminResearchFilterBarProps> = ({
  filters,
  onApplyFilters,
  onClearFilters,
  isLoading = false,
  availableBranches = [],
  availableSubjects = []
}) => {
  // Local form state
  const [startDate, setStartDate] = useState<string>(filters.startDate || "");
  const [endDate, setEndDate] = useState<string>(filters.endDate || "");
  const [branch, setBranch] = useState<string>(filters.branch || "");
  const [semester, setSemester] = useState<string>(
    filters.semester !== undefined && filters.semester !== null ? String(filters.semester) : ""
  );
  const [subjectCode, setSubjectCode] = useState<string>(filters.subjectCode || "");
  const [activityStatus, setActivityStatus] = useState<string>(filters.activityStatus || "");
  const [hasAuthenticBaseline, setHasAuthenticBaseline] = useState<string>(
    filters.hasAuthenticBaseline === true
      ? "true"
      : filters.hasAuthenticBaseline === false
      ? "false"
      : ""
  );

  const [dateError, setDateError] = useState<string | null>(null);

  // Sync with prop changes
  useEffect(() => {
    setStartDate(filters.startDate || "");
    setEndDate(filters.endDate || "");
    setBranch(filters.branch || "");
    setSemester(
      filters.semester !== undefined && filters.semester !== null ? String(filters.semester) : ""
    );
    setSubjectCode(filters.subjectCode || "");
    setActivityStatus(filters.activityStatus || "");
    setHasAuthenticBaseline(
      filters.hasAuthenticBaseline === true
        ? "true"
        : filters.hasAuthenticBaseline === false
        ? "false"
        : ""
    );
    setDateError(null);
  }, [filters]);

  // Validate dates whenever startDate or endDate changes
  useEffect(() => {
    if (startDate && endDate && startDate > endDate) {
      setDateError("Start date cannot be after end date.");
    } else {
      setDateError(null);
    }
  }, [startDate, endDate]);

  const handleApply = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (startDate && endDate && startDate > endDate) {
      setDateError("Start date cannot be after end date.");
      return;
    }

    const applied: AdminResearchAnalyticsFilters = {};
    if (startDate.trim()) applied.startDate = startDate.trim();
    if (endDate.trim()) applied.endDate = endDate.trim();
    if (branch.trim() && branch.trim().toUpperCase() !== "ALL") applied.branch = branch.trim();
    if (semester.trim() && semester.trim().toUpperCase() !== "ALL") {
      const num = parseInt(semester.trim(), 10);
      if (!isNaN(num)) applied.semester = num;
    }
    if (subjectCode.trim() && subjectCode.trim().toUpperCase() !== "ALL") {
      applied.subjectCode = subjectCode.trim();
    }
    if (activityStatus.trim() && activityStatus.trim().toUpperCase() !== "ALL") {
      applied.activityStatus = activityStatus.trim();
    }
    if (hasAuthenticBaseline === "true") {
      applied.hasAuthenticBaseline = true;
    } else if (hasAuthenticBaseline === "false") {
      applied.hasAuthenticBaseline = false;
    }

    onApplyFilters(applied);
  };

  const handleClear = () => {
    setStartDate("");
    setEndDate("");
    setBranch("");
    setSemester("");
    setSubjectCode("");
    setActivityStatus("");
    setHasAuthenticBaseline("");
    setDateError(null);
    onClearFilters();
  };

  // Check active filter count
  const activeFilters: Array<{ id: string; label: string; onRemove: () => void }> = [];

  if (filters.startDate || filters.endDate) {
    let dateLabel = "Date: ";
    if (filters.startDate && filters.endDate) {
      dateLabel += `${filters.startDate} – ${filters.endDate}`;
    } else if (filters.startDate) {
      dateLabel += `From ${filters.startDate}`;
    } else {
      dateLabel += `Up to ${filters.endDate}`;
    }
    activeFilters.push({
      id: "date",
      label: dateLabel,
      onRemove: () => {
        const next = { ...filters };
        delete next.startDate;
        delete next.endDate;
        onApplyFilters(next);
      }
    });
  }

  if (filters.branch) {
    activeFilters.push({
      id: "branch",
      label: `Branch: ${filters.branch}`,
      onRemove: () => {
        const next = { ...filters };
        delete next.branch;
        onApplyFilters(next);
      }
    });
  }

  if (filters.semester !== undefined && filters.semester !== null) {
    activeFilters.push({
      id: "semester",
      label: `Semester: ${filters.semester}`,
      onRemove: () => {
        const next = { ...filters };
        delete next.semester;
        onApplyFilters(next);
      }
    });
  }

  if (filters.subjectCode) {
    activeFilters.push({
      id: "subject",
      label: `Subject: ${filters.subjectCode}`,
      onRemove: () => {
        const next = { ...filters };
        delete next.subjectCode;
        onApplyFilters(next);
      }
    });
  }

  if (filters.activityStatus) {
    activeFilters.push({
      id: "activity",
      label: `Activity: ${filters.activityStatus}`,
      onRemove: () => {
        const next = { ...filters };
        delete next.activityStatus;
        onApplyFilters(next);
      }
    });
  }

  if (filters.hasAuthenticBaseline !== undefined && filters.hasAuthenticBaseline !== null) {
    activeFilters.push({
      id: "baseline",
      label: filters.hasAuthenticBaseline ? "Baseline: Authentic Only" : "Baseline: Missing Baseline",
      onRemove: () => {
        const next = { ...filters };
        delete next.hasAuthenticBaseline;
        onApplyFilters(next);
      }
    });
  }

  // Deduplicate and prepare branches
  const branchList = Array.from(new Set(availableBranches.filter(Boolean)));

  return (
    <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-4 bg-white/5 backdrop-blur-md">
      {/* Title / Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-white/5">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-purple-theme" />
          <h3 className="text-xs uppercase font-extrabold tracking-wider text-main-theme">
            Research Filters & Population Slicing
          </h3>
        </div>
        <div className="flex items-center gap-2">
          {activeFilters.length > 0 && (
            <span className="px-2.5 py-0.5 rounded-full bg-purple-500/20 border border-purple-500/30 text-purple-300 text-[11px] font-bold">
              {activeFilters.length} Active {activeFilters.length === 1 ? "Filter" : "Filters"}
            </span>
          )}
        </div>
      </div>

      {/* Date Validation Error Alert */}
      {dateError && (
        <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 flex items-center gap-2.5 text-xs font-semibold">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{dateError}</span>
        </div>
      )}

      {/* Filter Inputs Grid */}
      <form onSubmit={handleApply} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          
          {/* 1. Start Date */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5 text-purple-theme" />
              <span>Start Date</span>
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            />
          </div>

          {/* 2. End Date */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5 text-purple-theme" />
              <span>End Date</span>
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            />
          </div>

          {/* 3. Branch */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <Layers className="h-3.5 w-3.5 text-purple-theme" />
              <span>Branch</span>
            </label>
            <select
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            >
              <option value="">All Branches</option>
              {branchList.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </div>

          {/* 4. Semester */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <GraduationCap className="h-3.5 w-3.5 text-purple-theme" />
              <span>Semester</span>
            </label>
            <select
              value={semester}
              onChange={(e) => setSemester(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            >
              <option value="">All Semesters</option>
              {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                <option key={s} value={String(s)}>
                  Semester {s}
                </option>
              ))}
            </select>
          </div>

          {/* 5. Subject */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <BookOpen className="h-3.5 w-3.5 text-purple-theme" />
              <span>Subject</span>
            </label>
            <select
              value={subjectCode}
              onChange={(e) => setSubjectCode(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            >
              <option value="">All Subjects</option>
              {availableSubjects.map((sub) => (
                <option key={sub.code || sub.name} value={sub.code || sub.name}>
                  {sub.code ? `${sub.code} — ${sub.name}` : sub.name}
                </option>
              ))}
            </select>
          </div>

          {/* 6. Activity Status */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <Activity className="h-3.5 w-3.5 text-purple-theme" />
              <span>Activity Status</span>
            </label>
            <select
              value={activityStatus}
              onChange={(e) => setActivityStatus(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            >
              <option value="">All Activity Levels</option>
              <option value="ACTIVE">ACTIVE (≤ 7 days)</option>
              <option value="AT_RISK">AT_RISK (8–14 days)</option>
              <option value="INACTIVE">INACTIVE (&gt; 14 days)</option>
              <option value="NO_ACTIVITY">NO_ACTIVITY (None)</option>
            </select>
          </div>

          {/* 7. Authentic Baseline */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-secondary-theme flex items-center gap-1.5">
              <CheckCircle2 className="h-3.5 w-3.5 text-purple-theme" />
              <span>Baseline Availability</span>
            </label>
            <select
              value={hasAuthenticBaseline}
              onChange={(e) => setHasAuthenticBaseline(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-main-theme text-xs font-medium focus:outline-none focus:border-purple-500 transition-colors"
            >
              <option value="">All Students</option>
              <option value="true">Has Authentic Baseline</option>
              <option value="false">No Authentic Baseline</option>
            </select>
          </div>

          {/* 8. Action Buttons */}
          <div className="flex items-end gap-2.5">
            <button
              type="submit"
              disabled={isLoading || Boolean(dateError)}
              className="flex-1 flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white font-bold text-xs transition-all shadow-lg shadow-purple-600/20 cursor-pointer h-[38px]"
            >
              <Filter className="h-3.5 w-3.5" />
              <span>{isLoading ? "Applying..." : "Apply Filters"}</span>
            </button>

            <button
              type="button"
              onClick={handleClear}
              disabled={isLoading}
              className="flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-secondary-theme hover:text-main-theme font-bold text-xs transition-colors cursor-pointer h-[38px]"
              title="Clear all filters"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              <span>Reset</span>
            </button>
          </div>

        </div>
      </form>

      {/* Active Filter Badges */}
      {activeFilters.length > 0 && (
        <div className="pt-3 border-t border-white/5 flex flex-wrap items-center gap-2">
          <span className="text-[11px] font-extrabold uppercase tracking-wider text-secondary-theme">
            Active Filters:
          </span>
          {activeFilters.map((af) => (
            <span
              key={af.id}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-purple-500/10 border border-purple-500/20 text-purple-300 text-xs font-semibold"
            >
              <span>{af.label}</span>
              <button
                type="button"
                onClick={af.onRemove}
                className="hover:text-white transition-colors cursor-pointer"
                title={`Remove ${af.label}`}
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
          <button
            type="button"
            onClick={handleClear}
            className="text-xs font-bold text-purple-400 hover:text-purple-300 underline underline-offset-2 ml-1 cursor-pointer"
          >
            Clear all
          </button>
        </div>
      )}
    </div>
  );
};
