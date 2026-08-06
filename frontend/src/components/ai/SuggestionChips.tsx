import React from "react";
import {
  Sparkles,
  HelpCircle,
  Code,
  BookOpen,
  Compass,
  Lightbulb,
  Bot,
  GraduationCap,
  Minimize2,
  FileText,
  Zap
} from "lucide-react";

interface SuggestionChipsProps {
  activeModeLabel?: string;
  onSelectPrompt: (prompt: string) => void;
  studentContext?: any;
}

const PROMPT_SUGGESTIONS = [
  {
    icon: Sparkles,
    title: "Explain a Concept",
    prompt: "Can you explain the core concepts of Binary Search Trees and how search operations work?",
    badge: "Concept Mastery",
    color: "from-purple-600/20 to-indigo-600/20 border-purple-500/30 hover:border-purple-400/60"
  },
  {
    icon: Code,
    title: "Algorithm Implementation",
    prompt: "Provide an optimized Python implementation for QuickSort with step-by-step comments.",
    badge: "Coding & Algo",
    color: "from-cyan-600/20 to-blue-600/20 border-cyan-500/30 hover:border-cyan-400/60"
  },
  {
    icon: HelpCircle,
    title: "Test My Knowledge",
    prompt: "Give me 3 practice MCQs on Graph Traversals (BFS & DFS) with explanations.",
    badge: "Interactive Quiz",
    color: "from-emerald-600/20 to-teal-600/20 border-emerald-500/30 hover:border-emerald-400/60"
  },
  {
    icon: Compass,
    title: "Socratic Discovery",
    prompt: "Guide me step-by-step to derive the time complexity formula for Merge Sort.",
    badge: "Socratic Mode",
    color: "from-amber-600/20 to-orange-600/20 border-amber-500/30 hover:border-amber-400/60"
  },
  {
    icon: Minimize2,
    title: "Simplify for Me",
    prompt: "Explain Dynamic Programming like I'm 10 years old, using a real-world analogy.",
    badge: "ELI5 Mode",
    color: "from-pink-600/20 to-rose-600/20 border-pink-500/30 hover:border-pink-400/60"
  },
  {
    icon: FileText,
    title: "Quick Cheatsheet",
    prompt: "Create a concise cheatsheet for Sorting Algorithms — time complexity, space complexity, and use cases.",
    badge: "Summary",
    color: "from-indigo-600/20 to-violet-600/20 border-indigo-500/30 hover:border-indigo-400/60"
  }
];

const LEARNING_MODE_CARDS = [
  {
    key: "LEARN",
    icon: GraduationCap,
    label: "Adaptive Mastery",
    description: "Tailored step-by-step guidance",
    gradient: "from-purple-600 to-indigo-600",
    glow: "shadow-purple-500/20"
  },
  {
    key: "EXPLAIN",
    icon: BookOpen,
    label: "Deep Dive",
    description: "Comprehensive concept breakdowns",
    gradient: "from-cyan-600 to-blue-600",
    glow: "shadow-cyan-500/20"
  },
  {
    key: "QUIZ",
    icon: HelpCircle,
    label: "Quiz & Test",
    description: "Practice with instant feedback",
    gradient: "from-emerald-600 to-teal-600",
    glow: "shadow-emerald-500/20"
  },
  {
    key: "CODE",
    icon: Code,
    label: "Code & Algo",
    description: "Clean code & complexity analysis",
    gradient: "from-blue-600 to-cyan-600",
    glow: "shadow-blue-500/20"
  }
];

