import React, { useState } from "react";
import { User, Cpu, ChevronDown, ChevronUp, BookOpen, Target, Activity } from "lucide-react";

interface ContextHeaderProps {
  studentContext?: any;
  providerName?: string;
  /** When true, renders a compact single-line strip for the chat header */
  compact?: boolean;
}

export default function ContextHeader({
  studentContext,
  providerName = "Google Gemini 2.0 Flash",
  compact = false
}: ContextHeaderProps) {
  const [expanded, setExpanded] = useState(false);

  const course = studentContext?.course || studentContext?.academicProfile?.branch || "Computer Science";
  const semester = studentContext?.semester || studentContext?.academicProfile?.semester || 5;
  const currentTopic = studentContext?.currentTopic || "Data Structures & Algorithms";
  const sgiScore = studentContext?.studentGrowthIndex || studentContext?.academicProfile?.studentGrowthIndex || 8.4;
  const learningStyle = studentContext?.learningStyle || "Visual & Conceptual";
  const targetGoal = studentContext?.targetGoal || "Master Algorithms & Target CGPA 9.0+";

  /* ── COMPACT single-line strip (used inside chat header) ── */
  if (compact) {
    return (
      <div className="glass-panel rounded-xl border border-[var(--glass-border)] px-3 py-2 text-[11px] text-secondary-theme">
        <div className="flex items-center justify-between gap-3 flex-wrap">
          {/* Primary info */}
          <div className="flex items-center gap-3 overflow-hidden flex-wrap">
            <div className="flex items-center gap-1.5 font-semibold text-main-theme shrink-0">
              <User className="h-3 w-3 text-purple-theme" />
              <span>{course} · Sem {semester}</span>
            </div>

            <span className="text-secondary-theme opacity-30 hidden sm:inline">|</span>

            <div className="hidden sm:flex items-center gap-1 text-secondary-theme truncate">
              <BookOpen className="h-3 w-3 text-cyan-theme shrink-0" />
              <span className="truncate">Topic: <strong className="text-main-theme">{currentTopic}</strong></span>
            </div>

            <span className="text-secondary-theme opacity-30 hidden md:inline">|</span>

            <div className="hidden md:flex items-center gap-1 text-secondary-theme">
              <Activity className="h-3 w-3 text-emerald-theme" />
              <span>SGI: <strong className="text-emerald-theme font-bold">{sgiScore}/10</strong></span>
            </div>
          </div>

          {/* Provider + expand */}
          <div className="flex items-center gap-2 shrink-0">
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 text-[10px]">
              <Cpu className="h-2.5 w-2.5 text-purple-theme" />
              <span className="font-semibold">{providerName}</span>
            </div>

            <button
              onClick={() => setExpanded(!expanded)}
              className="flex items-center gap-0.5 text-secondary-theme hover:text-main-theme transition-colors cursor-pointer text-[10px] hover:bg-white/5 px-1.5 py-0.5 rounded-lg"
            >
              <span>{expanded ? "Hide" : "Context"}</span>
              {expanded ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            </button>
          </div>
        </div>

        {/* Expanded detail rows */}
        {expanded && (
          <div className="mt-2 pt-2 border-t border-[var(--glass-border)] grid grid-cols-1 sm:grid-cols-3 gap-2 text-[10px]">
            <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
              <Target className="h-3.5 w-3.5 text-purple-theme shrink-0" />
              <div>
                <span className="text-secondary-theme opacity-70 block text-[9px]">Learning Goal</span>
                <span className="text-main-theme font-medium">{targetGoal}</span>
              </div>
            </div>
            <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
              <Activity className="h-3.5 w-3.5 text-cyan-theme shrink-0" />
              <div>
                <span className="text-secondary-theme opacity-70 block text-[9px]">Learning Style</span>
                <span className="text-main-theme font-medium">{learningStyle}</span>
              </div>
            </div>
            <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
              <Cpu className="h-3.5 w-3.5 text-emerald-theme shrink-0" />
              <div>
                <span className="text-secondary-theme opacity-70 block text-[9px]">Personalization</span>
                <span className="text-main-theme font-medium">Multi-turn Memory Active</span>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  /* ── FULL (original) standalone mode ── */
  return (
    <div className="glass-panel rounded-2xl border border-[var(--glass-border)] px-4 py-2.5 text-xs text-secondary-theme shrink-0">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="flex items-center gap-1.5 font-semibold text-main-theme shrink-0">
            <User className="h-3.5 w-3.5 text-purple-theme" />
            <span>{course} (Sem {semester})</span>
          </div>
          <span className="text-secondary-theme opacity-40 hidden sm:inline">•</span>
          <div className="hidden sm:flex items-center gap-1.5 text-secondary-theme truncate">
            <BookOpen className="h-3.5 w-3.5 text-cyan-theme" />
            <span className="truncate">Topic: <strong className="text-main-theme">{currentTopic}</strong></span>
          </div>
          <span className="text-secondary-theme opacity-40 hidden md:inline">•</span>
          <div className="hidden md:flex items-center gap-1.5 text-secondary-theme">
            <Activity className="h-3.5 w-3.5 text-emerald-theme" />
            <span>Growth SGI: <strong className="text-emerald-theme font-bold">{sgiScore} / 10</strong></span>
          </div>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 text-[11px]">
            <Cpu className="h-3 w-3 text-purple-theme" />
            <span className="font-semibold">{providerName}</span>
          </div>
          <button
            onClick={() => setExpanded(!expanded)}
            className="flex items-center gap-1 text-secondary-theme hover:text-main-theme transition-colors cursor-pointer text-[11px]"
          >
            <span>{expanded ? "Hide Details" : "Context"}</span>
            {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>
      {expanded && (
        <div className="mt-2 pt-2 border-t border-[var(--glass-border)] max-w-7xl mx-auto grid grid-cols-1 sm:grid-cols-3 gap-3 text-secondary-theme text-[11px]">
          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
            <Target className="h-4 w-4 text-purple-theme shrink-0" />
            <div>
              <span className="text-secondary-theme opacity-70 block text-[10px]">Learning Goal</span>
              <span className="text-main-theme font-medium">{targetGoal}</span>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
            <Activity className="h-4 w-4 text-cyan-theme shrink-0" />
            <div>
              <span className="text-secondary-theme opacity-70 block text-[10px]">Learning Preference</span>
              <span className="text-main-theme font-medium">{learningStyle}</span>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-[var(--glass-border)]">
            <Cpu className="h-4 w-4 text-emerald-theme shrink-0" />
            <div>
              <span className="text-secondary-theme opacity-70 block text-[10px]">Adaptive Personalization</span>
              <span className="text-main-theme font-medium">Multi-turn Memory Active</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
