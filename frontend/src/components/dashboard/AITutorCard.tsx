import React from "react";
import { Link } from "react-router-dom";
import { Bot, Sparkles, ArrowRight, MessageSquareCode } from "lucide-react";
import { AITutorCardProps } from "./types";

export default function AITutorCard({ className = "" }: AITutorCardProps) {
  return (
    <div className={`glass-panel p-6 rounded-2xl border border-white/5 relative overflow-hidden bg-gradient-to-br from-purple-900/20 via-transparent to-pink-900/10 space-y-4 ${className}`}>
      <div className="flex items-center justify-between border-b border-white/5 pb-3 relative z-10">
        <div className="flex items-center gap-2">
          <Bot className="h-5 w-5 text-purple-theme" />
          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">AI Tutor Companion</h3>
        </div>
        <span className="text-[10px] text-emerald-400 font-bold bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-full flex items-center gap-1">
          <Sparkles className="h-3 w-3" />
          <span>LLM Provider Active</span>
        </span>
      </div>

      <div className="space-y-3 relative z-10">
        <div className="flex items-start gap-3">
          <div className="h-10 w-10 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-theme shrink-0">
            <MessageSquareCode className="h-5 w-5 animate-pulse text-purple-400" />
          </div>
          <div className="space-y-1">
            <h4 className="text-xs font-bold text-main-theme">24/7 Contextual Learning Assistant</h4>
            <p className="text-[10px] text-secondary-theme leading-relaxed">
              Ask questions, discuss complex Data Structures concepts, and receive step-by-step problem explanations.
            </p>
          </div>
        </div>

        <div className="p-3 bg-purple-900/20 rounded-xl border border-purple-500/20 flex items-center justify-between">
          <span className="text-[10px] text-purple-200 font-semibold">Ready to chat with your AI Tutor?</span>
          <Link
            to="/dashboard/ai-tutor"
            className="px-3.5 py-2 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white text-xs font-bold rounded-xl shadow-md shadow-purple-600/20 flex items-center gap-1.5 transition-all"
          >
            <span>Launch AI Tutor</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}
