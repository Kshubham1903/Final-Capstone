import React from "react";
import { Bot, Sparkles } from "lucide-react";
import MarkdownRenderer from "./MarkdownRenderer";

interface StreamingMessageProps {
  text?: string;
  modeLabel?: string;
}

export default function StreamingMessage({ text = "", modeLabel }: StreamingMessageProps) {
  return (
    <div className="flex gap-3 my-4 justify-start">
      <div className="h-8 w-8 rounded-2xl bg-gradient-to-tr from-purple-600 via-indigo-600 to-cyan-600 flex items-center justify-center text-white shrink-0 shadow-lg shadow-purple-500/20 border border-purple-400/30 animate-pulse">
        <Bot className="h-4 w-4" />
      </div>

      <div className="max-w-[85%] sm:max-w-[78%]">
        <div className="flex items-center gap-2 mb-1 px-1 text-[11px] text-slate-400">
          <span className="font-bold text-slate-300 flex items-center gap-1">
            EduPilot AI
            <Sparkles className="h-3 w-3 text-purple-400 animate-spin" />
          </span>
          {modeLabel && (
            <>
              <span>•</span>
              <span className="text-purple-300 font-semibold">{modeLabel}</span>
            </>
          )}
          <span>•</span>
          <span className="text-emerald-400 font-bold animate-pulse">Streaming Response...</span>
        </div>

        <div className="p-4 rounded-2xl border border-purple-500/30 bg-slate-900/90 text-slate-100 backdrop-blur-xl rounded-tl-xs shadow-xl relative">
          {text ? (
            <MarkdownRenderer content={text} />
          ) : (
            <div className="flex items-center gap-2 text-slate-400 text-xs py-1">
              <span className="h-2 w-2 rounded-full bg-purple-500 animate-ping" />
              <span>Formulating personalized explanation...</span>
            </div>
          )}

          {/* Streaming blinking cursor */}
          <span className="inline-block w-2 h-4 bg-purple-400 ml-1 animate-pulse" />
        </div>
      </div>
    </div>
  );
}
