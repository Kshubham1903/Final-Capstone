import React, { useState, useEffect } from "react";
import { TrendingUp, Award, Sparkles, RefreshCw } from "lucide-react";
import { fetchLearningGain } from "../../services/api";

export interface LearningGainCardProps {
  profile?: any;
  className?: string;
}

export default function LearningGainCard({ profile, className = "" }: LearningGainCardProps) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const loadLearningGain = async () => {
    setLoading(true);
    try {
      const activeUserId = 
        profile?.userId || 
        profile?.id || 
        (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "anonymous_student" : "anonymous_student");

      const gainData = await fetchLearningGain(activeUserId);
      if (gainData) {
        setData(gainData);
      }
    } catch (err) {
      console.warn("Failed to load learning gain metrics:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLearningGain();
  }, [profile]);

  const overallGainPct = Math.round((data?.overallLearningGain ?? 0) * 100);
  const topicsList = data?.topics || [];

  return (
    <div className={`glass-panel p-5 rounded-2xl border border-white/5 space-y-4 bg-gradient-to-br from-purple-900/5 via-transparent to-emerald-900/5 ${className}`}>
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <TrendingUp className="h-5 w-5 text-emerald-theme" />
          <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">
            Learning Gain
          </h4>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadLearningGain}
            className="p-1 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
            title="Refresh Learning Gain"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {/* Overall Score Banner */}
      <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-between">
        <div>
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">
            Normalized Learning Gain (Hake's g)
          </span>
          <span className="text-2xl font-black text-emerald-theme">
            +{overallGainPct}%
          </span>
        </div>
        <div className="h-9 w-9 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-300">
          <Award className="h-5 w-5" />
        </div>
      </div>

      {/* Topics Breakdown List */}
      {loading ? (
        <div className="space-y-3 py-2 animate-pulse">
          {[1, 2].map((i) => (
            <div key={i} className="h-12 bg-white/10 rounded-xl" />
          ))}
        </div>
      ) : topicsList.length > 0 ? (
        <div className="space-y-2.5">
          {topicsList.map((t: any, idx: number) => {
            const prePct = Math.round((t.preScore ?? 0) * 100);
            const postPct = Math.round((t.postScore ?? 0) * 100);
            const gainPct = Math.round((t.learningGain ?? 0) * 100);

            return (
              <div key={idx} className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-extrabold text-main-theme truncate">{t.topic}</span>
                  <span className="font-black text-emerald-theme">+{gainPct}%</span>
                </div>
                <div className="flex items-center justify-between text-[11px] text-secondary-theme">
                  <span>Pre: <strong className="text-main-theme">{prePct}%</strong></span>
                  <span>Current: <strong className="text-purple-theme">{postPct}%</strong></span>
                  <span>Gain: <strong className="text-emerald-theme">+{gainPct}%</strong></span>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="p-3.5 rounded-xl bg-white/3 text-center space-y-1 border border-white/5">
          <Sparkles className="h-4 w-4 text-purple-theme/50 mx-auto" />
          <p className="text-[11px] text-secondary-theme">
            Complete baseline diagnostic tests to track topic learning gain.
          </p>
        </div>
      )}

    </div>
  );
}
