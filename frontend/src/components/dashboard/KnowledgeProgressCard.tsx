import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2, AlertCircle, Sparkles, BrainCircuit, Activity, RefreshCw } from "lucide-react";
import { KnowledgeProgressCardProps } from "./types";
import { fetchKnowledgeProfile } from "../../services/api";

export default function KnowledgeProgressCard({ profile }: KnowledgeProgressCardProps) {
  const [knowledgeData, setKnowledgeData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadKnowledgeEngineData() {
      setLoading(true);
      const userId = profile?.id || localStorage.getItem("edupilot_user_id") || "";
      if (userId) {
        const kp = await fetchKnowledgeProfile(userId);
        if (kp) {
          setKnowledgeData(kp);
        }
      }
      setLoading(false);
    }
    loadKnowledgeEngineData();
  }, [profile]);

  const strongConceptsList = knowledgeData?.strongConcepts || [];
  const weakConceptsList = knowledgeData?.weakConcepts || [];
  const conceptEntries = knowledgeData?.conceptEntries || [];
  const learningHealth = knowledgeData?.learningHealthScore || 0.0;

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 bg-gradient-to-br from-purple-900/10 via-transparent to-cyan-900/10">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-theme border border-emerald-500/20">
            <Sparkles className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-base font-extrabold tracking-wide text-main-theme flex items-center gap-2">
              <span>Knowledge Intelligence Engine</span>
            </h3>
            <p className="text-[11px] text-secondary-theme">Persistent concept-level mastery & learning health profile</p>
          </div>
        </div>

        {/* Learning Health Indicator */}
        <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-xl border border-white/10 self-start sm:self-auto">
          <Activity className="h-4 w-4 text-emerald-theme" />
          <span className="text-xs text-secondary-theme">Learning Health:</span>
          <span className="text-xs font-black text-emerald-theme">
            {learningHealth > 0 ? `${learningHealth}%` : "Baseline"}
          </span>
        </div>
      </div>

      {/* 4 Mastery Level Counters */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center text-xs">
        <div className="p-3 bg-white/5 rounded-xl border border-white/5 space-y-1">
          <span className="text-[10px] text-emerald-400 font-extrabold uppercase">Mastered</span>
          <div className="text-lg font-black text-main-theme">{knowledgeData?.masteredCount || 0}</div>
        </div>
        <div className="p-3 bg-white/5 rounded-xl border border-white/5 space-y-1">
          <span className="text-[10px] text-purple-400 font-extrabold uppercase">Proficient</span>
          <div className="text-lg font-black text-main-theme">{knowledgeData?.proficientCount || 0}</div>
        </div>
        <div className="p-3 bg-white/5 rounded-xl border border-white/5 space-y-1">
          <span className="text-[10px] text-amber-400 font-extrabold uppercase">Intermediate</span>
          <div className="text-lg font-black text-main-theme">{knowledgeData?.intermediateCount || 0}</div>
        </div>
        <div className="p-3 bg-white/5 rounded-xl border border-white/5 space-y-1">
          <span className="text-[10px] text-pink-400 font-extrabold uppercase">Beginner</span>
          <div className="text-lg font-black text-main-theme">{knowledgeData?.beginnerCount || 0}</div>
        </div>
      </div>

      {/* Concept Breakdown Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        
        {/* Strong Mastered Concepts */}
        <div className="space-y-2.5">
          <div className="text-xs font-bold text-emerald-theme uppercase tracking-wider flex items-center gap-1.5">
            <CheckCircle2 className="h-4 w-4" />
            <span>Mastered & Strong Concepts ({strongConceptsList.length})</span>
          </div>
          <div className="space-y-1.5 max-h-[180px] overflow-y-auto pr-1">
            {strongConceptsList.length > 0 ? (
              strongConceptsList.map((cName: string, idx: number) => (
                <div 
                  key={idx} 
                  className="p-2.5 bg-emerald-500/5 border border-emerald-500/15 rounded-lg text-xs flex justify-between items-center"
                >
                  <span className="font-semibold text-main-theme truncate">{cName}</span>
                  <span className="text-[10px] text-emerald-400 font-extrabold px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
                    STRONG
                  </span>
                </div>
              ))
            ) : (
              <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme">
                No mastered concepts evaluated yet. Take a diagnostic assessment to populate strengths.
              </div>
            )}
          </div>
        </div>

        {/* Weak Concepts Needing Review */}
        <div className="space-y-2.5">
          <div className="text-xs font-bold text-pink-theme uppercase tracking-wider flex items-center gap-1.5">
            <AlertCircle className="h-4 w-4" />
            <span>Concepts Needing Focus ({weakConceptsList.length})</span>
          </div>
          <div className="space-y-1.5 max-h-[180px] overflow-y-auto pr-1">
            {weakConceptsList.length > 0 ? (
              weakConceptsList.map((cName: string, idx: number) => (
                <div 
                  key={idx} 
                  className="p-2.5 bg-pink-500/5 border border-pink-500/15 rounded-lg text-xs flex justify-between items-center"
                >
                  <span className="font-semibold text-main-theme truncate">{cName}</span>
                  <span className="px-2.5 py-1 bg-pink-500/20 text-pink-300 text-[10px] font-bold rounded-md shrink-0">
                    Needs Focus
                  </span>
                </div>
              ))
            ) : (
              <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme">
                No weak concepts identified. Exceptional performance!
              </div>
            )}
          </div>
        </div>

      </div>

    </div>
  );
}