export default function SuggestionChips({
  activeModeLabel,
  onSelectPrompt,
  studentContext
}: SuggestionChipsProps) {
  const name = studentContext?.fullName?.split(" ")[0] || "Student";

  return (
    <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-slate-800">
      <div className="max-w-3xl mx-auto px-4 py-6 space-y-6">

        {/* ── Hero Welcome Card ── */}
        <div className="relative glass-panel rounded-3xl border border-[var(--glass-border)] p-6 text-center overflow-hidden">
          {/* background glow */}
          <div className="absolute inset-0 bg-gradient-to-br from-purple-600/5 via-transparent to-cyan-600/5 pointer-events-none" />
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-32 bg-purple-600/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative space-y-3">
            <div className="h-14 w-14 rounded-2xl bg-gradient-to-tr from-purple-600 via-pink-600 to-cyan-600 flex items-center justify-center text-white mx-auto shadow-xl shadow-purple-500/30 border border-white/10">
              <Bot className="h-7 w-7" />
            </div>

            <div>
              <h2 className="text-xl font-black text-main-theme">
                Hello, {name}! 👋
              </h2>
              <p className="text-sm text-secondary-theme mt-1">
                Your adaptive AI study companion is ready. What would you like to master today?
              </p>
            </div>

            <div className="flex items-center justify-center gap-2 flex-wrap">
              <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-purple-500/15 border border-purple-500/30 text-purple-300 text-[11px] font-semibold">
                <Zap className="h-3 w-3" />
                Adaptive AI Active
              </span>
              <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/25 text-emerald-300 text-[11px] font-semibold">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                Gemini Connected
              </span>
              {activeModeLabel && (
                <span className="px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/25 text-cyan-300 text-[11px] font-semibold">
                  Mode: {activeModeLabel}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* ── Learning Mode Cards ── */}
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-secondary-theme px-1 mb-2.5 flex items-center gap-1.5">
            <Sparkles className="h-3 w-3 text-purple-theme" />
            Choose a Learning Mode
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            {LEARNING_MODE_CARDS.map((mode) => {
              const Icon = mode.icon;
              return (
                <button
                  key={mode.key}
                  onClick={() => onSelectPrompt(`Let's start a ${mode.label} session. I want to learn something new today.`)}
                  className="group p-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-[var(--glass-border)] hover:border-purple-500/40 transition-all duration-200 cursor-pointer text-left hover:scale-[1.02]"
                >
                  <div className={`h-8 w-8 rounded-xl bg-gradient-to-br ${mode.gradient} flex items-center justify-center text-white shadow-lg ${mode.glow} mb-2.5 border border-white/10`}>
                    <Icon className="h-4 w-4" />
                  </div>
                  <p className="text-xs font-bold text-main-theme group-hover:text-purple-theme transition-colors">{mode.label}</p>
                  <p className="text-[10px] text-secondary-theme mt-0.5 leading-tight">{mode.description}</p>
                </button>
              );
            })}
          </div>
        </div>

        {/* ── Suggested Prompts ── */}
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-secondary-theme px-1 mb-2.5 flex items-center gap-1.5">
            <Lightbulb className="h-3 w-3 text-amber-theme" />
            Suggested Prompts
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {PROMPT_SUGGESTIONS.map((item, idx) => {
              const Icon = item.icon;
              return (
                <button
                  key={idx}
                  onClick={() => onSelectPrompt(item.prompt)}
                  className={`group p-3.5 rounded-2xl bg-gradient-to-br ${item.color} border transition-all duration-200 cursor-pointer text-left hover:scale-[1.01] hover:shadow-lg`}
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <Icon className="h-4 w-4 text-secondary-theme group-hover:text-main-theme transition-colors mt-0.5 shrink-0" />
                    <span className="text-[9px] font-extrabold uppercase px-1.5 py-0.5 rounded-full bg-white/10 text-secondary-theme">
                      {item.badge}
                    </span>
                  </div>
                  <h4 className="text-xs font-bold text-main-theme group-hover:text-white transition-colors">{item.title}</h4>
                  <p className="text-[10px] text-secondary-theme line-clamp-2 mt-1 leading-snug">{item.prompt}</p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Bottom hint */}
        <p className="text-center text-[11px] text-secondary-theme opacity-60">
          Or type any academic question in the input below ↓
        </p>
      </div>
    </div>
  );
}
