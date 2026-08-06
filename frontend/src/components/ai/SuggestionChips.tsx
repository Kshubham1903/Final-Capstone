import React from "react";
import { Sparkles, HelpCircle, Code, BookOpen, Compass, Lightbulb } from "lucide-react";

interface SuggestionChipsProps {
  activeModeLabel?: string;
  onSelectPrompt: (prompt: string) => void;
}

export default function SuggestionChips({ activeModeLabel, onSelectPrompt }: SuggestionChipsProps) {
  const suggestions = [
    {
      icon: Sparkles,
      title: "Explain Concept",
      prompt: "Can you explain the core concepts of Binary Search Trees and how search operates?",
      badge: "Concept Mastery"
    },
    {
      icon: Code,
      title: "Algorithm Implementation",
      prompt: "Provide an optimized Python implementation for QuickSort with step-by-step comments.",
      badge: "Coding & Algo"
    },
    {
      icon: HelpCircle,
      title: "Test My Knowledge",
      prompt: "Give me 3 practice multiple-choice questions on Graph Traversals (BFS & DFS) to test me.",
      badge: "Interactive Quiz"
    },
    {
      icon: Compass,
      title: "Socratic Guided Discovery",
      prompt: "Guide me step-by-step to derive the time complexity formula for Merge Sort.",
      badge: "Socratic Mode"
    }
  ];

  return (
    <div className="max-w-2xl mx-auto my-8 p-6 glass-panel rounded-3xl border border-white/10 bg-slate-900/60 backdrop-blur-xl text-center space-y-6 shadow-2xl">
      <div className="space-y-2">
        <div className="h-12 w-12 rounded-2xl bg-gradient-to-tr from-purple-600 to-cyan-600 flex items-center justify-center text-white mx-auto shadow-lg shadow-purple-500/25">
          <Lightbulb className="h-6 w-6" />
        </div>
        <h3 className="text-lg font-extrabold text-white">How can EduPilot AI assist you today?</h3>
        <p className="text-xs text-slate-400">
          Select a prompt starter below or type any academic question. Active mode:{" "}
          <strong className="text-purple-300">{activeModeLabel || "Adaptive Mastery"}</strong>
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-left">
        {suggestions.map((item, idx) => {
          const Icon = item.icon;
          return (
            <button
              key={idx}
              onClick={() => onSelectPrompt(item.prompt)}
              className="p-4 rounded-2xl bg-white/5 hover:bg-purple-900/20 border border-white/10 hover:border-purple-500/40 transition-all duration-200 cursor-pointer group hover:scale-[1.02] flex flex-col justify-between space-y-2"
            >
              <div className="flex items-center justify-between">
                <span className="p-2 rounded-xl bg-purple-500/10 text-purple-400 group-hover:bg-purple-500/20 group-hover:text-purple-300">
                  <Icon className="h-4 w-4" />
                </span>
                <span className="text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-full bg-white/5 text-slate-400 group-hover:text-purple-300">
                  {item.badge}
                </span>
              </div>
              <div>
                <h4 className="text-xs font-bold text-white group-hover:text-purple-200">{item.title}</h4>
                <p className="text-[11px] text-slate-400 line-clamp-2 mt-1">{item.prompt}</p>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
