import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchStudentGrowth, StudentGrowthData } from "../../services/api";
import { TrendingUp, ArrowRight, Award, CheckCircle2, PlayCircle, Loader2 } from "lucide-react";

interface StudentGrowthCardProps {
  userId: string;
}

export default function StudentGrowthCard({ userId }: StudentGrowthCardProps) {
  const [growthData, setGrowthData] = useState<StudentGrowthData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    fetchStudentGrowth(userId)
      .then((data) => {
        setGrowthData(data);
      })
      .catch((err) => console.error("Error fetching growth data:", err))
      .finally(() => setLoading(false));
  }, [userId]);

  if (loading) {
    return (
      <div className="glass-panel p-5 rounded-2xl border border-white/5 bg-slate-900/40 animate-pulse flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Loader2 className="w-5 h-5 text-indigo-400 animate-spin" />
          <span className="text-xs text-slate-400 font-medium">Loading Growth Snapshot...</span>
        </div>
      </div>
    );
  }

  if (!growthData) return null;

  const hasDiag = growthData.hasDiagnostic;
  const growthPp = growthData.cumulativeGrowthPp ?? 0;
  const isPositive = growthPp >= 0;

  return (
    <div className="glass-panel p-6 rounded-2xl border border-indigo-500/20 bg-gradient-to-r from-indigo-950/40 via-purple-950/20 to-slate-900/60 shadow-xl space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              Student Growth Snapshot
              {hasDiag && (
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  $K_0 \rightarrow K_t$ Active
                </span>
              )}
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Continuous learning trajectory measured from authentic assessment evidence.
            </p>
          </div>
        </div>

        <Link
          to="/progress"
          className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white rounded-xl text-xs font-extrabold tracking-wide flex items-center justify-center gap-1.5 transition-all shadow-md hover:shadow-indigo-500/20 shrink-0"
        >
          View Detailed Progress
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

      {!hasDiag ? (
        <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs text-amber-300">
            <PlayCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>Complete your initial diagnostic test to activate longitudinal growth tracking.</span>
          </div>
          <Link
            to="/dashboard/quizzes"
            className="text-xs font-bold text-amber-300 hover:text-amber-200 underline shrink-0"
          >
            Start Diagnostic
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
          <div className="p-3 rounded-xl bg-white/5 border border-white/5">
            <span className="text-[10px] text-slate-400 uppercase font-semibold block">Baseline ($K_0$)</span>
            <span className="text-lg font-bold text-white">
              {growthData.baselineOverallAccuracy != null ? `${growthData.baselineOverallAccuracy.toFixed(1)}%` : "N/A"}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-white/5 border border-white/5">
            <span className="text-[10px] text-slate-400 uppercase font-semibold block">Current ($K_t$)</span>
            <span className="text-lg font-bold text-indigo-400">
              {growthData.currentOverallAccuracy != null ? `${growthData.currentOverallAccuracy.toFixed(1)}%` : "N/A"}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-white/5 border border-white/5">
            <span className="text-[10px] text-slate-400 uppercase font-semibold block">Cumulative Growth</span>
            <span className={`text-lg font-bold ${isPositive ? "text-emerald-400" : "text-rose-400"}`}>
              {isPositive ? `+${growthPp.toFixed(1)} pp` : `${growthPp.toFixed(1)} pp`}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-white/5 border border-white/5">
            <span className="text-[10px] text-slate-400 uppercase font-semibold block">Concepts Improved</span>
            <span className="text-lg font-bold text-emerald-400 flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              {growthData.conceptsImprovedCount} / {growthData.conceptsAssessedCount}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
