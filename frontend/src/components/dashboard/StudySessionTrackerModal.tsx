import React, { useState, useEffect } from "react";
import { Play, Pause, CheckCircle, Clock, X, Sparkles } from "lucide-react";
import { endStudySession } from "../../services/api";

interface StudySessionTrackerModalProps {
  sessionData: {
    sessionId: string;
    taskId: string;
    subjectCode: string;
    conceptName: string;
  } | null;
  onClose: () => void;
  onSessionFinished: () => void;
}

export default function StudySessionTrackerModal({
  sessionData,
  onClose,
  onSessionFinished
}: StudySessionTrackerModalProps) {
  const [seconds, setSeconds] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [notes, setNotes] = useState("");
  const [finishing, setFinishing] = useState(false);

  useEffect(() => {
    let interval: any = null;
    if (sessionData && !isPaused) {
      interval = setInterval(() => {
        setSeconds((prev) => prev + 1);
      }, 1000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [sessionData, isPaused]);

  if (!sessionData) return null;

  const formatTime = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const handleFinishSession = async () => {
    setFinishing(true);
    const actualMins = Math.max(1, Math.round(seconds / 60));
    await endStudySession({
      sessionId: sessionData.sessionId,
      actualDurationMinutes: actualMins,
      pausedDurationMinutes: 0,
      completionNotes: notes || "Completed study session."
    });
    setFinishing(false);
    onSessionFinished();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
      <div className="glass-panel w-full max-w-lg rounded-2xl border border-white/10 p-6 space-y-6 bg-slate-900/95 shadow-2xl relative">
        
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-secondary-theme hover:text-white p-1 rounded-lg hover:bg-white/10 transition-all cursor-pointer"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-theme">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-lg font-extrabold text-main-theme">Active Study Session</h3>
            <p className="text-xs text-secondary-theme font-medium">
              {sessionData.subjectCode} • {sessionData.conceptName}
            </p>
          </div>
        </div>

        {/* Live Timer Card */}
        <div className="p-6 bg-gradient-to-br from-purple-900/30 to-pink-900/20 border border-purple-500/30 rounded-2xl text-center space-y-3">
          <div className="text-4xl sm:text-5xl font-black tracking-widest font-mono text-purple-300">
            {formatTime(seconds)}
          </div>
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              onClick={() => setIsPaused(!isPaused)}
              className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs flex items-center gap-2 transition-all cursor-pointer"
            >
              {isPaused ? <Play className="h-4 w-4 text-emerald-400" /> : <Pause className="h-4 w-4 text-amber-400" />}
              <span>{isPaused ? "Resume Session" : "Pause Session"}</span>
            </button>
          </div>
        </div>

        {/* Completion Notes */}
        <div className="space-y-2">
          <label className="text-xs font-extrabold text-secondary-theme uppercase tracking-wider block">
            Session Learning Notes
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Record key concepts revised or questions encountered during this study session..."
            rows={3}
            className="w-full bg-white/5 border border-white/10 rounded-xl p-3 text-xs text-main-theme placeholder:text-secondary-theme/50 focus:outline-none focus:border-purple-500/50"
          />
        </div>

        {/* Action Button */}
        <button
          onClick={handleFinishSession}
          disabled={finishing}
          className="w-full py-3.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-black text-sm rounded-xl shadow-lg shadow-emerald-600/30 flex items-center justify-center gap-2 transition-all cursor-pointer"
        >
          <CheckCircle className="h-5 w-5" />
          <span>{finishing ? "Saving Session..." : "Finish & Complete Task"}</span>
        </button>

      </div>
    </div>
  );
}
