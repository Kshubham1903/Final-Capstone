import React from "react";
import { 
  BrainCircuit, Activity, TrendingUp, Award, Zap, HeartPulse, CheckCircle2, 
  Sparkles, Lightbulb, RefreshCw, AlertTriangle, ShieldCheck, WifiOff 
} from "lucide-react";
import { AILearningInsightsCardProps } from "./types";

type MetricStatus = "GOOD" | "MEDIUM" | "CRITICAL";

interface AIMetricItem {
  id: string;
  title: string;
  value: string;
  unit: string;
  icon: React.ElementType;
  status: MetricStatus;
  statusLabel: string;
  explanation: string;
}

export default function AILearningInsightsCard({
  profile,
  insights,
  predPerformanceLevel,
  isLoading = false,
  error = null,
  isBackendConnected = false,
  onRetry
}: AILearningInsightsCardProps) {

  // Helper for metric status styling (Green = GOOD/LOW, Amber = MEDIUM, Red = HIGH/CRITICAL)
  const getStatusBadge = (status: MetricStatus, label?: string) => {
    switch (status) {
      case "GOOD":
        return {
          colorClass: "text-emerald-theme bg-emerald-500/10 border-emerald-500/20",
          text: label || "GOOD"
        };
      case "MEDIUM":
        return {
          colorClass: "text-amber-theme bg-amber-500/10 border-amber-500/20",
          text: label || "MEDIUM"
        };
      case "CRITICAL":
        return {
          colorClass: "text-pink-theme bg-pink-500/10 border-pink-500/20",
          text: label || "HIGH RISK"
        };
    }
  };

  // 1. Loading Skeleton View
  if (isLoading) {
    return (
      <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-6 animate-pulse">
        <div className="flex items-center justify-between border-b border-white/5 pb-3">
          <div className="h-6 w-48 bg-white/10 rounded-lg" />
          <div className="h-5 w-24 bg-white/10 rounded-full" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4, 5, 6, 7].map((i) => (
            <div key={i} className="p-4 bg-white/5 rounded-xl space-y-3">
              <div className="h-4 w-24 bg-white/10 rounded" />
              <div className="h-8 w-16 bg-white/10 rounded" />
              <div className="h-3 w-32 bg-white/10 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  // 2. Error View
  if (error) {
    return (
      <div className="glass-panel p-6 rounded-2xl border border-pink-500/30 bg-pink-500/5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-pink-theme">
            <AlertTriangle className="h-5 w-5" />
            <h3 className="text-sm font-extrabold tracking-wide">AI Analytics Engine Unavailable</h3>
          </div>
          {onRetry && (
            <button
              onClick={onRetry}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-pink-500/20 hover:bg-pink-500/30 text-pink-300 text-xs font-bold transition-all cursor-pointer"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Retry Sync</span>
            </button>
          )}
        </div>
        <p className="text-xs text-secondary-theme leading-relaxed">
          {error || "Unable to retrieve real-time AI predictions. Please check server connectivity or try again."}
        </p>
      </div>
    );
  }

  // Extract real metric values from profile
  const sgi = profile?.studentGrowthIndex ?? 0.0;
  const riskLevel = profile?.academicRiskLevel || "LOW";
  const predCgpa = profile?.predictedCgpa ?? 0.0;
  const targetCgpa = profile?.targetCgpa ?? 0.0;
  const prodScore = profile?.productivityScore ?? 0;
  const lifeScore = profile?.lifestyleScore ?? 0;
  const consistScore = profile?.consistencyScore ?? 0;

  // Determine statuses for each of the 7 metrics
  const sgiStatus: MetricStatus = sgi >= 7.5 ? "GOOD" : sgi >= 5.0 ? "MEDIUM" : "CRITICAL";
  const riskStatus: MetricStatus = riskLevel === "LOW" ? "GOOD" : riskLevel === "MEDIUM" ? "MEDIUM" : "CRITICAL";
  const perfStatus: MetricStatus = predPerformanceLevel === "High" ? "GOOD" : predPerformanceLevel === "Medium" ? "MEDIUM" : "CRITICAL";
  const cgpaStatus: MetricStatus = (predCgpa >= targetCgpa && predCgpa > 0) || predCgpa >= 7.5 ? "GOOD" : predCgpa >= 6.0 ? "MEDIUM" : "CRITICAL";
  const prodStatus: MetricStatus = prodScore >= 75 ? "GOOD" : prodScore >= 50 ? "MEDIUM" : "CRITICAL";
  const lifeStatus: MetricStatus = lifeScore >= 75 ? "GOOD" : lifeScore >= 50 ? "MEDIUM" : "CRITICAL";
  const consistStatus: MetricStatus = consistScore >= 75 ? "GOOD" : consistScore >= 50 ? "MEDIUM" : "CRITICAL";

  // Build the array of 7 metrics
  const aiMetrics: AIMetricItem[] = [
    {
      id: "sgi",
      title: "Growth Index (SGI)",
      value: sgi.toFixed(1),
      unit: "/ 10.0",
      icon: BrainCircuit,
      status: sgiStatus,
      statusLabel: sgiStatus === "GOOD" ? "OPTIMAL" : sgiStatus === "MEDIUM" ? "MODERATE" : "ATTENTION",
      explanation: "Composite growth score across academics, mastery & lifestyle"
    },
    {
      id: "risk",
      title: "Academic Risk Level",
      value: `${riskLevel} RISK`,
      unit: "",
      icon: Activity,
      status: riskStatus,
      statusLabel: riskLevel,
      explanation: "Predictive early warning tier based on attendance & workload"
    },
    {
      id: "performance",
      title: "Performance Level",
      value: predPerformanceLevel,
      unit: "Tier",
      icon: TrendingUp,
      status: perfStatus,
      statusLabel: predPerformanceLevel.toUpperCase(),
      explanation: "ML classification based on quiz accuracy & response velocity"
    },
    {
      id: "cgpa",
      title: "Predicted CGPA",
      value: predCgpa > 0 ? predCgpa.toFixed(1) : "N/A",
      unit: targetCgpa > 0 ? `Target: ${targetCgpa.toFixed(1)}` : "",
      icon: Award,
      status: cgpaStatus,
      statusLabel: predCgpa >= targetCgpa && targetCgpa > 0 ? "ON TRACK" : "PROJECTED",
      explanation: "Python AI regression model projection of cumulative GPA"
    },
    {
      id: "productivity",
      title: "Productivity Score",
      value: `${prodScore}`,
      unit: "/ 100",
      icon: Zap,
      status: prodStatus,
      statusLabel: prodScore >= 75 ? "HIGH FOCUS" : prodScore >= 50 ? "BALANCED" : "NEEDS FOCUS",
      explanation: "Focus time ratio, interval rating & Pomodoro sessions"
    },
    {
      id: "lifestyle",
      title: "Lifestyle Score",
      value: `${lifeScore}`,
      unit: "/ 100",
      icon: HeartPulse,
      status: lifeStatus,
      statusLabel: lifeScore >= 75 ? "HEALTHY" : lifeScore >= 50 ? "BALANCED" : "STRESS ALERT",
      explanation: "Balance index for sleep regularity, screen time & stress"
    },
    {
      id: "consistency",
      title: "Consistency Score",
      value: `${consistScore}`,
      unit: "/ 100",
      icon: CheckCircle2,
      status: consistStatus,
      statusLabel: consistScore >= 75 ? "CONSISTENT" : consistScore >= 50 ? "STABLE" : "IRREGULAR",
      explanation: "Quiz frequency, streak retention & routine adherence"
    }
  ];

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-6 bg-gradient-to-br from-purple-900/5 via-transparent to-cyan-900/5">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-purple-500/10 flex items-center justify-center text-purple-theme border border-purple-500/20">
            <Sparkles className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-base font-extrabold tracking-wide text-main-theme flex items-center gap-2">
              <span>🤖 AI Learning Insights</span>
            </h3>
            <p className="text-[11px] text-secondary-theme">Real-time predictive analytics & study copilot guides</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Connection Mode Pill */}
          <div className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border ${
            isBackendConnected 
              ? "bg-emerald-500/10 text-emerald-theme border-emerald-500/20" 
              : "bg-amber-500/10 text-amber-theme border-amber-500/20"
          }`}>
            {isBackendConnected ? (
              <>
                <ShieldCheck className="h-3 w-3" />
                <span>AI Core Live</span>
              </>
            ) : (
              <>
                <WifiOff className="h-3 w-3" />
                <span>AI Local Engine</span>
              </>
            )}
          </div>

          {onRetry && (
            <button
              onClick={onRetry}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
              title="Refresh AI Insights"
            >
              <RefreshCw className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* 7 Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {aiMetrics.map((m) => {
          const Icon = m.icon;
          const badgeStyle = getStatusBadge(m.status, m.statusLabel);
          return (
            <div 
              key={m.id} 
              className="p-4 rounded-xl glass-panel border border-white/5 hover:border-purple-500/20 transition-all duration-200 flex flex-col justify-between space-y-3 group"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] uppercase font-extrabold text-secondary-theme tracking-wider">
                    {m.title}
                  </span>
                  <Icon className="h-4 w-4 text-purple-theme group-hover:scale-110 transition-transform" />
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-black text-main-theme tracking-tight">{m.value}</span>
                  {m.unit && <span className="text-[10px] text-secondary-theme font-semibold">{m.unit}</span>}
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-white/5">
                <span className={`inline-block text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-md border ${badgeStyle.colorClass}`}>
                  {badgeStyle.text}
                </span>
                <p className="text-[10px] text-secondary-theme leading-tight">{m.explanation}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* AI Study Recommendations Section */}
      <div className="pt-2 border-t border-white/5 space-y-3">
        <div className="flex items-center justify-between">
          <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider flex items-center gap-1.5">
            <Lightbulb className="h-4 w-4 text-cyan-theme" />
            <span>AI Copilot Action Guides</span>
          </h4>
          <span className="text-[10px] text-secondary-theme font-semibold">
            {insights.length} Active Suggestions
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {insights && insights.length > 0 ? (
            insights.map((insight, idx) => (
              <div 
                key={idx} 
                className="p-3.5 rounded-xl bg-white/5 border border-white/5 hover:border-purple-500/30 transition-all flex items-start gap-3 group"
              >
                <div className="h-7 w-7 rounded-lg bg-purple-500/10 flex items-center justify-center mt-0.5 shrink-0 group-hover:bg-purple-500/20 transition-colors">
                  <Sparkles className="h-3.5 w-3.5 text-purple-theme" />
                </div>
                <p className="text-xs text-main-theme leading-relaxed font-medium">{insight}</p>
              </div>
            ))
          ) : (
            <div className="col-span-2 p-5 bg-white/3 rounded-xl border border-white/5 text-center space-y-2">
              <Lightbulb className="h-6 w-6 text-cyan-theme opacity-50 mx-auto animate-pulse" />
              <h5 className="font-bold text-xs text-main-theme">No Active Action Guides</h5>
              <p className="text-[10px] text-secondary-theme leading-relaxed max-w-sm mx-auto">
                Complete daily habit logs or adaptive quizzes to generate custom learning guides.
              </p>
            </div>
          )}
        </div>
      </div>

    </div>
  );
}
