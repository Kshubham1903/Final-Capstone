import React, { useState, useEffect } from "react";
import { CheckCircle2, Clock, Activity, RefreshCw, ShieldCheck, Star, AlertTriangle } from "lucide-react";
import { fetchEvaluationMetrics } from "../../services/api";

export interface EvaluationMetricsCardProps {
  profile?: any;
  className?: string;
}

export default function EvaluationMetricsCard({ profile, className = "" }: EvaluationMetricsCardProps) {
  const [metrics, setMetrics] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const loadMetrics = async () => {
    setLoading(true);
    try {
      const activeUserId = 
        profile?.userId || 
        profile?.id || 
        (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "anonymous_student" : "anonymous_student");

      const data = await fetchEvaluationMetrics(activeUserId);
      if (data) {
        setMetrics(data);
      }
    } catch (err) {
      console.warn("Failed to load evaluation metrics:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMetrics();
  }, [profile]);

  const completionPct = Math.round((metrics?.completionRate ?? 0) * 100);
  const totalRecs = metrics?.totalRecommendations ?? 0;
  const completedRecs = metrics?.completedRecommendations ?? 0;
  const avgTime = metrics?.averageTimeToMasteryMinutes;
  const topicList = metrics?.topicTimeToMastery || [];

  const overallRetentionVal = metrics?.overallRetention;
  const retentionPct = overallRetentionVal != null ? Math.round(overallRetentionVal * 100) : null;
  const retentionList = metrics?.topicRetention || [];

  const avgSatisfaction = metrics?.averageSatisfaction;
  const satisfactionResponseCount = metrics?.satisfactionResponseCount ?? 0;

  const lastActivityAt = metrics?.lastActivityAt;
  const daysSinceLastActivity = metrics?.daysSinceLastActivity;
  const activityStatus = metrics?.activityStatus || "NO_ACTIVITY";
  const dropoutRisk = metrics?.dropoutRisk;
  const dropoutRate = metrics?.dropoutRate;

  return (
    <div className={`glass-panel p-5 rounded-2xl border border-white/5 space-y-4 bg-gradient-to-br from-cyan-900/5 via-transparent to-purple-900/5 ${className}`}>
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-cyan-theme" />
          <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">
            Evaluation Analytics
          </h4>
        </div>

        <button
          onClick={loadMetrics}
          className="p-1 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
          title="Refresh Metrics"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
        </button>
      </div>

      {/* Overview Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        
        {/* Completion Rate */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider flex items-center gap-1">
            <CheckCircle2 className="h-3 w-3 text-emerald-theme" /> Completion Rate
          </span>
          <div className="flex items-baseline gap-1">
            <span className="text-xl font-black text-emerald-theme">{completionPct}%</span>
          </div>
          <span className="text-[10px] text-secondary-theme block">
            {completedRecs} of {totalRecs} activities completed
          </span>
        </div>

        {/* Avg Time to Mastery */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider flex items-center gap-1">
            <Clock className="h-3 w-3 text-cyan-theme" /> Avg Time to Mastery
          </span>
          <div className="flex items-baseline gap-1">
            <span className="text-xl font-black text-cyan-theme">
              {avgTime != null ? `${avgTime}m` : "N/A"}
            </span>
          </div>
          <span className="text-[10px] text-secondary-theme block truncate">
            {avgTime != null ? "Target 80%+ threshold" : "Insufficient data"}
          </span>
        </div>

        {/* Student Satisfaction */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider flex items-center gap-1">
            <Star className="h-3 w-3 text-amber-400 fill-amber-400" /> Student Satisfaction
          </span>
          <div className="flex items-baseline gap-1">
            <span className="text-xl font-black text-amber-400">
              {avgSatisfaction != null ? `${Number(avgSatisfaction).toFixed(1)} / 5` : "N/A"}
            </span>
          </div>
          <span className="text-[10px] text-secondary-theme block truncate">
            {satisfactionResponseCount > 0 ? `Responses: ${satisfactionResponseCount}` : "No ratings yet"}
          </span>
        </div>
      </div>

      {/* Knowledge Retention Banner */}
      <div className="p-3.5 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-between">
        <div className="space-y-0.5">
          <span className="text-[10px] font-bold text-purple-theme uppercase tracking-wider flex items-center gap-1">
            <ShieldCheck className="h-3.5 w-3.5" /> Knowledge Retention
          </span>
          <span className="text-sm font-black text-purple-theme block">
            {retentionPct != null ? `${retentionPct}% Retention` : "Retention: Not enough reassessment data"}
          </span>
        </div>
        {retentionPct != null && (
          <span className="text-[10px] font-extrabold px-2 py-1 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
            Calculated
          </span>
        )}
      </div>

      {/* Inactivity Status & Dropout Risk Banner */}
      <div className={`p-3.5 rounded-xl border flex flex-col md:flex-row md:items-center justify-between gap-3 ${
        activityStatus === "ACTIVE" ? "bg-emerald-500/10 border-emerald-500/20" :
        activityStatus === "AT_RISK" ? "bg-amber-500/10 border-amber-500/20" :
        activityStatus === "INACTIVE" ? "bg-rose-500/10 border-rose-500/20" :
        "bg-white/5 border-white/10"
      }`}>
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-secondary-theme flex items-center gap-1">
              <AlertTriangle className="h-3.5 w-3.5 text-amber-400" /> Inactivity Status
            </span>
            <span className={`text-[10px] font-black uppercase px-2 py-0.5 rounded border ${
              activityStatus === "ACTIVE" ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30" :
              activityStatus === "AT_RISK" ? "bg-amber-500/20 text-amber-400 border-amber-500/30" :
              activityStatus === "INACTIVE" ? "bg-rose-500/20 text-rose-400 border-rose-500/30" :
              "bg-white/10 text-secondary-theme border-white/20"
            }`}>
              {activityStatus === "AT_RISK" ? "AT RISK" : activityStatus.replace("_", " ")}
            </span>
          </div>

          <p className="text-xs text-main-theme font-medium">
            {lastActivityAt ? (
              <>
                Last Activity: <span className="font-bold">{new Date(lastActivityAt).toLocaleDateString()}</span> ({daysSinceLastActivity} {daysSinceLastActivity === 1 ? "day" : "days"} ago)
              </>
            ) : (
              "No learning activity recorded yet"
            )}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {dropoutRisk != null && (
            <div className="text-right">
              <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">
                Dropout Risk
              </span>
              <span className={`text-base font-black ${
                dropoutRisk > 0.7 ? "text-rose-400" : dropoutRisk > 0.3 ? "text-amber-400" : "text-emerald-400"
              }`}>
                {Math.round(dropoutRisk * 100)}%
              </span>
            </div>
          )}

          {dropoutRate != null && (
            <div className="text-right pl-3 border-l border-white/10">
              <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">
                Cohort Dropout Rate
              </span>
              <span className="text-base font-black text-cyan-theme">
                {Math.round(dropoutRate * 100)}%
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Topic Time to Mastery & Retention Breakdown */}
      {loading ? (
        <div className="space-y-2 py-1 animate-pulse">
          <div className="h-10 bg-white/10 rounded-xl" />
        </div>
      ) : topicList.length > 0 ? (
        <div className="space-y-2">
          <span className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider block">
            Time to Mastery & Retention per Topic
          </span>
          <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
            {topicList.map((t: any, idx: number) => {
              const retObj = retentionList.find((r: any) => r.topic === t.topic);
              const retVal = retObj?.retentionScore != null ? Math.round(retObj.retentionScore * 100) : null;

              return (
                <div key={idx} className="p-2.5 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                  <div>
                    <span className="font-bold text-main-theme block truncate max-w-[140px]">{t.topic}</span>
                    <span className="text-[10px] text-secondary-theme">
                      {t.attempts} attempts • Retention: {retVal != null ? `${retVal}%` : "Pending"}
                    </span>
                  </div>

                  <div>
                    {t.status === "calculated" && t.timeToMasteryMinutes != null ? (
                      <span className="font-extrabold text-cyan-theme px-2 py-0.5 rounded bg-cyan-500/10 border border-cyan-500/20">
                        {t.timeToMasteryMinutes} min
                      </span>
                    ) : t.status === "in_progress" ? (
                      <span className="text-[10px] font-bold text-amber-400 px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/20">
                        In Progress
                      </span>
                    ) : (
                      <span className="text-[10px] text-secondary-theme px-2 py-0.5 rounded bg-white/5 border border-white/10">
                        Insufficient Data
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : null}

    </div>
  );
}
