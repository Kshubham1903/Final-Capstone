import React from "react";
import { Award, Clock, CheckCircle2 } from "lucide-react";
import { RecentActivityCardProps } from "./types";

export default function RecentActivityCard({ profile }: RecentActivityCardProps) {
  const badges = profile?.badges || [];
  const completedQuizzes = profile?.completedQuizzesCount || 0;

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-amber-theme" />
          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Recent Achievements</h3>
        </div>
        <span className="text-[10px] text-amber-theme font-bold bg-amber-500/10 px-2 py-0.5 rounded-md">
          {badges.length} Badges
        </span>
      </div>

      <div className="space-y-3">
        {/* Quiz Count Activity Summary */}
        <div className="p-3 bg-white/5 rounded-xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-lg bg-purple-500/10 text-purple-theme flex items-center justify-center">
              <CheckCircle2 className="h-4 w-4" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-main-theme">Adaptive Quizzes Passed</h4>
              <p className="text-[10px] text-secondary-theme">Total completed assessments</p>
            </div>
          </div>
          <span className="text-base font-black text-purple-theme">{completedQuizzes}</span>
        </div>

        {/* Badges List */}
        <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
          {badges.length > 0 ? (
            badges.map((b) => (
              <div key={b.name} className="flex items-center gap-3 p-2.5 bg-white/5 rounded-xl border border-white/5 hover:border-white/10 transition-all">
                <div className="text-xl h-8 w-8 bg-amber-500/10 rounded-lg flex items-center justify-center shrink-0">
                  {b.icon}
                </div>
                <div className="min-w-0">
                  <h5 className="text-xs font-bold text-main-theme truncate">{b.name}</h5>
                  <p className="text-[10px] text-secondary-theme truncate">{b.description}</p>
                </div>
              </div>
            ))
          ) : (
            <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme space-y-1">
              <Award className="h-6 w-6 text-amber-theme opacity-40 mx-auto" />
              <p className="font-bold text-xs text-main-theme">No Badges Unlocked Yet</p>
              <p>Complete adaptive quizzes to earn achievement badges!</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
