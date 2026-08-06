import React from "react";
import { 
  GraduationCap, 
  BookOpen, 
  HelpCircle, 
  FileText, 
  Sparkles, 
  Minimize2, 
  Code2 
} from "lucide-react";

export type LearningMode = 
  | "LEARN" 
  | "EXPLAIN" 
  | "QUIZ" 
  | "SUMMARY" 
  | "SOCRATIC" 
  | "SIMPLIFY" 
  | "CODE";

export interface LearningModeOption {
  key: LearningMode;
  label: string;
  description: string;
  icon: React.ElementType;
  badge?: string;
  color: string;
  activeColor: string;
}

export const LEARNING_MODES: LearningModeOption[] = [
  {
    key: "LEARN",
    label: "Adaptive Mastery",
    description: "Step-by-step guidance tailored to your learning profile",
    icon: GraduationCap,
    color: "text-purple-400",
    activeColor: "from-purple-600 to-indigo-600 border-purple-400/50 text-white shadow-purple-500/25",
    badge: "Recommended"
  },
  {
    key: "EXPLAIN",
    label: "Deep Dive",
    description: "Comprehensive breakdown of concepts with diagrams & examples",
    icon: BookOpen,
    color: "text-cyan-400",
    activeColor: "from-cyan-600 to-blue-600 border-cyan-400/50 text-white shadow-cyan-500/25"
  },
  {
    key: "SOCRATIC",
    label: "Socratic Method",
    description: "Guided questioning to help you derive answers independently",
    icon: Sparkles,
    color: "text-amber-400",
    activeColor: "from-amber-600 to-orange-600 border-amber-400/50 text-white shadow-amber-500/25",
    badge: "Interactive"
  },
  {
    key: "QUIZ",
    label: "Quiz & Test",
    description: "Practice questions with instant feedback and explanations",
    icon: HelpCircle,
    color: "text-emerald-400",
    activeColor: "from-emerald-600 to-teal-600 border-emerald-400/50 text-white shadow-emerald-500/25"
  },
  {
    key: "SIMPLIFY",
    label: "ELI5 Mode",
    description: "Complex concepts explained in simple everyday analogies",
    icon: Minimize2,
    color: "text-pink-400",
    activeColor: "from-pink-600 to-rose-600 border-pink-400/50 text-white shadow-pink-500/25"
  },
  {
    key: "CODE",
    label: "Code & Algo",
    description: "Clean implementations, time/space complexity analysis & debugging",
    icon: Code2,
    color: "text-blue-400",
    activeColor: "from-blue-600 to-cyan-600 border-blue-400/50 text-white shadow-blue-500/25"
  },
  {
    key: "SUMMARY",
    label: "Cheatsheet & Summary",
    description: "Key takeaways, formulas, bullet points, and quick revisions",
    icon: FileText,
    color: "text-indigo-400",
    activeColor: "from-indigo-600 to-violet-600 border-indigo-400/50 text-white shadow-indigo-500/25"
  }
];

interface LearningModeSelectorProps {
  activeMode: LearningMode;
  onModeChange: (mode: LearningMode) => void;
}

export default function LearningModeSelector({
  activeMode,
  onModeChange
}: LearningModeSelectorProps) {
  return (
    <div className="glass-panel rounded-2xl border border-[var(--glass-border)] px-4 py-2.5 overflow-x-auto shrink-0 backdrop-blur-md">
      <div className="flex items-center gap-2 max-w-7xl mx-auto min-w-max">
        {LEARNING_MODES.map((mode) => {
          const Icon = mode.icon;
          const isActive = activeMode === mode.key;
          return (
            <button
              key={mode.key}
              onClick={() => onModeChange(mode.key)}
              title={mode.description}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all duration-200 cursor-pointer border ${
                isActive
                  ? `bg-gradient-to-r ${mode.activeColor} shadow-lg border-white/20 font-bold scale-[1.02]`
                  : "bg-white/5 border-[var(--glass-border)] text-secondary-theme hover:text-main-theme hover:bg-white/10"
              }`}
            >
              <Icon className={`h-4 w-4 ${isActive ? "text-white" : mode.color}`} />
              <span>{mode.label}</span>
              {mode.badge && !isActive && (
                <span className="text-[9px] font-extrabold uppercase px-1.5 py-0.5 rounded-md bg-purple-500/20 text-purple-300 border border-purple-500/30">
                  {mode.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
