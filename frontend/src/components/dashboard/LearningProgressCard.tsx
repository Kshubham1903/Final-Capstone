import React from "react";
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer 
} from "recharts";
import { Activity } from "lucide-react";
import { LearningProgressCardProps } from "./types";

export default function LearningProgressCard({ profile, onSelectSubject }: LearningProgressCardProps) {
  // Use profile.subjects as the single source of truth for the student's subjects (exact same source as Adaptive Quizzes)
  const studentSubjects = profile?.subjects && profile.subjects.length > 0 
    ? profile.subjects 
    : Object.keys(profile?.conceptMastery || {});

  const masteryData = studentSubjects.map((subj) => {
    let val = 0;
    if (profile?.conceptMastery) {
      if (profile.conceptMastery[subj] !== undefined) {
        val = profile.conceptMastery[subj];
      } else {
        const foundKey = Object.keys(profile.conceptMastery).find(
          k => k.trim().toLowerCase() === subj.trim().toLowerCase()
        );
        if (foundKey && profile.conceptMastery[foundKey] !== undefined) {
          val = profile.conceptMastery[foundKey];
        }
      }
    }
    return {
      subject: subj.length > 14 ? subj.substring(0, 14) + "..." : subj,
      fullSubject: subj,
      rawMastery: val,
      Mastery: Math.round(val)
    };
  });

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-purple-theme" />
          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Subject Mastery Breakdown</h3>
        </div>
        <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider">
          Learning Analytics (Click Bar for History)
        </span>
      </div>

      {/* Chart Visualization */}
      <div className="h-64 w-full text-xs">
        {masteryData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={masteryData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--glass-border)" />
              <XAxis dataKey="subject" stroke="var(--text-secondary)" tick={{ fontSize: 10 }} />
              <YAxis stroke="var(--text-secondary)" domain={[0, 100]} tick={{ fontSize: 10 }} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: "var(--glass-bg)", 
                  color: "var(--text-main)", 
                  border: "1px solid var(--glass-border)", 
                  borderRadius: "8px",
                  fontSize: "12px"
                }} 
              />
              <Bar 
                dataKey="Mastery" 
                fill="var(--accent-purple)" 
                radius={[4, 4, 0, 0]} 
                className="cursor-pointer hover:opacity-80 transition-opacity"
                onClick={(data: any) => {
                  if (data && data.fullSubject && onSelectSubject) {
                    onSelectSubject(data.fullSubject, data.rawMastery ?? data.Mastery);
                  }
                }}
              />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex flex-col items-center justify-center h-full p-6 text-center space-y-2 bg-white/3 rounded-xl border border-white/5">
            <Activity className="h-8 w-8 text-purple-theme opacity-50" />
            <h4 className="font-bold text-xs text-main-theme">No Subject Mastery Data Yet</h4>
            <p className="text-[10px] text-secondary-theme max-w-xs">
              Complete onboarding or take adaptive quizzes to populate your subject mastery analytics.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
