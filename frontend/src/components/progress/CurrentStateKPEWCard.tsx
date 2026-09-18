import React from "react";
import { Cpu, Brain, Sliders, Activity, HeartPulse } from "lucide-react";

interface CurrentStateKPEWCardProps {
  stateData: any; // Raw or structured response from fetchStudentState /api/students/{userId}/state
}

export const CurrentStateKPEWCard: React.FC<CurrentStateKPEWCardProps> = ({ stateData }) => {
  if (!stateData) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-2">
          <Cpu className="w-5 h-5 text-indigo-600" />
          Multi-Dimensional Student State Vector [K, P, E, W]
        </h3>
        <p className="text-sm text-slate-500 italic">
          Student state vector currently loading or offline.
        </p>
      </div>
    );
  }

  const kState = stateData.knowledgeState || stateData.K || {};
  const pState = stateData.preferenceState || stateData.P || {};
  const eState = stateData.engagementState || stateData.E || {};
  const wState = stateData.wellbeingState || stateData.W || {};

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <Cpu className="w-5 h-5 text-indigo-600" />
            Multi-Dimensional Student State Vector $[K, P, E, W]$
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Real-time state vector maintained by <code className="text-indigo-600 bg-indigo-50 px-1 py-0.5 rounded">StudentStateService</code> (`GET /api/students/&#123;userId&#125;/state`).
          </p>
        </div>
        <span className="text-xs font-mono font-semibold px-2.5 py-1 bg-indigo-50 text-indigo-700 rounded-full border border-indigo-100">
          State Vector Active
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* K Vector: Knowledge */}
        <div className="p-4 rounded-xl border border-blue-200 bg-blue-50/40">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-blue-100 text-blue-700">
              <Brain className="w-4 h-4" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Knowledge State ($K$)</h4>
              <span className="text-[10px] text-slate-500 uppercase font-semibold">Mastery Representation</span>
            </div>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-blue-100/80 pb-1.5">
              <span className="text-slate-600">Tracked Concepts:</span>
              <span className="font-bold text-slate-800 font-mono">
                {kState.conceptMasteryMap ? Object.keys(kState.conceptMasteryMap).length : kState.trackedConceptsCount || 0}
              </span>
            </div>
            <div className="flex justify-between border-b border-blue-100/80 pb-1.5">
              <span className="text-slate-600">Mean Mastery:</span>
              <span className="font-bold text-blue-600 font-mono">
                {kState.overallMastery != null
                  ? `${(kState.overallMastery * (kState.overallMastery <= 1 ? 100 : 1)).toFixed(1)}%`
                  : "N/A"}
              </span>
            </div>
            <div className="flex justify-between pb-1">
              <span className="text-slate-600">Knowledge Stability:</span>
              <span className="font-bold text-slate-700 font-mono">
                {kState.stabilityScore != null ? `${(kState.stabilityScore * 100).toFixed(0)}%` : "High"}
              </span>
            </div>
          </div>
        </div>

        {/* P Vector: Preferences */}
        <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50/40">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-700">
              <Sliders className="w-4 h-4" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Preference State ($P$)</h4>
              <span className="text-[10px] text-slate-500 uppercase font-semibold">Adaptive Tuning</span>
            </div>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-emerald-100/80 pb-1.5">
              <span className="text-slate-600">Preferred Pace:</span>
              <span className="font-bold text-slate-800">
                {pState.learningPace || pState.pace || "MODERATE"}
              </span>
            </div>
            <div className="flex justify-between border-b border-emerald-100/80 pb-1.5">
              <span className="text-slate-600">Primary Format:</span>
              <span className="font-bold text-slate-800">
                {pState.preferredFormat || pState.format || "VISUAL / INTERACTIVE"}
              </span>
            </div>
            <div className="flex justify-between pb-1">
              <span className="text-slate-600">Target Difficulty:</span>
              <span className="font-bold text-emerald-700 font-mono">
                {pState.targetDifficulty || "ADAPTIVE"}
              </span>
            </div>
          </div>
        </div>

        {/* E Vector: Engagement */}
        <div className="p-4 rounded-xl border border-amber-200 bg-amber-50/40">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-amber-100 text-amber-700">
              <Activity className="w-4 h-4" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Engagement State ($E$)</h4>
              <span className="text-[10px] text-slate-500 uppercase font-semibold">Behavioral Velocity</span>
            </div>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-amber-100/80 pb-1.5">
              <span className="text-slate-600">Session Frequency:</span>
              <span className="font-bold text-slate-800 font-mono">
                {eState.sessionCount || eState.totalSessions || 1} Sessions
              </span>
            </div>
            <div className="flex justify-between border-b border-amber-100/80 pb-1.5">
              <span className="text-slate-600">Completion Rate:</span>
              <span className="font-bold text-amber-700 font-mono">
                {eState.completionRate != null ? `${(eState.completionRate * 100).toFixed(0)}%` : "100%"}
              </span>
            </div>
            <div className="flex justify-between pb-1">
              <span className="text-slate-600">Attention Index:</span>
              <span className="font-bold text-slate-800 font-mono">
                {eState.attentionIndex != null ? `${(eState.attentionIndex * 100).toFixed(0)}%` : "Optimal"}
              </span>
            </div>
          </div>
        </div>

        {/* W Vector: Wellbeing */}
        <div className="p-4 rounded-xl border border-rose-200 bg-rose-50/40">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-rose-100 text-rose-700">
              <HeartPulse className="w-4 h-4" />
            </div>
            <div>
              <h4 className="font-bold text-slate-800 text-sm">Wellbeing State ($W$)</h4>
              <span className="text-[10px] text-slate-500 uppercase font-semibold">Cognitive Capacity</span>
            </div>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-rose-100/80 pb-1.5">
              <span className="text-slate-600">Fatigue Risk:</span>
              <span className="font-bold text-slate-800">
                {wState.fatigueRisk || wState.fatigue || "LOW"}
              </span>
            </div>
            <div className="flex justify-between border-b border-rose-100/80 pb-1.5">
              <span className="text-slate-600">Stress Index:</span>
              <span className="font-bold text-rose-700 font-mono">
                {wState.stressIndex != null ? `${(wState.stressIndex * 100).toFixed(0)}%` : "Normal"}
              </span>
            </div>
            <div className="flex justify-between pb-1">
              <span className="text-slate-600">Recommended Break:</span>
              <span className="font-bold text-slate-800">
                {wState.recommendedBreak || "Not Required"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
