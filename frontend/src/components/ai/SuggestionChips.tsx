import React from "react";
import { Bot, Sparkles } from "lucide-react";

interface SuggestionChipsProps {
  onSelectPrompt: (prompt: string) => void;
  activeModeLabel?: string;
  studentContext?: any;
}

const QUICK_PROMPTS = [
  "Explain Binary Search Trees and how search operations work",
  "Provide a clean Python implementation of QuickSort",
  "Give me 3 practice MCQs on Graph Traversals with explanations"
];

export default function SuggestionChips({ onSelectPrompt }: SuggestionChipsProps) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-6 text-center overflow-y-auto">
      <div className="max-w-xl w-full space-y-6 animate-fade-in">

        {/* Minimal Bot Icon & Welcome Heading */}
        <div className="space-y-3">
          <div className="h-12 w-12 rounded-2xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400 mx-auto shadow-lg shadow-purple-500/10">
            <Bot className="h-6 w-6" />
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-main-theme tracking-tight">
            What can I help you learn today?
          </h2>
          <p className="text-xs sm:text-sm text-secondary-theme leading-relaxed">
            Ask any academic question, get code breakdowns, or explore complex topics step-by-step.
          </p>
        </div>

        {/* Minimal text-only suggested prompt pills */}
        <div className="flex flex-col gap-2 pt-2">
          {QUICK_PROMPTS.map((prompt, idx) => (
            <button
              key={idx}
              onClick={() => onSelectPrompt(prompt)}
              className="px-4 py-3 rounded-2xl bg-white/5 hover:bg-white/10 border border-[var(--glass-border)] text-xs text-left font-medium text-main-theme hover:border-purple-500/40 transition-all cursor-pointer flex items-center justify-between gap-3 group"
            >
              <span className="line-clamp-1">{prompt}</span>
              <Sparkles className="h-3.5 w-3.5 text-secondary-theme group-hover:text-purple-400 transition-colors shrink-0" />
            </button>
          ))}
        </div>

      </div>
    </div>
  );
}
