import React, { useState } from "react";
import { ConceptGrowthDetail } from "../../services/api";
import { CheckCircle2, Search, Filter, HelpCircle, ArrowUpRight, ArrowDownRight, Minus } from "lucide-react";

interface ConceptImprovementTableProps {
  concepts: ConceptGrowthDetail[];
}

export const ConceptImprovementTable: React.FC<ConceptImprovementTableProps> = ({ concepts }) => {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedSubject, setSelectedSubject] = useState<string>("ALL");
  const [filterImprovedOnly, setFilterImprovedOnly] = useState<boolean>(false);

  if (!concepts || concepts.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          Concept Improvement Breakdown
        </h3>
        <p className="text-sm text-slate-500 italic">
          No detailed concept mastery records exist yet. Complete an initial diagnostic to establish concept baselines.
        </p>
      </div>
    );
  }

  // Extract unique subjects dynamically
  const availableSubjects = Array.from(
    new Set(concepts.map((c) => c?.subject).filter((s): s is string => Boolean(s && typeof s === "string" && s.trim())))
  );

  const filteredConcepts = concepts.filter((c) => {
    if (!c) return false;
    const nameStr = (c.conceptName || c.conceptId || "").toLowerCase();
    const idStr = (c.conceptId || "").toLowerCase();
    const subjectStr = (c.subject || "").toLowerCase();
    const searchLower = (searchTerm || "").toLowerCase();

    const matchesSearch =
      nameStr.includes(searchLower) ||
      idStr.includes(searchLower) ||
      subjectStr.includes(searchLower);
    const matchesSubject = selectedSubject === "ALL" || c.subject === selectedSubject;
    const matchesImproved = !filterImprovedOnly || Boolean(c.isImproved);

    return matchesSearch && matchesSubject && matchesImproved;
  });

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            Concept Improvement Breakdown
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Rule: Concept Improved = Current Concept Knowledge &gt; Valid Baseline Concept Knowledge ($T_0$).
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
          {/* Search bar */}
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search concepts..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Subject Dropdown */}
          {availableSubjects.length > 1 && (
            <div className="flex items-center gap-1.5 bg-slate-50 px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs">
              <Filter className="w-3.5 h-3.5 text-slate-500" />
              <select
                value={selectedSubject}
                onChange={(e) => setSelectedSubject(e.target.value)}
                className="bg-transparent focus:outline-none text-slate-700 font-medium"
              >
                <option value="ALL">All Subjects</option>
                {availableSubjects.map((subj) => (
                  <option key={subj} value={subj}>
                    {subj}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Checkbox for Improved Only */}
          <label className="flex items-center gap-2 text-xs font-medium text-slate-600 cursor-pointer bg-slate-50 px-2.5 py-1.5 rounded-lg border border-slate-200">
            <input
              type="checkbox"
              checked={filterImprovedOnly}
              onChange={(e) => setFilterImprovedOnly(e.target.checked)}
              className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
            />
            Improved Only
          </label>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
              <th className="py-3 px-4">Concept Name</th>
              <th className="py-3 px-4">Subject</th>
              <th className="py-3 px-4 text-center">Baseline ($T_0$)</th>
              <th className="py-3 px-4 text-center">Current ($K_t$)</th>
              <th className="py-3 px-4 text-center">Growth (pp)</th>
              <th className="py-3 px-4 text-center">Status</th>
              <th className="py-3 px-4 text-center">Assessments</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
            {filteredConcepts.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-400 italic">
                  No concepts match the selected filters.
                </td>
              </tr>
            ) : (
              filteredConcepts.map((item, idx) => {
                const displayName = item.conceptName || item.conceptId || `Concept ${idx + 1}`;
                const displayId = item.conceptId || "";
                const displaySubject = item.subject || "General";
                const hasBaseline = Boolean(item.baselineAssessed) && typeof item.baselineAccuracy === "number";
                const growth = typeof item.growthPp === "number" ? item.growthPp : null;
                const currentAcc = typeof item.currentAccuracy === "number" ? item.currentAccuracy : 0;

                return (
                  <tr key={displayId || `concept-row-${idx}`} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3 px-4 font-semibold text-slate-800">
                      {displayName}
                      {displayId && (
                        <span className="block text-[10px] font-mono text-slate-400 font-normal">
                          {displayId}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-medium text-[11px]">
                        {displaySubject}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-mono">
                      {hasBaseline ? (
                        `${item.baselineAccuracy!.toFixed(1)}%`
                      ) : (
                        <span className="text-slate-400 text-[11px] italic" title="Concept was not in initial T0 diagnostic">
                          Not in $T_0$
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center font-mono font-bold text-blue-600">
                      {currentAcc.toFixed(1)}%
                    </td>
                    <td className="py-3 px-4 text-center font-mono">
                      {hasBaseline && growth !== null ? (
                        <span
                          className={`inline-flex items-center gap-0.5 font-bold ${
                            growth > 0
                              ? "text-emerald-600"
                              : growth < 0
                              ? "text-rose-600"
                              : "text-slate-500"
                          }`}
                        >
                          {growth > 0 ? (
                            <>
                              <ArrowUpRight className="w-3.5 h-3.5" />
                              +{growth.toFixed(1)} pp
                            </>
                          ) : growth < 0 ? (
                            <>
                              <ArrowDownRight className="w-3.5 h-3.5" />
                              {growth.toFixed(1)} pp
                            </>
                          ) : (
                            <>
                              <Minus className="w-3.5 h-3.5 text-slate-400" />
                              0.0 pp
                            </>
                          )}
                        </span>
                      ) : (
                        <span className="text-slate-400 text-[11px]">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {item.isImproved ? (
                        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800 inline-flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                          Improved
                        </span>
                      ) : hasBaseline && growth !== null && growth < 0 ? (
                        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800">
                          Declined
                        </span>
                      ) : (
                        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-slate-100 text-slate-600">
                          Maintained
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center text-slate-500 font-mono">
                      {item.assessmentCount ?? (hasBaseline ? 2 : 1)}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-4 pt-3 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
        <span className="flex items-center gap-1">
          <HelpCircle className="w-3.5 h-3.5" />
          Baseline ($T_0$) values originate strictly from the initial diagnostic.
        </span>
        <span>
          Showing {filteredConcepts.length} of {concepts.length} concepts
        </span>
      </div>
    </div>
  );
};
