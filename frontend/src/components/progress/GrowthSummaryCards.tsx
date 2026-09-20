import React from "react";
import { StudentGrowthData } from "../../services/api";
import { TrendingUp, ArrowUpRight, Award, CheckCircle2, Layers } from "lucide-react";

export interface GrowthSummaryCardsProps {
  growthData?: StudentGrowthData | null;
  cumulativeGrowth?: number;
  recentGain?: number;
  conceptsImproved?: number;
  weakConceptsRemaining?: number;
}

export default function GrowthSummaryCards({
  growthData,
  cumulativeGrowth: directCumulativeGrowth,
  recentGain: directRecentGain,
  conceptsImproved: directConceptsImproved,
  weakConceptsRemaining: directWeakConceptsRemaining
}: GrowthSummaryCardsProps) {
  const cumulativeGrowth = growthData?.cumulativeGrowthPp ?? directCumulativeGrowth ?? 0;
  const recentGain = growthData?.recentGainPp ?? directRecentGain ?? 0;
  const conceptsImproved = growthData?.conceptsImprovedCount ?? directConceptsImproved ?? 0;
  const totalAssessedConcepts = growthData?.conceptsAssessedCount ?? 0;

  const isCumulativePositive = cumulativeGrowth >= 0;
  const isRecentPositive = recentGain >= 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Cumulative Growth */}
      <div className="glass-panel p-5 rounded-2xl border border-purple-500/20 bg-gradient-to-br from-purple-500/5 to-transparent space-y-1">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Cumulative Growth</span>
          <TrendingUp className="w-4 h-4 text-purple-400" />
        </div>
        <div className="flex items-baseline justify-between pt-1">
          <span className={`text-2xl font-black ${isCumulativePositive ? "text-emerald-400" : "text-rose-400"}`}>
            {isCumulativePositive ? `+${cumulativeGrowth.toFixed(1)}` : cumulativeGrowth.toFixed(1)} pp
          </span>
          <span className="text-[10px] text-slate-400">$K_t - K_0$ ($T_0$ Baseline)</span>
        </div>
      </div>

      {/* 2. Recent Gain */}
      <div className="glass-panel p-5 rounded-2xl border border-cyan-500/20 bg-gradient-to-br from-cyan-500/5 to-transparent space-y-1">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Recent Gain</span>
          <ArrowUpRight className="w-4 h-4 text-cyan-400" />
        </div>
        <div className="flex items-baseline justify-between pt-1">
          <span className={`text-2xl font-black ${isRecentPositive ? "text-cyan-400" : "text-rose-400"}`}>
            {isRecentPositive ? `+${recentGain.toFixed(1)}` : recentGain.toFixed(1)} pp
          </span>
          <span className="text-[10px] text-slate-400">$K_t - K_{"{t-1}"}$ (Latest Session)</span>
        </div>
      </div>

      {/* 3. Concepts Improved */}
      <div className="glass-panel p-5 rounded-2xl border border-emerald-500/20 bg-gradient-to-br from-emerald-500/5 to-transparent space-y-1">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Concepts Improved</span>
          <Award className="w-4 h-4 text-emerald-400" />
        </div>
        <div className="flex items-baseline justify-between pt-1">
          <span className="text-2xl font-black text-emerald-400">{conceptsImproved}</span>
          <span className="text-[10px] text-slate-400">
            {totalAssessedConcepts > 0 ? `out of ${totalAssessedConcepts} Assessed` : "Current > Baseline"}
          </span>
        </div>
      </div>

      {/* 4. Total Assessments */}
      <div className="glass-panel p-5 rounded-2xl border border-amber-500/20 bg-gradient-to-br from-amber-500/5 to-transparent space-y-1">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Assessment Evidence</span>
          <Layers className="w-4 h-4 text-amber-400" />
        </div>
        <div className="flex items-baseline justify-between pt-1">
          <span className="text-2xl font-black text-amber-400">
            {growthData?.totalAssessmentsCount ?? 0}
          </span>
          <span className="text-[10px] text-slate-400">Valid Trajectory Events</span>
        </div>
      </div>
    </div>
  );
}
