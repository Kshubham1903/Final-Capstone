import React from "react";
import { Sparkles, BookOpen, Target, Award, Brain } from "lucide-react";
import { WelcomeCardProps } from "./types";

export default function WelcomeCard({ firstName, profile }: WelcomeCardProps) {
  const course = profile?.course || "Computer Science & Engineering";
  const semester = profile?.semester ?? 1;
  const targetCgpa = profile?.targetCgpa ?? 0.0;
  const predictedCgpa = profile?.predictedCgpa ?? 0.0;
  const learningStyle = profile?.learningStyle || "Visual";

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 relative overflow-hidden bg-gradient-to-r from-purple-900/10 via-transparent to-pink-900/10">
      <div className="absolute top-0 right-0 h-40 w-40 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />
      
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-theme text-xs font-semibold">
            <Brain className="h-3.5 w-3.5" />
            <span>AI Learning Companion Ready</span>
          </div>
          <h2 className="text-2xl md:text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <span>Welcome back, {firstName}!</span>
            <Sparkles className="h-6 w-6 text-purple-theme animate-pulse" />
          </h2>
          <p className="text-secondary-theme text-sm max-w-xl">
            Currently on track for <span className="font-semibold text-main-theme">{course}</span> — Semester {semester}. 
            Your AI Copilot has prepared today's personalized learning roadmap for you.
          </p>
        </div>

        <div className="flex flex-wrap sm:flex-nowrap items-center gap-3">
          <div className="glass-panel p-3.5 rounded-xl border border-white/5 flex items-center gap-3 min-w-[130px]">
            <div className="h-9 w-9 rounded-lg bg-purple-500/10 flex items-center justify-center text-purple-theme">
              <Target className="h-5 w-5" />
            </div>
            <div>
              <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider block">Target CGPA</span>
              <span className="text-sm font-black text-main-theme">{targetCgpa > 0 ? targetCgpa.toFixed(1) : "N/A"}</span>
            </div>
          </div>

          <div className="glass-panel p-3.5 rounded-xl border border-white/5 flex items-center gap-3 min-w-[130px]">
            <div className="h-9 w-9 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-theme">
              <Award className="h-5 w-5" />
            </div>
            <div>
              <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider block">Pred CGPA</span>
              <span className="text-sm font-black text-emerald-theme">{predictedCgpa > 0 ? predictedCgpa.toFixed(1) : "N/A"}</span>
            </div>
          </div>

          <div className="glass-panel p-3.5 rounded-xl border border-white/5 flex items-center gap-3 min-w-[130px]">
            <div className="h-9 w-9 rounded-lg bg-pink-500/10 flex items-center justify-center text-pink-theme">
              <BookOpen className="h-5 w-5" />
            </div>
            <div>
              <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider block">Style</span>
              <span className="text-sm font-black text-pink-theme">{learningStyle}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
