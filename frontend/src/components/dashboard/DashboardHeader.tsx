import React from "react";
import { BrainCircuit, Flame, Sparkles } from "lucide-react";
import { DashboardHeaderProps } from "./types";

export default function DashboardHeader({ streak = 0, isBackendConnected = false }: DashboardHeaderProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
      <div className="flex items-center gap-3">
        <div className="h-10 w-10 rounded-xl bg-purple-600/20 flex items-center justify-center border border-purple-500/30 shadow-lg shadow-purple-500/10">
          <BrainCircuit className="h-6 w-6 text-purple-theme" />
        </div>
        <div>
          <h1 className="text-xl font-extrabold text-main-theme tracking-wide flex items-center gap-2">
            <span>My AI Learning Companion</span>
            <Sparkles className="h-4 w-4 text-purple-theme animate-pulse" />
          </h1>
          <p className="text-xs text-secondary-theme">Personalized AI Academic Dashboard</p>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 bg-amber-500/10 text-amber-theme border border-amber-500/20 px-3 py-1.5 rounded-full text-xs font-semibold">
          <Flame className="h-4 w-4 fill-[var(--accent-amber)]" />
          <span>{streak} Day Streak</span>
        </div>

        <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold border ${
          isBackendConnected 
            ? "bg-emerald-500/10 text-emerald-theme border-emerald-500/20"
            : "bg-purple-500/10 text-purple-theme border-purple-500/20"
        }`}>
          <span className={`h-2 w-2 rounded-full ${isBackendConnected ? "bg-emerald-400 animate-pulse" : "bg-purple-400"}`} />
          <span>{isBackendConnected ? "AI Core Live" : "AI Local Engine"}</span>
        </div>
      </div>
    </div>
  );
}
