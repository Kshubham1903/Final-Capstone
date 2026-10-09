import React, { useState, useRef, useEffect } from "react";
import { 
  GraduationCap, 
  BookOpen, 
  HelpCircle, 
  FileText, 
  Sparkles, 
  Minimize2, 
  Code2,
  ChevronDown,
  Check
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
  className?: string;
}

export default function LearningModeSelector({
  activeMode,
  onModeChange,
  className = ""
}: LearningModeSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const currentModeObj = LEARNING_MODES.find((m) => m.key === activeMode) || LEARNING_MODES[0];
  const CurrentIcon = currentModeObj.icon;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className={`relative inline-block ${className}`} ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-[var(--glass-border)] text-xs font-semibold text-main-theme transition-all cursor-pointer"
        title="Change learning mode"
      >
        <CurrentIcon className={`h-4 w-4 ${currentModeObj.color}`} />
        <span>{currentModeObj.label}</span>
        <ChevronDown className={`h-3.5 w-3.5 text-secondary-theme transition-transform ${isOpen ? "rotate-180" : ""}`} />
      </button>

      {isOpen && (
        <div className="absolute right-0 sm:left-0 mt-2 w-72 bg-[var(--glass-hover-bg)] rounded-2xl p-2 border border-[var(--glass-border)] shadow-2xl z-50 backdrop-blur-2xl space-y-1">
          <div className="px-3 py-1.5 text-[10px] uppercase font-bold text-secondary-theme tracking-wider">
            Select Learning Mode
          </div>
          {LEARNING_MODES.map((mode) => {
            const Icon = mode.icon;
            const isSelected = activeMode === mode.key;
            return (
              <button
                key={mode.key}
                onClick={() => {
                  onModeChange(mode.key);
                  setIsOpen(false);
                }}
                className={`w-full flex items-start gap-3 p-2.5 rounded-xl text-left transition-all cursor-pointer ${
                  isSelected
                    ? "bg-purple-600/20 border border-purple-500/40 text-main-theme"
                    : "hover:bg-white/5 text-secondary-theme hover:text-main-theme border border-transparent"
                }`}
              >
                <div className="p-1.5 rounded-lg bg-white/5 shrink-0 mt-0.5">
                  <Icon className={`h-4 w-4 ${mode.color}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <span className="text-xs font-bold text-main-theme">{mode.label}</span>
                    {isSelected && <Check className="h-3.5 w-3.5 text-purple-400 shrink-0" />}
                  </div>
                  <p className="text-[10px] text-secondary-theme leading-tight mt-0.5 line-clamp-1">
                    {mode.description}
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
