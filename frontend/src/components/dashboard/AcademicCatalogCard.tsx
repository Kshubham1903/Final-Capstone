import React, { useState, useEffect } from "react";
import { BookOpen, Layers, Award, Sparkles, Filter, CheckCircle2 } from "lucide-react";
import { AcademicCatalogCardProps, SubjectCatalogItem } from "./types";
import { fetchSubjectBranches, fetchSubjectsByBranchAndSemester } from "../../services/api";

export default function AcademicCatalogCard({
  currentBranch = "Computer Science & Engineering",
  currentSemester = 3
}: AcademicCatalogCardProps) {
  const [branches, setBranches] = useState<string[]>([]);
  const [selectedBranch, setSelectedBranch] = useState(currentBranch);
  const [selectedSemester, setSelectedSemester] = useState(currentSemester);
  const [subjects, setSubjects] = useState<SubjectCatalogItem[]>([]);
  const [loading, setLoading] = useState(true);

  // Load distinct branches
  useEffect(() => {
    async function loadBranches() {
      const bList = await fetchSubjectBranches();
      if (bList && bList.length > 0) {
        setBranches(bList);
        if (!bList.includes(selectedBranch)) {
          setSelectedBranch(bList[0]);
        }
      }
    }
    loadBranches();
  }, []);

  // Load subjects for selected branch & semester
  useEffect(() => {
    async function loadCatalogSubjects() {
      setLoading(true);
      const items = await fetchSubjectsByBranchAndSemester(selectedBranch, selectedSemester);
      setSubjects(items || []);
      setLoading(false);
    }
    loadCatalogSubjects();
  }, [selectedBranch, selectedSemester]);

  const totalCredits = subjects.reduce((sum, item) => sum + (item.credits || 0), 0);

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 bg-gradient-to-br from-purple-900/10 via-transparent to-cyan-900/10">
      
      {/* Header & Filter Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-purple-500/10 flex items-center justify-center text-purple-theme border border-purple-500/20">
            <BookOpen className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-base font-extrabold tracking-wide text-main-theme flex items-center gap-2">
              <span>📚 Academic Catalog Browser</span>
            </h3>
            <p className="text-[11px] text-secondary-theme">Master curriculum hierarchy & semester subject registry</p>
          </div>
        </div>

        {/* Branch & Semester Selectors */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <div className="flex items-center gap-1 bg-white/5 border border-white/10 px-2.5 py-1 rounded-xl">
            <Filter className="h-3.5 w-3.5 text-secondary-theme" />
            <select
              value={selectedBranch}
              onChange={(e) => setSelectedBranch(e.target.value)}
              className="bg-transparent text-main-theme font-bold text-xs focus:outline-none cursor-pointer"
            >
              {branches.length > 0 ? (
                branches.map((b) => (
                  <option key={b} value={b} className="bg-[#0b0f19] text-white">
                    {b}
                  </option>
                ))
              ) : (
                <option value={selectedBranch} className="bg-[#0b0f19] text-white">
                  {selectedBranch}
                </option>
              )}
            </select>
          </div>

          <div className="flex items-center gap-1 bg-white/5 border border-white/10 px-2.5 py-1 rounded-xl">
            <span className="text-[10px] text-secondary-theme font-extrabold uppercase">Sem:</span>
            <select
              value={selectedSemester}
              onChange={(e) => setSelectedSemester(Number(e.target.value))}
              className="bg-transparent text-purple-theme font-bold text-xs focus:outline-none cursor-pointer"
            >
              {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                <option key={s} value={s} className="bg-[#0b0f19] text-white">
                  Semester {s}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Catalog Items Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="p-4 bg-white/5 rounded-xl space-y-2">
              <div className="h-4 w-16 bg-white/10 rounded" />
              <div className="h-5 w-32 bg-white/10 rounded" />
            </div>
          ))}
        </div>
      ) : subjects.length > 0 ? (
        <div className="space-y-3">
          <div className="flex justify-between items-center text-xs">
            <span className="text-secondary-theme">
              Displaying <strong className="text-main-theme">{subjects.length}</strong> core subjects for {selectedBranch} (Semester {selectedSemester})
            </span>
            <span className="text-purple-theme font-bold bg-purple-500/10 px-2.5 py-0.5 rounded-full border border-purple-500/20">
              Total Credits: {totalCredits}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {subjects.map((item) => (
              <div 
                key={item.id || item.subjectCode} 
                className="p-3.5 rounded-xl glass-panel border border-white/5 hover:border-purple-500/30 transition-all space-y-2 group"
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-black uppercase text-purple-theme bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20">
                    {item.subjectCode}
                  </span>
                  <span className="text-[10px] font-bold text-emerald-theme bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                    {item.credits} Credits
                  </span>
                </div>
                <h4 className="text-xs font-extrabold text-main-theme group-hover:text-purple-theme transition-colors leading-snug">
                  {item.subjectName}
                </h4>
                <div className="pt-1 flex items-center justify-between text-[10px] text-secondary-theme">
                  <span>{item.institution || "EduPilot Academy"}</span>
                  <span className="flex items-center gap-1 text-emerald-theme font-semibold">
                    <CheckCircle2 className="h-3 w-3" />
                    Active
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="p-6 bg-white/3 rounded-xl border border-white/5 text-center space-y-2">
          <Layers className="h-8 w-8 text-secondary-theme opacity-40 mx-auto" />
          <h4 className="font-bold text-xs text-main-theme">No Catalog Subjects Found</h4>
          <p className="text-[10px] text-secondary-theme max-w-sm mx-auto">
            No subjects registered for {selectedBranch} in Semester {selectedSemester}. Select another branch or semester.
          </p>
        </div>
      )}

    </div>
  );
}
