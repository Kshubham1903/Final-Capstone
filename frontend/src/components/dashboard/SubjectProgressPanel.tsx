import React, { useState, useEffect } from "react";
import { 
  TrendingUp, 
  X, 
  Calendar, 
  Loader2, 
  Sparkles
} from "lucide-react";
import { 
  ResponsiveContainer, 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip 
} from "recharts";
import { 
  fetchSubjectProgressHistory, 
  AttemptMasteryPointDTO 
} from "../../services/api";

interface SubjectProgressPanelProps {
  studentId: string;
  subject: string;
  currentMastery: number;
  onClose: () => void;
}

export default function SubjectProgressPanel({
  studentId,
  subject,
  currentMastery,
  onClose
}: SubjectProgressPanelProps) {
  const [history, setHistory] = useState<AttemptMasteryPointDTO[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [days, setDays] = useState<number>(30);

  useEffect(() => {
    let isMounted = true;
    const loadHistory = async () => {
      setIsLoading(true);
      try {
        const data = await fetchSubjectProgressHistory(studentId, subject, days, "perAttempt");
        if (isMounted) {
          setHistory(data);
        }
      } catch (err) {
        console.warn("Error loading progress history for panel:", err);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    if (studentId && subject) {
      loadHistory();
    }
    return () => {
      isMounted = false;
    };
  }, [studentId, subject, days]);

  const formattedChartData = history.map((item, index) => {
    const d = new Date(item.dateTime);
    const dateStr = !isNaN(d.getTime()) 
      ? d.toLocaleDateString("en-US", { month: "short", day: "numeric" }) + " " + d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false })
      : `Attempt #${index + 1}`;
    return {
      attemptIndex: index + 1,
      attemptLabel: `Test #${index + 1}`,
      displayDate: dateStr,
      rawDateTime: item.dateTime,
      Mastery: Math.round(item.scorePercentage),
      questionsAnswered: item.questionsInAttempt,
      correctCount: item.correctInAttempt
    };
  });

  const totalAnsweredInWindow = history.reduce((sum, pt) => sum + pt.questionsInAttempt, 0);
  const peakMasteryInWindow = history.reduce((max, pt) => Math.max(max, pt.scorePercentage), currentMastery);

  return (
    <div className="glass-panel p-6 rounded-2xl border border-purple-500/20 bg-gradient-to-br from-purple-950/20 via-transparent to-pink-950/20 space-y-5 animate-fade-in relative">
      
      {/* Header Bar */}
      <div className="flex items-center justify-between border-b border-white/5 pb-3.5">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="h-9 w-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-300 shrink-0">
            <TrendingUp className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-black text-main-theme uppercase tracking-wider truncate">
                {subject}
              </h3>
              <span className="text-xs font-black px-2 py-0.5 rounded-md bg-purple-500/20 text-purple-300 border border-purple-500/30">
                {Math.round(currentMastery)}% Mastery
              </span>
            </div>
            <p className="text-[11px] text-secondary-theme leading-tight mt-0.5">
              Per-Test Attempt Performance Trend
            </p>
          </div>
        </div>

        <button
          onClick={onClose}
          className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-colors cursor-pointer shrink-0"
          title="Close History Panel"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-3 gap-3">
        <div className="glass-panel p-3 rounded-xl border border-white/5 space-y-0.5">
          <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Current Mastery</span>
          <div className="flex items-baseline gap-1">
            <span className="text-lg font-black text-purple-300">{Math.round(currentMastery)}%</span>
          </div>
        </div>

        <div className="glass-panel p-3 rounded-xl border border-white/5 space-y-0.5">
          <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Peak Score</span>
          <span className="text-lg font-black text-emerald-400">{Math.round(peakMasteryInWindow)}%</span>
        </div>

        <div className="glass-panel p-3 rounded-xl border border-white/5 space-y-0.5">
          <span className="text-[9px] text-secondary-theme uppercase font-bold tracking-wider block">Total Questions</span>
          <span className="text-lg font-black text-cyan-400">{totalAnsweredInWindow}</span>
        </div>
      </div>

      {/* Time Horizon Selector */}
      <div className="flex items-center justify-between text-xs pt-1">
        <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider flex items-center gap-1">
          <Calendar className="h-3.5 w-3.5 text-purple-400" />
          <span>Attempt History ({days} Days)</span>
        </span>
        <div className="flex items-center gap-1 bg-white/5 p-1 rounded-lg border border-white/5">
          {[14, 30, 60].map(d => (
            <button
              key={d}
              onClick={() => setDays(d)}
              className={`px-2 py-0.5 text-[10px] font-bold rounded-md transition-all cursor-pointer ${
                days === d 
                  ? "bg-purple-500 text-white shadow-sm" 
                  : "text-secondary-theme hover:text-main-theme"
              }`}
            >
              {d}D
            </button>
          ))}
        </div>
      </div>

      {/* Line Chart */}
      <div className="h-56 w-full text-xs">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full space-y-2">
            <Loader2 className="h-6 w-6 text-purple-400 animate-spin" />
            <span className="text-[10px] text-secondary-theme">Loading test attempts timeline...</span>
          </div>
        ) : formattedChartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={formattedChartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--glass-border)" />
              <XAxis 
                dataKey="attemptLabel" 
                stroke="var(--text-secondary)" 
                tick={{ fontSize: 10 }}
              />
              <YAxis 
                stroke="var(--text-secondary)" 
                domain={[0, 100]} 
                tick={{ fontSize: 10 }}
                unit="%"
              />
              <Tooltip 
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload;
                    return (
                      <div className="p-3 rounded-xl bg-[#0d0e17] border border-purple-500/30 text-xs shadow-xl space-y-1">
                        <p className="font-extrabold text-main-theme border-b border-white/10 pb-1">
                          Test #{data.attemptIndex} ({data.displayDate})
                        </p>
                        <p className="text-purple-300 font-bold">Attempt Score: {data.Mastery}%</p>
                        <p className="text-[10px] text-emerald-400 font-medium">
                          Correct: {data.correctCount} / {data.questionsAnswered} Questions
                        </p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Line 
                type="monotone" 
                dataKey="Mastery" 
                stroke="var(--accent-purple)" 
                strokeWidth={2.5}
                dot={{ r: 4, fill: "var(--accent-purple)" }}
                activeDot={{ r: 7, fill: "#a855f7", stroke: "#ffffff", strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex flex-col items-center justify-center h-full p-6 text-center space-y-1 bg-white/3 rounded-xl border border-white/5">
            <Sparkles className="h-6 w-6 text-purple-theme opacity-50" />
            <p className="text-xs font-bold text-main-theme">No Test Attempts Recorded</p>
            <p className="text-[10px] text-secondary-theme">Take adaptive quizzes or baseline knowledge tests to see your attempt-by-attempt score progression.</p>
          </div>
        )}
      </div>
    </div>
  );
}
