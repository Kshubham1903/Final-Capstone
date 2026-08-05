import React from "react";
import { Link } from "react-router-dom";
import { GraduationCap, Timer, User, Zap, Sparkles } from "lucide-react";
import { QuickActionsCardProps } from "./types";

export default function QuickActionsCard({ className = "" }: QuickActionsCardProps) {
  const actions = [
    {
      title: "Adaptive Quiz",
      desc: "AI difficulty scaling",
      icon: GraduationCap,
      href: "/dashboard/quizzes",
      color: "from-purple-600/20 to-purple-800/10 border-purple-500/30 text-purple-theme"
    },
    {
      title: "Pomodoro Focus",
      desc: "Deep work timer",
      icon: Timer,
      href: "/dashboard/pomodoro",
      color: "from-pink-600/20 to-pink-800/10 border-pink-500/30 text-pink-theme"
    },
    {
      title: "Practice Concepts",
      desc: "Targeted revision",
      icon: Zap,
      href: "/dashboard/quizzes",
      color: "from-cyan-600/20 to-cyan-800/10 border-cyan-500/30 text-cyan-theme"
    },
    {
      title: "Student Profile",
      desc: "Academic settings",
      icon: User,
      href: "/dashboard/profile",
      color: "from-amber-600/20 to-amber-800/10 border-amber-500/30 text-amber-theme"
    }
  ];

  return (
    <div className={`glass-panel p-6 rounded-2xl border border-white/5 space-y-4 ${className}`}>
      <div className="flex items-center justify-between border-b border-white/5 pb-3">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-amber-theme" />
          <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Quick Actions</h3>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {actions.map((act) => {
          const Icon = act.icon;
          return (
            <Link
              key={act.title}
              to={act.href}
              className={`p-3.5 rounded-xl border bg-gradient-to-br ${act.color} hover:scale-[1.02] transition-all duration-200 flex flex-col justify-between group`}
            >
              <div className="flex items-center justify-between mb-2">
                <Icon className="h-5 w-5 group-hover:rotate-6 transition-transform" />
                <span className="text-[10px] font-bold uppercase tracking-wider opacity-60">Action</span>
              </div>
              <div>
                <h4 className="text-xs font-bold text-main-theme">{act.title}</h4>
                <p className="text-[10px] text-secondary-theme mt-0.5">{act.desc}</p>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
