import React, { useState, useEffect } from "react";
import { BrainCircuit, Play, CheckCircle2, Award, Clock, ArrowRight } from "lucide-react";
import { DiagnosticAssessmentCardProps } from "./types";
import { fetchLatestDiagnosticResult } from "../../services/api";
import AssessmentRunner from "./AssessmentRunner";

export default function DiagnosticAssessmentCard({ profile }: DiagnosticAssessmentCardProps) {
  const [showRunner, setShowRunner] = useState(false);
  const [latestResult, setLatestResult] = useState<any>(null);

  useEffect(() => {
    async function loadLatestResult() {
      const userId = profile?.userId || profile?.id || localStorage.getItem("edupilot_user_id") || "";
      if (userId) {
        const res = await fetchLatestDiagnosticResult(userId);
        if (res) {
          setLatestResult(res);
        }
      }
    }
    loadLatestResult();

    const handleAssessmentCompleted = () => {
      loadLatestResult();
    };

    if (typeof window !== "undefined") {
      window.addEventListener("edupilot:assessment-completed", handleAssessmentCompleted);
    }
    return () => {
      if (typeof window !== "undefined") {
        window.removeEventListener("edupilot:assessment-completed", handleAssessmentCompleted);
      }
    };
  }, [profile]);

  const activeBranch = profile?.branch || profile?.course || "Computer Science & Engineering";
  const activeSemester = profile?.semester || 3;
  const initialSubjectCode = (profile?.subjects && profile.subjects.length > 0) ? undefined : "CS301";

  return (
    <div className="space-y-4">
      
      {/* Diagnostic Evaluation Banner */}
      <div className="glass-panel p-6 rounded-2xl border border-white/5 bg-gradient-to-r from-purple-900/15 via-transparent to-pink-900/15 flex flex-col md:flex-row md:items-center justify-between gap-6">
        
        <div className="space-y-2 max-w-xl">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase text-purple-theme bg-purple-500/10 border border-purple-500/20">
              Diagnostic Intelligence Engine
            </span>
            <span className="text-[10px] text-secondary-theme">• Catalog Integrated</span>
          </div>

          <h3 className="text-lg font-extrabold text-main-theme flex items-center gap-2">
            <BrainCircuit className="h-5 w-5 text-purple-theme" />
            <span>Evaluate Your Conceptual Knowledge</span>
          </h3>

          <p className="text-xs text-secondary-theme leading-relaxed">
            Take adaptive diagnostic evaluations mapped to your branch ({activeBranch}) and semester ({activeSemester}) curriculum to generate live mastery maps and AI recommendations.
          </p>

          {/* Last Assessment Summary if available */}
          {latestResult && (
            <div className="pt-2 flex items-center gap-4 text-xs">
              <div className="flex items-center gap-1.5 text-emerald-400 font-bold bg-emerald-500/10 px-2.5 py-1 rounded-lg border border-emerald-500/20">
                <Award className="h-3.5 w-3.5" />
                <span>Last Score: {latestResult.score}/{latestResult.totalMarks} ({latestResult.percentage}%)</span>
              </div>
              <span className="text-[10px] text-secondary-theme">
                Subject: <strong className="text-main-theme">{latestResult.subjectCode}</strong> • Mastery: <strong className="text-purple-400">{latestResult.masteryLevel}</strong>
              </span>
            </div>
          )}
        </div>

        {/* CTA Launch Assessment Button */}
        <div className="shrink-0">
          <button
            onClick={() => setShowRunner(true)}
            className="px-6 py-3.5 rounded-xl bg-gradient-to-r from-purple-600 via-pink-600 to-purple-600 hover:from-purple-500 hover:to-pink-500 text-white font-extrabold text-xs shadow-xl shadow-purple-600/25 flex items-center gap-2 transition-all cursor-pointer group"
          >
            <Play className="h-4 w-4 fill-white group-hover:scale-110 transition-transform" />
            <span>Start Diagnostic Assessment</span>
            <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

      </div>

      {/* Assessment Runner Modal */}
      {showRunner && (
        <AssessmentRunner
          branch={activeBranch}
          semester={activeSemester}
          initialSubjectCode={initialSubjectCode}
          onClose={() => {
            setShowRunner(false);
          }}
        />
      )}

    </div>
  );
}
