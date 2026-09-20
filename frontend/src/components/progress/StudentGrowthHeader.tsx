import React from "react";
import { TrendingUp, ArrowUpRight, Award, Clock } from "lucide-react";

export interface StudentGrowthHeaderProps {
  hasDiagnostic?: boolean;
  totalAssessments?: number;
  lastUpdated?: string | null;
  baselineKnowledge?: number | null;
  currentKnowledge?: number | null;
  cumulativeGrowth?: number | null;
}

export default function StudentGrowthHeader({
  hasDiagnostic = false,
  totalAssessments = 0,
  lastUpdated = null,
  baselineKnowledge = null,
  currentKnowledge = null,
  cumulativeGrowth = null
}: StudentGrowthHeaderProps) {
  const isPositive = (cumulativeGrowth || 0) >= 0;

  return (
    <div className="glass-panel p-6 rounded-2xl border border-purple-500/20 bg-gradient-to-r from-purple-500/10 via-slate-900/90 to-pink-500/10 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
      <div className="space-y-1.5">
        <div className="flex items-center space-x-2.5">
          <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20">
            <TrendingUp className="w-6 h-6 text-purple-400" />
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Student Progress &amp; Longitudinal Analytics
          </h1>
        </div>
        <p className="text-xs text-slate-400 leading-relaxed max-w-2xl">
          Continuous measurement of observed learning trajectory derived from genuine assessment evidence ($K_0 \rightarrow K_t$). No separate post-test required.
        </p>
      </div>

      <div className="flex items-center gap-3">
        {/* Baseline (K0) */}
        <div className="glass-panel px-4 py-3 rounded-xl border border-white/5 space-y-0.5 text-center min-w-[100px] bg-slate-900/60">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Baseline ($K_0$)</span>
          <span className="text-lg font-black text-white">
            {baselineKnowledge != null ? `${baselineKnowledge.toFixed(1)}%` : "N/A"}
          </span>
        </div>

        {/* Current (Kt) */}
        <div className="glass-panel px-4 py-3 rounded-xl border border-purple-500/30 space-y-0.5 text-center min-w-[100px] bg-purple-500/10">
          <span className="text-[10px] font-bold text-purple-300 uppercase tracking-wider block">Current ($K_t$)</span>
          <span className="text-lg font-black text-purple-300">
            {currentKnowledge != null ? `${currentKnowledge.toFixed(1)}%` : "N/A"}
          </span>
        </div>

        {/* Growth (+pp) */}
        {cumulativeGrowth != null && (
          <div className={`px-4 py-3 rounded-xl border space-y-0.5 text-center min-w-[110px] ${
            isPositive
              ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400"
              : "bg-rose-500/10 border-rose-500/30 text-rose-400"
          }`}>
            <span className="text-[10px] font-bold uppercase tracking-wider block opacity-90">Growth ($K_t - K_0$)</span>
            <div className="flex items-center justify-center space-x-0.5">
              <ArrowUpRight className="w-4 h-4" />
              <span className="text-lg font-black">
                {isPositive ? `+${cumulativeGrowth.toFixed(1)} pp` : `${cumulativeGrowth.toFixed(1)} pp`}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
