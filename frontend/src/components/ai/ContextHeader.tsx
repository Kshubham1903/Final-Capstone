import React, { useState } from "react";
import { User, Cpu, ChevronDown, ChevronUp, BookOpen, Target, Activity } from "lucide-react";

interface ContextHeaderProps {
  studentContext?: any;
  providerName?: string;
}

export default function ContextHeader({ studentContext, providerName = "Google Gemini 2.0 Flash" }: ContextHeaderProps) {
  const [expanded, setExpanded] = useState(false);

  const course = studentContext?.course || studentContext?.academicProfile?.branch || "Computer Science";
  const semester = studentContext?.semester || studentContext?.academicProfile?.semester || 5;
  const currentTopic = studentContext?.currentTopic || "Data Structures & Algorithms";
  const sgiScore = studentContext?.studentGrowthIndex || studentContext?.academicProfile?.studentGrowthIndex || 8.4;
  const learningStyle = studentContext?.learningStyle || "Visual & Conceptual";
  const targetGoal = studentContext?.targetGoal || "Master Algorithms & Target CGPA 9.0+";

  return (
    <div className="bg-slate-900/90 border-b border-white/10 px-4 py-2 text-xs text-slate-300">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
        {/* Primary context bar */}
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="flex items-center gap-1.5 font-semibold text-slate-200 shrink-0">
            <User className="h-3.5 w-3.5 text-purple-400" />
            <span>{course} (Sem {semester})</span>
          </div>

          <span className="text-slate-600 hidden sm:inline">•</span>

          <div className="hidden sm:flex items-center gap-1.5 text-slate-300 truncate">
            <BookOpen className="h-3.5 w-3.5 text-cyan-400" />
            <span className="truncate">Topic: <strong className="text-white">{currentTopic}</strong></span>
          </div>

          <span className="text-slate-600 hidden md:inline">•</span>

          <div className="hidden md:flex items-center gap-1.5 text-slate-300">
            <Activity className="h-3.5 w-3.5 text-emerald-400" />
            <span>Growth SGI: <strong className="text-emerald-400 font-bold">{sgiScore} / 10</strong></span>
          </div>
        </div>

        {/* LLM & expand toggle */}
        <div className="flex items-center gap-3 shrink-0">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 text-[11px]">
            <Cpu className="h-3 w-3 text-purple-400" />
            <span className="font-semibold">{providerName}</span>
          </div>

          <button
            onClick={() => setExpanded(!expanded)}
            className="flex items-center gap-1 text-slate-400 hover:text-white transition-colors cursor-pointer text-[11px]"
          >
            <span>{expanded ? "Hide Details" : "Context"}</span>
            {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>

      {/* Expanded Details */}
      {expanded && (
        <div className="mt-2 pt-2 border-t border-white/10 max-w-7xl mx-auto grid grid-cols-1 sm:grid-cols-3 gap-3 text-slate-400 text-[11px]">
          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-white/5">
            <Target className="h-4 w-4 text-purple-400 shrink-0" />
            <div>
              <span className="text-slate-500 block text-[10px]">Learning Goal</span>
              <span className="text-slate-200 font-medium">{targetGoal}</span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-white/5">
            <Activity className="h-4 w-4 text-cyan-400 shrink-0" />
            <div>
              <span className="text-slate-500 block text-[10px]">Learning Preference</span>
              <span className="text-slate-200 font-medium">{learningStyle}</span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-white/5">
            <Cpu className="h-4 w-4 text-emerald-400 shrink-0" />
            <div>
              <span className="text-slate-500 block text-[10px]">Adaptive Personalization</span>
              <span className="text-slate-200 font-medium">Multi-turn Memory Active</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
