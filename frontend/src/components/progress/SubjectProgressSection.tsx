import React from "react";
import { SubjectProgressData } from "../../services/api";
import { BookOpen, TrendingUp, TrendingDown, Minus, Layers } from "lucide-react";

interface SubjectProgressSectionProps {
  subjects: SubjectProgressData[];
}

export const SubjectProgressSection: React.FC<SubjectProgressSectionProps> = ({ subjects }) => {
  if (!subjects || subjects.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-3">
          <BookOpen className="w-5 h-5 text-blue-600" />
          Subject-Level Mastery & Growth
        </h3>
        <p className="text-sm text-slate-500 italic">
          No subject-level assessment data is available yet. Complete diagnostics or practice quizzes to begin tracking subject growth.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-blue-600" />
            Subject-Level Mastery & Growth
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Dynamic subject progress calculated strictly from authentic assessment evidence ($K_0 \rightarrow K_t$).
          </p>
        </div>
        <span className="text-xs font-semibold px-2.5 py-1 bg-blue-50 text-blue-700 rounded-full border border-blue-100 flex items-center gap-1">
          <Layers className="w-3.5 h-3.5" />
          {subjects.length} Active {subjects.length === 1 ? "Subject" : "Subjects"}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {subjects.map((subj) => {
          const isPositive = subj.growthPp > 0;
          const isNegative = subj.growthPp < 0;

          return (
            <div
              key={subj.subject}
              className="p-5 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-slate-50 transition-colors flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-bold text-slate-800 text-base flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-blue-600"></span>
                    {subj.subject}
                  </h4>
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold ${
                      isPositive
                        ? "bg-emerald-100 text-emerald-800"
                        : isNegative
                        ? "bg-rose-100 text-rose-800"
                        : "bg-slate-200 text-slate-700"
                    }`}
                  >
                    {isPositive ? (
                      <TrendingUp className="w-3 h-3 text-emerald-600" />
                    ) : isNegative ? (
                      <TrendingDown className="w-3 h-3 text-rose-600" />
                    ) : (
                      <Minus className="w-3 h-3 text-slate-500" />
                    )}
                    {subj.growthPp > 0 ? `+${subj.growthPp.toFixed(1)} pp` : `${subj.growthPp.toFixed(1)} pp`}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 mb-4 bg-white p-3 rounded-lg border border-slate-100">
                  <div>
                    <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                      Baseline ($K_0$)
                    </span>
                    <span className="text-base font-bold text-slate-700">
                      {subj.baselineAccuracy.toFixed(1)}%
                    </span>
                  </div>
                  <div>
                    <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                      Current ($K_t$)
                    </span>
                    <span className="text-base font-bold text-blue-600">
                      {subj.currentAccuracy.toFixed(1)}%
                    </span>
                  </div>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center text-xs text-slate-500 mb-1.5">
                  <span>Assessed Concepts</span>
                  <span className="font-semibold text-slate-700">
                    {subj.assessedConceptsCount} / {subj.conceptsCount}
                  </span>
                </div>
                <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-blue-600 h-full rounded-full transition-all duration-500"
                    style={{
                      width: `${subj.conceptsCount > 0 ? Math.min(100, Math.max(0, (subj.assessedConceptsCount / subj.conceptsCount) * 100)) : 0}%`
                    }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
