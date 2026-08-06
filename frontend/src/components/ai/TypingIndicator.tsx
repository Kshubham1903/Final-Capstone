import React from "react";
import { Bot, Sparkles } from "lucide-react";

interface TypingIndicatorProps {
  modeLabel?: string;
}

export default function TypingIndicator({ modeLabel }: TypingIndicatorProps) {
  return (
    <div className="flex gap-3 my-4 justify-start items-end">
      <div className="h-8 w-8 rounded-2xl bg-gradient-to-tr from-purple-600 via-indigo-600 to-cyan-600 flex items-center justify-center text-white shrink-0 shadow-lg shadow-purple-500/20 border border-purple-400/30 animate-pulse">
        <Bot className="h-4 w-4" />
      </div>

      <div className="p-3.5 px-4 rounded-2xl bg-slate-900/90 border border-purple-500/30 text-slate-300 backdrop-blur-xl rounded-tl-xs shadow-xl flex items-center gap-3">
        <div className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-purple-400 animate-bounce [animation-delay:-0.3s]" />
          <span className="h-2 w-2 rounded-full bg-cyan-400 animate-bounce [animation-delay:-0.15s]" />
          <span className="h-2 w-2 rounded-full bg-indigo-400 animate-bounce" />
        </div>
        <span className="text-xs font-semibold text-slate-300 flex items-center gap-1">
          <span>EduPilot AI thinking</span>
          {modeLabel && <span className="text-purple-400 font-bold">({modeLabel})</span>}
        </span>
      </div>
    </div>
  );
}
