import React, { useState, useEffect } from "react";
import { 
  BookOpen, 
  ExternalLink, 
  X, 
  Loader2, 
  AlertCircle, 
  Play, 
  Pause, 
  CheckCircle, 
  Clock, 
  Globe, 
  Sparkles,
  Search
} from "lucide-react";
import { fetchStudyResources, endStudySession } from "../../services/api";

interface ResourceItem {
  title: string;
  url: string;
  domain: string;
  description: string;
}

interface StudyResourceModalProps {
  sessionData: {
    sessionId: string;
    taskId: string;
    subjectCode: string;
    conceptName: string;
    subjectName?: string;
  };
  onClose: () => void;
  onSessionFinished: () => void;
}

export default function StudyResourceModal({
  sessionData,
  onClose,
  onSessionFinished
}: StudyResourceModalProps) {
  // Resource discovery states
  const [loadingResources, setLoadingResources] = useState(true);
  const [resourcesData, setResourcesData] = useState<ResourceItem[]>([]);
  const [resourceError, setResourceError] = useState<string | null>(null);

  // Timer & study tracker states
  const [seconds, setSeconds] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [notes, setNotes] = useState("");
  const [finishing, setFinishing] = useState(false);

  const subject = sessionData.subjectName || sessionData.subjectCode || "";
  const concept = sessionData.conceptName || "";

  // 1. Discover Dynamic Study Resources
  useEffect(() => {
    let isMounted = true;
    const loadResources = async () => {
      setLoadingResources(true);
      setResourceError(null);

      const res = await fetchStudyResources(subject, concept);

      if (isMounted) {
        if (res && res.resources && Array.isArray(res.resources) && res.resources.length > 0) {
          setResourcesData(res.resources);
        } else {
          setResourceError("Unable to discover learning resources for this concept at the moment.");
        }
        setLoadingResources(false);
      }
    };

    if (concept) {
      loadResources();
    } else {
      setLoadingResources(false);
      setResourceError("Concept name is missing.");
    }

    return () => {
      isMounted = false;
    };
  }, [subject, concept]);

  // 2. Study Session Live Timer
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
      completionNotes: notes || `Studied ${concept} using dynamic educational resources.`
    });
    setFinishing(false);
    onSessionFinished();
    onClose();
  };

  const getDomainBadgeStyle = (domain: string) => {
    const d = domain.toLowerCase();
    if (d.includes("geeks")) return "bg-emerald-500/20 text-emerald-300 border-emerald-500/30";
    if (d.includes("w3schools")) return "bg-green-500/20 text-green-300 border-green-500/30";
    if (d.includes("wikipedia")) return "bg-cyan-500/20 text-cyan-300 border-cyan-500/30";
    if (d.includes("youtube")) return "bg-red-500/20 text-red-300 border-red-500/30";
    if (d.includes("scholar")) return "bg-purple-500/20 text-purple-300 border-purple-500/30";
    return "bg-blue-500/20 text-blue-300 border-blue-500/30";
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
      <div className="glass-panel w-full max-w-2xl rounded-3xl border border-white/10 p-6 space-y-5 bg-[#0e101a] text-main-theme shadow-2xl relative my-auto max-h-[92vh] overflow-y-auto">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-secondary-theme hover:text-white p-1.5 rounded-xl bg-white/5 hover:bg-white/10 transition-all cursor-pointer z-10"
        >
          <X className="h-4 w-4" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 border-b border-white/10 pb-4">
          <div className="h-10 w-10 rounded-2xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-300 shrink-0">
            <BookOpen className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-black text-main-theme">
                Adaptive Concept Study Hub
              </h3>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                {sessionData.subjectCode}
              </span>
            </div>
            <p className="text-xs text-purple-300 font-bold mt-0.5">
              Target Concept: {concept}
            </p>
          </div>
        </div>

        {/* Live Timer Strip */}
        <div className="p-4 bg-gradient-to-r from-purple-950/40 via-slate-900 to-emerald-950/40 border border-white/10 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="text-2xl font-black tracking-widest font-mono text-purple-300">
              {formatTime(seconds)}
            </div>
            <span className="text-[11px] text-secondary-theme font-medium">Study Timer Active</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsPaused(!isPaused)}
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer border border-white/10"
            >
              {isPaused ? <Play className="h-3.5 w-3.5 text-emerald-400" /> : <Pause className="h-3.5 w-3.5 text-amber-400" />}
              <span>{isPaused ? "Resume" : "Pause"}</span>
            </button>

            <button
              onClick={handleFinishSession}
              disabled={finishing}
              className="px-4 py-1.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-black text-xs rounded-xl shadow-md flex items-center gap-1.5 transition-all cursor-pointer"
            >
              <CheckCircle className="h-3.5 w-3.5" />
              <span>{finishing ? "Saving..." : "Finish & Complete"}</span>
            </button>
          </div>
        </div>

        {/* Resources Section Header */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-purple-400 fill-current" />
            <h4 className="text-xs font-black uppercase tracking-wider text-main-theme">
              Dynamically Discovered Learning Resources
            </h4>
          </div>
          <span className="text-[10px] text-secondary-theme font-semibold">
            {resourcesData.length} Sources Discovered
          </span>
        </div>

        {/* Dynamic Resources List */}
        {loadingResources ? (
          <div className="py-12 text-center space-y-3 glass-panel rounded-2xl border border-white/5 p-6">
            <Loader2 className="h-8 w-8 text-purple-400 animate-spin mx-auto" />
            <p className="text-xs font-bold text-main-theme">Discovering Dynamic Learning Resources...</p>
            <p className="text-[10px] text-secondary-theme">Searching educational authorities for {concept}</p>
          </div>
        ) : resourceError ? (
          <div className="p-5 bg-pink-500/10 border border-pink-500/30 rounded-2xl text-center space-y-2">
            <AlertCircle className="h-6 w-6 text-pink-400 mx-auto" />
            <p className="text-xs font-bold text-pink-300">{resourceError}</p>
            <p className="text-[10px] text-secondary-theme">You can still track your study session using the timer above.</p>
          </div>
        ) : (
          <div className="space-y-3 max-h-[320px] overflow-y-auto pr-1">
            {resourcesData.map((item: ResourceItem, idx: number) => (
              <div 
                key={idx}
                className="p-4 rounded-2xl glass-panel border border-white/5 hover:border-purple-500/30 transition-all space-y-2 bg-white/3"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase border ${getDomainBadgeStyle(item.domain)}`}>
                        {item.domain}
                      </span>
                    </div>
                    <h5 className="text-xs font-extrabold text-main-theme leading-snug">
                      {item.title}
                    </h5>
                  </div>

                  <a
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3 py-1.5 rounded-xl bg-purple-600/30 hover:bg-purple-600/60 text-purple-200 text-[11px] font-extrabold border border-purple-500/40 flex items-center gap-1.5 transition-all shrink-0 cursor-pointer shadow-sm hover:scale-105"
                  >
                    <span>Open Resource</span>
                    <ExternalLink className="h-3.5 w-3.5" />
                  </a>
                </div>

                <p className="text-[11px] text-secondary-theme leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        )}

        {/* Learning Notes Input */}
        <div className="space-y-1.5 pt-2">
          <label className="text-[10px] font-extrabold text-secondary-theme uppercase tracking-wider block">
            Session Revision Notes
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Record key principles revised from the resources above..."
            rows={2}
            className="w-full bg-white/5 border border-white/10 rounded-xl p-2.5 text-xs text-main-theme placeholder:text-secondary-theme/50 focus:outline-none focus:border-purple-500/50"
          />
        </div>

      </div>
    </div>
  );
}
