import React from "react";
import { ConceptGrowthDetail, StudentGrowthData } from "../../services/api";
import { Lightbulb, Target, AlertTriangle, Compass, CheckCircle2 } from "lucide-react";

interface AdaptiveInsightsSectionProps {
  growthData: StudentGrowthData;
}

export const AdaptiveInsightsSection: React.FC<AdaptiveInsightsSectionProps> = ({ growthData }) => {
  const concepts = growthData?.conceptMasteries || [];

  // Identify weak concepts (<60% current accuracy) sorted ascending by accuracy
  const weakConcepts = concepts
    .filter((c) => c.currentAccuracy < 60)
    .sort((a, b) => a.currentAccuracy - b.currentAccuracy);

  // Identify top improved concepts
  const topImproved = concepts
    .filter((c) => c.isImproved && c.growthPp != null && c.growthPp > 0)
    .sort((a, b) => (b.growthPp || 0) - (a.growthPp || 0))
    .slice(0, 3);

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <Lightbulb className="w-5 h-5 text-amber-500" />
            Adaptive Learning Insights & Guidance
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Data-driven recommendations derived from concept-level performance gaps.
          </p>
        </div>
        <span className="text-xs font-semibold px-2.5 py-1 bg-amber-50 text-amber-700 rounded-full border border-amber-200 flex items-center gap-1">
          <Compass className="w-3.5 h-3.5" />
          Active Recommendations
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Priority Focus Areas (Weak Concepts) */}
        <div className="p-5 rounded-xl border border-amber-200/80 bg-amber-50/30">
          <h4 className="font-bold text-slate-800 text-sm flex items-center gap-2 mb-3">
            <AlertTriangle className="w-4 h-4 text-amber-600" />
            Priority Focus Areas ({weakConcepts.length})
          </h4>

          {weakConcepts.length === 0 ? (
            <div className="flex items-start gap-3 p-3 bg-white rounded-lg border border-slate-200/60">
              <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-bold text-slate-800">High Concept Mastery Achieved</p>
                <p className="text-xs text-slate-500 mt-0.5">
                  No concepts are currently flagged below 60% accuracy. Continue regular adaptive practice sessions to maintain mastery stability.
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <p className="text-xs text-slate-600">
                The following concepts demonstrate current accuracy below target threshold (&lt;60%) and require targeted review:
              </p>
              {weakConcepts.slice(0, 4).map((c) => (
                <div
                  key={c.conceptId}
                  className="flex items-center justify-between p-3 bg-white rounded-lg border border-slate-200/80 hover:border-amber-300 transition-colors"
                >
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">{c.conceptName}</span>
                    <span className="text-[10px] text-slate-400 font-mono">{c.subject} • {c.conceptId}</span>
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-bold text-amber-700 font-mono block">
                      {c.currentAccuracy.toFixed(1)}%
                    </span>
                    <span className="text-[10px] text-slate-500 font-medium">Needs Practice</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Learning Trajectory Highlights & Next Steps */}
        <div className="p-5 rounded-xl border border-blue-200/80 bg-blue-50/30">
          <h4 className="font-bold text-slate-800 text-sm flex items-center gap-2 mb-3">
            <Target className="w-4 h-4 text-blue-600" />
            Recommended Adaptation Strategy
          </h4>

          <div className="space-y-3">
            <div className="p-3 bg-white rounded-lg border border-slate-200/80">
              <span className="text-xs font-bold text-slate-800 block mb-1">
                Target Quiz Difficulty
              </span>
              <p className="text-xs text-slate-600">
                {weakConcepts.length > 0
                  ? "Recommend EASY to INTERMEDIATE difficulty practice quizzes focusing on foundational concepts before attempting hard diagnostic items."
                  : "Recommend INTERMEDIATE to HARD difficulty challenge items to accelerate depth of mastery."}
              </p>
            </div>

            {topImproved.length > 0 && (
              <div className="p-3 bg-white rounded-lg border border-slate-200/80">
                <span className="text-xs font-bold text-slate-800 block mb-1">
                  Strongest Growth Areas
                </span>
                <div className="flex flex-wrap gap-2 mt-1">
                  {topImproved.map((c) => (
                    <span
                      key={c.conceptId}
                      className="text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200"
                    >
                      {c.conceptName} (+{c.growthPp?.toFixed(1)} pp)
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
