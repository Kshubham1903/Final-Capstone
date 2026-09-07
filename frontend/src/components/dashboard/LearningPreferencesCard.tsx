import React, { useState, useEffect } from "react";
import { Sliders, Sparkles, Brain, RefreshCw } from "lucide-react";
import { fetchStudentState } from "../../services/api";

export interface LearningPreferencesCardProps {
  profile?: any;
  className?: string;
}

export interface PreferenceScores {
  visual?: number | null;
  readingVerbal?: number | null;
  practicalKinesthetic?: number | null;
  sequentialGlobal?: number | null;
  feedbackPractice?: number | null;
}

export default function LearningPreferencesCard({ profile, className = "" }: LearningPreferencesCardProps) {
  const [preferences, setPreferences] = useState<PreferenceScores | null>(null);
  const [loading, setLoading] = useState(true);

  const loadPreferences = async () => {
    setLoading(true);
    try {
      const activeUserId = 
        profile?.userId || 
        profile?.id || 
        (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "anonymous_student" : "anonymous_student");

      const stateData = await fetchStudentState(activeUserId);
      if (stateData && stateData.preference) {
        setPreferences(stateData.preference);
      }
    } catch (err) {
      console.warn("Failed to load calculated preference dimensions:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPreferences();
  }, [profile]);

  const getPct = (val?: number | null): number => {
    if (val === null || val === undefined || isNaN(val)) return 0;
    const normalized = val > 1 ? val : val * 100;
    return Math.min(100, Math.max(0, Math.round(normalized)));
  };

  const pItems = [
    {
      key: "visual",
      label: "Visual",
      color: "from-purple-500 to-indigo-500",
      pct: getPct(preferences?.visual)
    },
    {
      key: "readingVerbal",
      label: "Reading / Verbal",
      color: "from-blue-500 to-cyan-500",
      pct: getPct(preferences?.readingVerbal)
    },
    {
      key: "practicalKinesthetic",
      label: "Practical / Kinesthetic",
      color: "from-emerald-500 to-teal-500",
      pct: getPct(preferences?.practicalKinesthetic)
    },
    {
      key: "sequentialGlobal",
      label: "Sequential / Global",
      color: "from-amber-500 to-orange-500",
      pct: getPct(preferences?.sequentialGlobal)
    },
    {
      key: "feedbackPractice",
      label: "Feedback / Practice",
      color: "from-pink-500 to-rose-500",
      pct: getPct(preferences?.feedbackPractice)
    }
  ];

  // Find highest dimension score
  const maxPct = Math.max(...pItems.map(i => i.pct));

  return (
    <div className={`glass-panel p-5 rounded-2xl border border-white/5 space-y-4 bg-gradient-to-br from-purple-900/5 via-transparent to-pink-900/5 ${className}`}>
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <Sliders className="h-5 w-5 text-purple-theme" />
          <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">
            Learning Preferences
          </h4>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadPreferences}
            className="p-1 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
            title="Refresh Preferences"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {/* Preferences List */}
      {loading ? (
        <div className="space-y-3 py-2 animate-pulse">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="space-y-1.5">
              <div className="h-3 w-32 bg-white/10 rounded" />
              <div className="h-2 w-full bg-white/10 rounded-full" />
            </div>
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {pItems.map((item) => {
            const isHighest = item.pct === maxPct && maxPct > 0;

            return (
              <div key={item.key} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-main-theme flex items-center gap-1.5">
                    <span>{item.label}</span>
                    {isHighest && (
                      <span className="text-[9px] font-extrabold px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-theme border border-purple-500/30 flex items-center gap-1">
                        <Sparkles className="h-2.5 w-2.5" /> Primary
                      </span>
                    )}
                  </span>
                  <span className={`font-black ${isHighest ? "text-purple-theme" : "text-secondary-theme"}`}>
                    {item.pct}%
                  </span>
                </div>

                {/* Progress Bar */}
                <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden border border-white/5 relative">
                  <div
                    className={`h-full rounded-full bg-gradient-to-r ${item.color} transition-all duration-500`}
                    style={{ width: `${item.pct}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Footer Info */}
      <div className="pt-2 border-t border-white/5 flex items-center gap-1.5 text-[10px] text-secondary-theme">
        <Brain className="h-3.5 w-3.5 text-purple-theme shrink-0" />
        <span>Calculated dynamically from Student State API</span>
      </div>

    </div>
  );
}
