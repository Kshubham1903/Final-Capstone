import React, { useState, useEffect } from "react";
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
  ArrowDownRight
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
import { fetchAdminCohortAnalytics, AdminCohortAnalytics } from "../../../services/api";

export default function ResearchAnalyticsPage() {
  const [cohort, setCohort] = useState<AdminCohortAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAnalytics = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminCohortAnalytics();
      if (data) {
        setCohort(data);
      } else {
        setError("Unable to retrieve cohort research analytics. Please ensure you are logged in as an ADMIN.");
      }
    } catch (err) {
      setError("Network error while communicating with the cohort analytics service.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnalytics();
  }, []);

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
              Cohort-level learning growth and research metrics
            </p>
          </div>
          <button
            onClick={loadAnalytics}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
          >
            <RefreshCw className={`h-4 w-4 text-purple-theme ${loading ? "animate-spin" : ""}`} />
            <span>{loading ? "Refreshing Data..." : "Refresh Cohort Data"}</span>
          </button>
        </div>

        {/* Error Notification with Retry */}
        {error && (
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-sm">
            <div className="flex items-center gap-3">
              <ShieldAlert className="h-5 w-5 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={loadAnalytics}
              className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs transition-colors cursor-pointer"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {loading && !cohort && (
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

      </div>
    </Layout>
  );
}
